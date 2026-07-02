import CryptoKit
import Foundation
import Shared
import UIKit
import UniformTypeIdentifiers

final class BettamindIosNativeAiBridge: NSObject, IosNativeAiBridge, UIDocumentPickerDelegate {
    private let fileManager = FileManager.default
    private var pendingInstallCallback: IosModelPackInstallCallback?
    private var pendingDocumentPicker: UIDocumentPickerViewController?

    func installerAvailable() -> Bool {
        true
    }

    func runtimeAvailable() -> Bool {
        false
    }

    func installedManifestJson() -> String? {
        guard let installedRoot = installedRootIfPresent(),
              let directories = try? fileManager.contentsOfDirectory(
                at: installedRoot,
                includingPropertiesForKeys: [.isDirectoryKey],
                options: [.skipsHiddenFiles]
              ) else {
            return nil
        }

        return directories.firstNotNull { directory in
            guard directory.isDirectory else {
                return nil
            }
            let manifestUrl = directory.appendingPathComponent(Self.manifestFileName, isDirectory: false)
            guard let selectedManifest = try? readManifest(url: manifestUrl),
                  selectedManifest.manifest.installValidationFailure() == nil,
                  verifySignature(selectedManifest.manifest) else {
                return nil
            }
            let artifact = directory.appendingPathComponent(
                selectedManifest.manifest.artifactFileName,
                isDirectory: false
            )
            guard artifact.exists,
                  artifact.fileSize == selectedManifest.manifest.artifactSizeBytes else {
                return nil
            }
            return normalizedManifestJson(selectedManifest.manifest)
        }
    }

    func requestUserInstall(callback: IosModelPackInstallCallback) -> Bool {
        if Thread.isMainThread {
            return presentModelPackPicker(callback: callback)
        }
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                callback.onInstallFailed(failureName: NativeInstallFailure.storageFailed.rawValue)
                return
            }
            _ = self.presentModelPackPicker(callback: callback)
        }
        return true
    }

    func removeInstalledModel() -> Bool {
        var removed = false
        if let installedRoot = installedRootIfPresent() {
            do {
                try fileManager.removeItem(at: installedRoot)
                removed = true
            } catch {
                return false
            }
        }
        if let stagedRoot = stagedRootIfPresent() {
            try? fileManager.removeItem(at: stagedRoot)
        }
        return removed
    }

    func loadRuntime() -> Bool {
        false
    }

    func generate(prompt: String) -> String? {
        nil
    }

    func closeRuntime() {
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        completeInstall(result: .failure(.selectionCanceled))
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard !urls.isEmpty else {
            completeInstall(result: .failure(.selectionCanceled))
            return
        }
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else {
                return
            }
            let result = self.installSelectedDocuments(urls)
            self.completeInstall(result: result)
        }
    }

    private func presentModelPackPicker(callback: IosModelPackInstallCallback) -> Bool {
        guard pendingInstallCallback == nil else {
            callback.onInstallFailed(failureName: NativeInstallFailure.storageFailed.rawValue)
            return false
        }
        guard let presenter = UIApplication.shared.bettamindTopViewController else {
            callback.onInstallFailed(failureName: NativeInstallFailure.storageFailed.rawValue)
            return false
        }

        var contentTypes = [UTType.json, UTType.data, UTType.item]
        if let liteRtLmType = UTType(filenameExtension: "litertlm") {
            contentTypes.insert(liteRtLmType, at: 1)
        }

        let picker = UIDocumentPickerViewController(
            forOpeningContentTypes: contentTypes,
            asCopy: true
        )
        picker.allowsMultipleSelection = true
        picker.delegate = self
        pendingInstallCallback = callback
        pendingDocumentPicker = picker
        presenter.present(picker, animated: true)
        return true
    }

    private func installSelectedDocuments(_ urls: [URL]) -> Result<String, NativeInstallFailure> {
        do {
            let selectedManifest = try selectedManifest(from: urls)
            guard selectedManifest.manifest.installValidationFailure() == nil else {
                return .failure(selectedManifest.manifest.installValidationFailure() ?? .invalidManifest)
            }
            guard verifySignature(selectedManifest.manifest) else {
                return .failure(.invalidSignature)
            }
            guard let artifactUrl = selectedArtifactUrl(
                from: urls,
                manifest: selectedManifest.manifest,
                manifestUrl: selectedManifest.url
            ) else {
                return .failure(.missingArtifact)
            }
            let installedManifestJson = try installArtifact(
                manifest: selectedManifest.manifest,
                artifactUrl: artifactUrl
            )
            return .success(installedManifestJson)
        } catch let error as NativeInstallError {
            return .failure(error.failure)
        } catch {
            return .failure(.storageFailed)
        }
    }

    private func selectedManifest(from urls: [URL]) throws -> SelectedManifest {
        for url in urls {
            guard shouldInspectAsManifest(url: url) else {
                continue
            }
            if let manifest = try? readManifest(url: url) {
                return manifest
            }
        }
        throw NativeInstallError(.missingManifest)
    }

    private func shouldInspectAsManifest(url: URL) -> Bool {
        let fileName = url.lastPathComponent.lowercased()
        if fileName.hasSuffix(".json") {
            return true
        }
        if let size = url.fileSize {
            return size <= Self.maxManifestBytes
        }
        return true
    }

    private func readManifest(url: URL) throws -> SelectedManifest {
        try withSecurityScope(url) {
            let data = try readSmallFile(url: url, limitBytes: Self.maxManifestBytes)
            guard manifestJsonHasOnlyKnownKeys(data) else {
                throw NativeInstallError(.invalidManifest)
            }
            let manifest = try Self.manifestDecoder.decode(NativeModelPackManifest.self, from: data)
            return SelectedManifest(url: url, manifest: manifest)
        }
    }

    private func readSmallFile(url: URL, limitBytes: Int64) throws -> Data {
        let handle = try FileHandle(forReadingFrom: url)
        defer {
            handle.closeFile()
        }
        let data = handle.readData(ofLength: Int(limitBytes) + 1)
        if Int64(data.count) > limitBytes {
            throw NativeInstallError(.invalidManifest)
        }
        return data
    }

    private func manifestJsonHasOnlyKnownKeys(_ data: Data) -> Bool {
        guard let object = try? JSONSerialization.jsonObject(with: data),
              let dictionary = object as? [String: Any] else {
            return false
        }
        let keys = Set(dictionary.keys)
        return Self.requiredManifestKeys.isSubset(of: keys) &&
            keys.isSubset(of: Self.knownManifestKeys)
    }

    private func selectedArtifactUrl(
        from urls: [URL],
        manifest: NativeModelPackManifest,
        manifestUrl: URL
    ) -> URL? {
        urls.first { url in
            guard url != manifestUrl else {
                return false
            }
            return url.lastPathComponent == manifest.artifactFileName ||
                url.fileSize == manifest.artifactSizeBytes
        }
    }

    private func installArtifact(
        manifest: NativeModelPackManifest,
        artifactUrl: URL
    ) throws -> String {
        let normalizedManifest = normalizedManifestJson(manifest)
        let installDir = try installedDirectory(for: manifest)
        let stageDir = try stagedDirectory(for: manifest)
        let stagedArtifact = stageDir.appendingPathComponent(
            "\(manifest.artifactFileName).part",
            isDirectory: false
        )

        try? fileManager.removeItem(at: stageDir)
        try fileManager.createDirectory(at: stageDir, withIntermediateDirectories: true)
        excludeFromBackup(stageDir)
        defer {
            try? fileManager.removeItem(at: stageDir)
        }

        var hasher = SHA256()
        var copiedBytes: Int64 = 0
        try withSecurityScope(artifactUrl) {
            let input = try FileHandle(forReadingFrom: artifactUrl)
            defer {
                input.closeFile()
            }
            fileManager.createFile(atPath: stagedArtifact.path, contents: nil)
            let output = try FileHandle(forWritingTo: stagedArtifact)
            defer {
                output.closeFile()
            }

            while true {
                let chunk = input.readData(ofLength: Self.copyBufferBytes)
                if chunk.isEmpty {
                    break
                }
                copiedBytes += Int64(chunk.count)
                if copiedBytes > manifest.artifactSizeBytes {
                    throw NativeInstallError(.artifactSizeMismatch)
                }
                hasher.update(data: chunk)
                output.write(chunk)
            }
        }

        guard copiedBytes == manifest.artifactSizeBytes else {
            throw NativeInstallError(.artifactSizeMismatch)
        }
        let checksum = hasher.finalize().hexString
        guard checksum.caseInsensitiveCompare(manifest.artifactChecksumSha256) == .orderedSame else {
            throw NativeInstallError(.checksumMismatch)
        }

        try? fileManager.removeItem(at: installDir)
        try fileManager.createDirectory(at: installDir, withIntermediateDirectories: true)
        excludeFromBackup(installDir)

        let installedArtifact = installDir.appendingPathComponent(
            manifest.artifactFileName,
            isDirectory: false
        )
        try fileManager.moveItem(at: stagedArtifact, to: installedArtifact)
        let manifestUrl = installDir.appendingPathComponent(Self.manifestFileName, isDirectory: false)
        try Data(normalizedManifest.utf8).write(to: manifestUrl, options: [.atomic])
        return normalizedManifest
    }

    private func verifySignature(_ manifest: NativeModelPackManifest) -> Bool {
        guard manifest.signingKeyId == Self.trustAnchorKeyId,
              let signature = Data(base64Encoded: manifest.signature, options: [.ignoreUnknownCharacters]),
              let publicKeyData = Data(
                base64Encoded: Self.trustAnchorPublicKeyBase64,
                options: [.ignoreUnknownCharacters]
              ) else {
            return false
        }

        let rawPublicKey: Data = publicKeyData.count == Self.ed25519RawPublicKeyBytes
            ? publicKeyData
            : Data(publicKeyData.suffix(Self.ed25519RawPublicKeyBytes))

        return (try? Curve25519.Signing.PublicKey(rawRepresentation: rawPublicKey)
            .isValidSignature(signature, for: signedManifestBytes(manifest))) == true
    }

    private func signedManifestBytes(_ manifest: NativeModelPackManifest) -> Data {
        let capabilities = Set(manifest.capabilities).sorted()
        let json = [
            "{\"modelId\":\(jsonString(manifest.modelId))",
            "\"version\":\(manifest.version)",
            "\"displayName\":\(jsonString(manifest.displayName))",
            "\"runtimeId\":\(jsonString(manifest.runtimeId))",
            "\"artifactFileName\":\(jsonString(manifest.artifactFileName))",
            "\"artifactSizeBytes\":\(manifest.artifactSizeBytes)",
            "\"artifactChecksumSha256\":\(jsonString(manifest.artifactChecksumSha256))",
            "\"capabilities\":[\(capabilities.map(jsonString).joined(separator: ","))]",
            "\"signingKeyId\":\(jsonString(manifest.signingKeyId))",
            "\"signatureAlgorithm\":\(jsonString(manifest.signatureAlgorithm))}",
        ].joined(separator: ",")
        return Data(json.utf8)
    }

    private func normalizedManifestJson(_ manifest: NativeModelPackManifest) -> String {
        let json = [
            "{\"modelId\":\(jsonString(manifest.modelId))",
            "\"version\":\(manifest.version)",
            "\"displayName\":\(jsonString(manifest.displayName))",
            "\"runtimeId\":\(jsonString(manifest.runtimeId))",
            "\"artifactFileName\":\(jsonString(manifest.artifactFileName))",
            "\"artifactSizeBytes\":\(manifest.artifactSizeBytes)",
            "\"artifactChecksumSha256\":\(jsonString(manifest.artifactChecksumSha256))",
            "\"capabilities\":[\(manifest.capabilities.map(jsonString).joined(separator: ","))]",
            "\"signingKeyId\":\(jsonString(manifest.signingKeyId))",
            "\"signatureAlgorithm\":\(jsonString(manifest.signatureAlgorithm))",
            "\"signature\":\(jsonString(manifest.signature))}",
        ].joined(separator: ",")
        return json
    }

    private func jsonString(_ value: String) -> String {
        var result = "\""
        for scalar in value.unicodeScalars {
            switch scalar.value {
            case 0x22:
                result += "\\\""
            case 0x5c:
                result += "\\\\"
            case 0x08:
                result += "\\b"
            case 0x0c:
                result += "\\f"
            case 0x0a:
                result += "\\n"
            case 0x0d:
                result += "\\r"
            case 0x09:
                result += "\\t"
            case 0x00...0x1f:
                result += String(format: "\\u%04x", scalar.value)
            default:
                result.unicodeScalars.append(scalar)
            }
        }
        result += "\""
        return result
    }

    private func completeInstall(result: Result<String, NativeInstallFailure>) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                return
            }
            let callback = self.pendingInstallCallback
            self.pendingInstallCallback = nil
            self.pendingDocumentPicker = nil
            switch result {
            case .success(let manifestJson):
                callback?.onInstallCompleted(manifestJson: manifestJson)
            case .failure(let failure):
                callback?.onInstallFailed(failureName: failure.rawValue)
            }
        }
    }

    private func modelPacksRoot(create: Bool = true) throws -> URL {
        let supportRoot = try fileManager.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: create
        )
        let root = supportRoot.appendingPathComponent("model-packs", isDirectory: true)
        if create {
            try fileManager.createDirectory(at: root, withIntermediateDirectories: true)
            excludeFromBackup(root)
        }
        return root
    }

    private func installedRootIfPresent() -> URL? {
        guard let modelRoot = try? modelPacksRoot(create: false) else {
            return nil
        }
        let root = modelRoot.appendingPathComponent("installed", isDirectory: true)
        return root.exists ? root : nil
    }

    private func stagedRootIfPresent() -> URL? {
        guard let modelRoot = try? modelPacksRoot(create: false) else {
            return nil
        }
        let root = modelRoot.appendingPathComponent("staged", isDirectory: true)
        return root.exists ? root : nil
    }

    private func installedDirectory(for manifest: NativeModelPackManifest) throws -> URL {
        let root = try modelPacksRoot()
            .appendingPathComponent("installed", isDirectory: true)
        try fileManager.createDirectory(at: root, withIntermediateDirectories: true)
        excludeFromBackup(root)
        return root.appendingPathComponent(
            "\(manifest.modelId.safeFileSegment())-v\(manifest.version)",
            isDirectory: true
        )
    }

    private func stagedDirectory(for manifest: NativeModelPackManifest) throws -> URL {
        let root = try modelPacksRoot()
            .appendingPathComponent("staged", isDirectory: true)
        try fileManager.createDirectory(at: root, withIntermediateDirectories: true)
        excludeFromBackup(root)
        return root.appendingPathComponent(
            "\(manifest.modelId.safeFileSegment())-v\(manifest.version)",
            isDirectory: true
        )
    }

    private func excludeFromBackup(_ url: URL) {
        var resourceUrl = url
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try? resourceUrl.setResourceValues(values)
    }

    private func withSecurityScope<T>(_ url: URL, _ block: () throws -> T) rethrows -> T {
        let accessing = url.startAccessingSecurityScopedResource()
        defer {
            if accessing {
                url.stopAccessingSecurityScopedResource()
            }
        }
        return try block()
    }

    private struct SelectedManifest {
        let url: URL
        let manifest: NativeModelPackManifest
    }

    private enum NativeInstallFailure: String, Error {
        case selectionCanceled = "SelectionCanceled"
        case missingManifest = "MissingManifest"
        case missingArtifact = "MissingArtifact"
        case invalidManifest = "InvalidManifest"
        case unapprovedModel = "UnapprovedModel"
        case untrustedSigningKey = "UntrustedSigningKey"
        case invalidSignature = "InvalidSignature"
        case checksumMismatch = "ChecksumMismatch"
        case artifactSizeMismatch = "ArtifactSizeMismatch"
        case storageFailed = "StorageFailed"
    }

    private struct NativeInstallError: Error {
        let failure: NativeInstallFailure

        init(_ failure: NativeInstallFailure) {
            self.failure = failure
        }
    }

    private struct NativeModelPackManifest: Decodable {
        let modelId: String
        let version: Int
        let displayName: String
        let runtimeId: String
        let artifactFileName: String
        let artifactSizeBytes: Int64
        let artifactChecksumSha256: String
        let capabilities: [String]
        let signingKeyId: String
        let signatureAlgorithm: String
        let signature: String

        enum CodingKeys: String, CodingKey {
            case modelId
            case version
            case displayName
            case runtimeId
            case artifactFileName
            case artifactSizeBytes
            case artifactChecksumSha256
            case capabilities
            case signingKeyId
            case signatureAlgorithm
            case signature
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            modelId = try container.decode(String.self, forKey: .modelId)
            version = try container.decode(Int.self, forKey: .version)
            displayName = try container.decode(String.self, forKey: .displayName)
            runtimeId = try container.decodeIfPresent(String.self, forKey: .runtimeId) ?? Self.defaultRuntimeId
            artifactFileName = try container.decode(String.self, forKey: .artifactFileName)
            artifactSizeBytes = try container.decode(Int64.self, forKey: .artifactSizeBytes)
            artifactChecksumSha256 = try container.decode(String.self, forKey: .artifactChecksumSha256)
            capabilities = try container.decode([String].self, forKey: .capabilities)
            signingKeyId = try container.decode(String.self, forKey: .signingKeyId)
            signatureAlgorithm = try container.decodeIfPresent(
                String.self,
                forKey: .signatureAlgorithm
            ) ?? Self.defaultSignatureAlgorithm
            signature = try container.decode(String.self, forKey: .signature)
        }

        func installValidationFailure() -> NativeInstallFailure? {
            if modelId != BettamindIosNativeAiBridge.firstProductionModelId ||
                artifactFileName != BettamindIosNativeAiBridge.firstProductionArtifactFileName {
                return .unapprovedModel
            }
            if runtimeId != Self.defaultRuntimeId ||
                !capabilities.contains("Generation") ||
                artifactSizeBytes <= 0 ||
                artifactChecksumSha256.isEmpty ||
                signature.isEmpty ||
                signatureAlgorithm != Self.defaultSignatureAlgorithm {
                return .invalidManifest
            }
            if signingKeyId != BettamindIosNativeAiBridge.trustAnchorKeyId {
                return .untrustedSigningKey
            }
            return nil
        }

        private static let defaultRuntimeId = "litert-lm"
        private static let defaultSignatureAlgorithm = "Ed25519"
    }

    private static let manifestDecoder = JSONDecoder()
    private static let manifestFileName = "manifest.json"
    private static let maxManifestBytes: Int64 = 1024 * 1024
    private static let copyBufferBytes = 1024 * 1024
    private static let ed25519RawPublicKeyBytes = 32
    private static let firstProductionModelId = "Qwen/Qwen2.5-1.5B-Instruct"
    private static let firstProductionArtifactFileName = "qwen2_5_1_5b_instruct_bettamind_v1.litertlm"
    private static let trustAnchorKeyId = "bettamind-model-prod-2026-01"
    private static let trustAnchorPublicKeyBase64 = "MCowBQYDK2VwAyEAGsgkjHlXsNaWfwbOzajfTImt5yC6nSGIEIVUL18EvKY="
    private static let requiredManifestKeys: Set<String> = [
        "modelId",
        "version",
        "displayName",
        "artifactFileName",
        "artifactSizeBytes",
        "artifactChecksumSha256",
        "capabilities",
        "signingKeyId",
        "signature",
    ]
    private static let knownManifestKeys: Set<String> = requiredManifestKeys.union([
        "runtimeId",
        "signatureAlgorithm",
    ])
}

private extension URL {
    var exists: Bool {
        FileManager.default.fileExists(atPath: path)
    }

    var isDirectory: Bool {
        (try? resourceValues(forKeys: [.isDirectoryKey]).isDirectory) == true
    }

    var fileSize: Int64? {
        guard let size = try? resourceValues(forKeys: [.fileSizeKey]).fileSize else {
            return nil
        }
        return Int64(size)
    }
}

private extension Sequence {
    func firstNotNull<T>(_ transform: (Element) -> T?) -> T? {
        for element in self {
            if let value = transform(element) {
                return value
            }
        }
        return nil
    }
}

private extension SHA256.Digest {
    var hexString: String {
        map { String(format: "%02x", $0) }.joined()
    }
}

private extension String {
    func safeFileSegment() -> String {
        replacingOccurrences(
            of: "[^A-Za-z0-9._-]",
            with: "_",
            options: .regularExpression
        )
    }
}

private extension UIApplication {
    var bettamindTopViewController: UIViewController? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController?
            .bettamindTopPresentedViewController
    }
}

private extension UIViewController {
    var bettamindTopPresentedViewController: UIViewController {
        if let navigationController = self as? UINavigationController,
           let visible = navigationController.visibleViewController {
            return visible.bettamindTopPresentedViewController
        }
        if let tabController = self as? UITabBarController,
           let selected = tabController.selectedViewController {
            return selected.bettamindTopPresentedViewController
        }
        if let presented = presentedViewController {
            return presented.bettamindTopPresentedViewController
        }
        return self
    }
}
