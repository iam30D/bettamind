import CryptoKit
import Foundation
import LiteRTLM
import Shared
import UIKit
import UniformTypeIdentifiers

final class BettamindIosNativeAiBridge: NSObject, IosNativeAiBridge, UIDocumentPickerDelegate {
    private let runtime = LiteRtLmRuntimeBox()
    private var installCallback: IosModelPackInstallCallback?
    private var documentPicker: UIDocumentPickerViewController?

    func installerAvailable() -> Bool {
        true
    }

    func runtimeAvailable() -> Bool {
        installedPack() != nil
    }

    func installedManifestJson() -> String? {
        guard let pack = installedPack() else {
            return nil
        }
        return try? String(contentsOf: pack.manifestUrl, encoding: .utf8)
    }

    func requestUserInstall(callback: IosModelPackInstallCallback) -> Bool {
        guard installCallback == nil else {
            callback.onInstallFailed(failureName: "StorageFailed")
            return false
        }

        let artifactType = UTType(filenameExtension: "litertlm") ?? .data
        let picker = UIDocumentPickerViewController(
            forOpeningContentTypes: [.json, artifactType, .data, .item],
            asCopy: true
        )
        picker.allowsMultipleSelection = true
        picker.delegate = self
        installCallback = callback
        documentPicker = picker

        DispatchQueue.main.async {
            guard let presenter = UIApplication.shared.bettamindTopViewController() else {
                self.finishInstallFailure("StorageFailed")
                return
            }
            presenter.present(picker, animated: true)
        }
        return true
    }

    func removeInstalledModel() -> Bool {
        closeRuntime()
        guard let root = try? installedRoot() else {
            return false
        }
        guard FileManager.default.fileExists(atPath: root.path) else {
            return false
        }
        do {
            try FileManager.default.removeItem(at: root)
            return true
        } catch {
            return false
        }
    }

    func loadRuntime() -> Bool {
        waitForRuntime {
            try await self.runtime.load(installedPackProvider: self.installedPack)
        } ?? false
    }

    func generate(prompt: String) -> String? {
        waitForRuntime {
            try await self.runtime.generate(
                prompt: prompt,
                installedPackProvider: self.installedPack
            )
        }
    }

    func closeRuntime() {
        _ = waitForRuntime {
            await self.runtime.close()
            return true
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        finishInstallFailure("SelectionCanceled")
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        let callback = installCallback
        installCallback = nil
        documentPicker = nil

        DispatchQueue.global(qos: .userInitiated).async {
            let result = self.installSelectedDocuments(urls)
            DispatchQueue.main.async {
                switch result {
                case .success(let manifestJson):
                    callback?.onInstallCompleted(manifestJson: manifestJson)
                case .failure(let failure):
                    callback?.onInstallFailed(failureName: failure.rawValue)
                }
            }
        }
    }

    private func finishInstallFailure(_ failureName: String) {
        let callback = installCallback
        installCallback = nil
        documentPicker = nil
        callback?.onInstallFailed(failureName: failureName)
    }

    private func installSelectedDocuments(_ urls: [URL]) -> Result<String, ModelPackInstallFailureName> {
        do {
            let manifestSelection = try selectedManifest(from: urls)
            guard let manifestSelection else {
                return .failure(.missingManifest)
            }
            let manifest = manifestSelection.manifest

            if let failure = validateManifestStructure(manifest) {
                return .failure(failure)
            }
            guard verifySignature(manifest) else {
                return .failure(.invalidSignature)
            }
            guard let artifactUrl = try selectedArtifactUrl(from: urls, manifest: manifest) else {
                return .failure(.missingArtifact)
            }

            return try installArtifact(
                manifest: manifest,
                manifestJson: manifestSelection.json,
                artifactUrl: artifactUrl
            )
        } catch {
            return .failure(.storageFailed)
        }
    }

    private func selectedManifest(from urls: [URL]) throws -> ManifestSelection? {
        for url in urls {
            let access = url.startAccessingSecurityScopedResource()
            defer {
                if access {
                    url.stopAccessingSecurityScopedResource()
                }
            }

            let values = try url.resourceValues(forKeys: [.fileSizeKey, .nameKey])
            let name = values.name ?? url.lastPathComponent
            let size = values.fileSize ?? 0
            guard name.lowercased().hasSuffix(".json") || size <= Constants.maxManifestBytes else {
                continue
            }
            guard let data = try? Data(contentsOf: url), data.count <= Constants.maxManifestBytes else {
                continue
            }
            guard let manifest = try? JSONDecoder().decode(NativeModelPackManifest.self, from: data),
                  let json = String(data: data, encoding: .utf8) else {
                continue
            }
            return ManifestSelection(manifest: manifest, json: json)
        }
        return nil
    }

    private func selectedArtifactUrl(from urls: [URL], manifest: NativeModelPackManifest) throws -> URL? {
        for url in urls {
            let values = try url.resourceValues(forKeys: [.fileSizeKey, .nameKey])
            let name = values.name ?? url.lastPathComponent
            let size = Int64(values.fileSize ?? -1)
            if name == manifest.artifactFileName || size == manifest.artifactSizeBytes {
                return url
            }
        }
        return nil
    }

    private func validateManifestStructure(_ manifest: NativeModelPackManifest) -> ModelPackInstallFailureName? {
        if manifest.modelId != Constants.modelId || manifest.artifactFileName != Constants.artifactFileName {
            return .unapprovedModel
        }
        if manifest.runtimeId != Constants.runtimeId || !manifest.capabilities.contains("Generation") {
            return .invalidManifest
        }
        if manifest.signingKeyId != Constants.signingKeyId {
            return .untrustedSigningKey
        }
        if manifest.artifactSizeBytes <= 0 ||
            manifest.artifactChecksumSha256.isEmpty ||
            manifest.signature.isEmpty ||
            manifest.signatureAlgorithm != "Ed25519" {
            return .invalidManifest
        }
        return nil
    }

    private func verifySignature(_ manifest: NativeModelPackManifest) -> Bool {
        guard let signature = Data(base64Encoded: manifest.signature),
              var publicKeyBytes = Data(base64Encoded: Constants.publicKeyBase64) else {
            return false
        }
        if publicKeyBytes.count == Constants.x509Ed25519PublicKeyBytes {
            publicKeyBytes = publicKeyBytes.suffix(Constants.rawEd25519PublicKeyBytes)
        }
        guard publicKeyBytes.count == Constants.rawEd25519PublicKeyBytes,
              let signedData = signedManifestData(manifest) else {
            return false
        }

        do {
            let publicKey = try Curve25519.Signing.PublicKey(rawRepresentation: publicKeyBytes)
            return publicKey.isValidSignature(signature, for: signedData)
        } catch {
            return false
        }
    }

    private func signedManifestData(_ manifest: NativeModelPackManifest) -> Data? {
        let capabilities = Array(Set(manifest.capabilities)).sorted()
        let capabilitiesJson = capabilities.map { jsonString($0) }.joined(separator: ",")
        let json = "{\"modelId\":\(jsonString(manifest.modelId)),\"version\":\(manifest.version),\"displayName\":\(jsonString(manifest.displayName)),\"runtimeId\":\(jsonString(manifest.runtimeId)),\"artifactFileName\":\(jsonString(manifest.artifactFileName)),\"artifactSizeBytes\":\(manifest.artifactSizeBytes),\"artifactChecksumSha256\":\(jsonString(manifest.artifactChecksumSha256)),\"capabilities\":[\(capabilitiesJson)],\"signingKeyId\":\(jsonString(manifest.signingKeyId)),\"signatureAlgorithm\":\(jsonString(manifest.signatureAlgorithm))}"
        return Data(json.utf8)
    }

    private func installArtifact(
        manifest: NativeModelPackManifest,
        manifestJson: String,
        artifactUrl: URL
    ) throws -> Result<String, ModelPackInstallFailureName> {
        let access = artifactUrl.startAccessingSecurityScopedResource()
        defer {
            if access {
                artifactUrl.stopAccessingSecurityScopedResource()
            }
        }

        let root = try modelPacksRoot()
        let installDir = installedDirectory(for: manifest)
        let stageDir = root.appendingPathComponent("staged", isDirectory: true)
            .appendingPathComponent(installDir.lastPathComponent, isDirectory: true)
        let stagedArtifact = stageDir.appendingPathComponent("\(manifest.artifactFileName).part")

        try? FileManager.default.removeItem(at: stageDir)
        try FileManager.default.createDirectory(at: stageDir, withIntermediateDirectories: true)

        let checksumAndSize = try copyAndHash(from: artifactUrl, to: stagedArtifact)
        guard checksumAndSize.size == manifest.artifactSizeBytes else {
            try? FileManager.default.removeItem(at: stageDir)
            return .failure(.artifactSizeMismatch)
        }
        guard checksumAndSize.sha256Hex.caseInsensitiveCompare(manifest.artifactChecksumSha256) == .orderedSame else {
            try? FileManager.default.removeItem(at: stageDir)
            return .failure(.checksumMismatch)
        }

        try? FileManager.default.removeItem(at: installDir)
        try FileManager.default.createDirectory(at: installDir, withIntermediateDirectories: true)
        let installedArtifact = installDir.appendingPathComponent(manifest.artifactFileName)
        try FileManager.default.moveItem(at: stagedArtifact, to: installedArtifact)
        let manifestUrl = installDir.appendingPathComponent(Constants.manifestFileName)
        try manifestJson.write(to: manifestUrl, atomically: true, encoding: .utf8)
        try? FileManager.default.removeItem(at: stageDir)
        excludeFromBackup(root)
        excludeFromBackup(installDir)
        closeRuntime()

        return .success(manifestJson)
    }

    private func copyAndHash(from sourceUrl: URL, to destinationUrl: URL) throws -> (sha256Hex: String, size: Int64) {
        guard FileManager.default.createFile(atPath: destinationUrl.path, contents: nil) else {
            throw BridgeError.storage
        }

        let source = try FileHandle(forReadingFrom: sourceUrl)
        let destination = try FileHandle(forWritingTo: destinationUrl)
        defer {
            try? source.close()
            try? destination.close()
        }

        var hasher = SHA256()
        var copied: Int64 = 0
        while true {
            let data = try source.read(upToCount: Constants.copyChunkBytes) ?? Data()
            if data.isEmpty {
                break
            }
            hasher.update(data: data)
            try destination.write(contentsOf: data)
            copied += Int64(data.count)
        }
        let digest = hasher.finalize().map { String(format: "%02x", $0) }.joined()
        return (digest, copied)
    }

    private func installedPack() -> NativeInstalledModelPack? {
        guard let root = try? installedRoot(),
              let directories = try? FileManager.default.contentsOfDirectory(
                at: root,
                includingPropertiesForKeys: [.isDirectoryKey],
                options: [.skipsHiddenFiles]
              ) else {
            return nil
        }

        for directory in directories {
            let manifestUrl = directory.appendingPathComponent(Constants.manifestFileName)
            guard let data = try? Data(contentsOf: manifestUrl),
                  let manifest = try? JSONDecoder().decode(NativeModelPackManifest.self, from: data) else {
                continue
            }
            let artifact = directory.appendingPathComponent(manifest.artifactFileName)
            guard manifest.modelId == Constants.modelId,
                  manifest.runtimeId == Constants.runtimeId,
                  FileManager.default.fileExists(atPath: artifact.path),
                  (try? artifact.resourceValues(forKeys: [.fileSizeKey]).fileSize).map({ Int64($0) }) == manifest.artifactSizeBytes else {
                continue
            }
            return NativeInstalledModelPack(manifest: manifest, manifestUrl: manifestUrl, artifactUrl: artifact)
        }
        return nil
    }

    private func modelPacksRoot() throws -> URL {
        let root = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        ).appendingPathComponent("model-packs", isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        excludeFromBackup(root)
        return root
    }

    private func installedRoot() throws -> URL {
        let root = try modelPacksRoot().appendingPathComponent("installed", isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        return root
    }

    private func installedDirectory(for manifest: NativeModelPackManifest) throws -> URL {
        try installedRoot().appendingPathComponent(
            "\(safeFileSegment(manifest.modelId))-v\(manifest.version)",
            isDirectory: true
        )
    }

    private func safeFileSegment(_ value: String) -> String {
        value.map { character in
            if character.isASCII && (character.isLetter || character.isNumber || character == "." || character == "_" || character == "-") {
                return String(character)
            }
            return "_"
        }.joined()
    }

    private func excludeFromBackup(_ url: URL) {
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutableUrl = url
        try? mutableUrl.setResourceValues(values)
    }

    private func waitForRuntime<T>(_ work: @escaping () async throws -> T) -> T? {
        let semaphore = DispatchSemaphore(value: 0)
        let resultBox = ResultBox<T>()
        Task.detached {
            do {
                resultBox.result = .success(try await work())
            } catch {
                resultBox.result = .failure(error)
            }
            semaphore.signal()
        }
        guard semaphore.wait(timeout: .now() + Constants.runtimeTimeoutSeconds) == .success else {
            return nil
        }
        return try? resultBox.result?.get()
    }
}

private actor LiteRtLmRuntimeBox {
    private var engine: Engine?
    private var loadedModelKey: String?

    func load(installedPackProvider: () -> NativeInstalledModelPack?) async throws -> Bool {
        _ = try await ensureEngine(installedPackProvider: installedPackProvider)
        return true
    }

    func generate(
        prompt: String,
        installedPackProvider: () -> NativeInstalledModelPack?
    ) async throws -> String {
        let engine = try await ensureEngine(installedPackProvider: installedPackProvider)
        let conversation = try await engine.createConversation()
        var text = ""
        for try await chunk in conversation.sendMessageStream(Message(prompt)) {
            text += chunk.toString
        }
        return text
    }

    func close() {
        engine = nil
        loadedModelKey = nil
    }

    private func ensureEngine(installedPackProvider: () -> NativeInstalledModelPack?) async throws -> Engine {
        guard let pack = installedPackProvider() else {
            throw BridgeError.missingModel
        }
        let modelKey = pack.modelKey
        if let engine, loadedModelKey == modelKey {
            return engine
        }

        let cacheDir = try FileManager.default.url(
            for: .cachesDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        ).appendingPathComponent("litert-lm", isDirectory: true)
        try FileManager.default.createDirectory(at: cacheDir, withIntermediateDirectories: true)

        let config = try EngineConfig(
            modelPath: pack.artifactUrl.path,
            backend: .cpu(),
            cacheDir: cacheDir.path
        )
        let loaded = Engine(engineConfig: config)
        try await loaded.initialize()
        engine = loaded
        loadedModelKey = modelKey
        return loaded
    }
}

private final class ResultBox<T> {
    var result: Result<T, Error>?
}

private struct ManifestSelection {
    let manifest: NativeModelPackManifest
    let json: String
}

private struct NativeInstalledModelPack {
    let manifest: NativeModelPackManifest
    let manifestUrl: URL
    let artifactUrl: URL

    var modelKey: String {
        [
            manifest.modelId,
            "\(manifest.version)",
            manifest.artifactChecksumSha256,
            artifactUrl.path,
            "\(artifactUrl.fileSize ?? 0)",
            "\(artifactUrl.lastModifiedTime ?? 0)",
        ].joined(separator: "|")
    }
}

private struct NativeModelPackManifest: Codable {
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
}

private enum ModelPackInstallFailureName: String {
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

private enum BridgeError: Error {
    case missingModel
    case storage
}

private enum Constants {
    static let modelId = "Qwen/Qwen2.5-1.5B-Instruct"
    static let runtimeId = "litert-lm"
    static let artifactFileName = "qwen2_5_1_5b_instruct_bettamind_v1.litertlm"
    static let signingKeyId = "bettamind-model-prod-2026-01"
    static let publicKeyBase64 = "MCowBQYDK2VwAyEAGsgkjHlXsNaWfwbOzajfTImt5yC6nSGIEIVUL18EvKY="
    static let manifestFileName = "manifest.json"
    static let maxManifestBytes = 1_048_576
    static let copyChunkBytes = 1_048_576
    static let runtimeTimeoutSeconds = 120.0
    static let x509Ed25519PublicKeyBytes = 44
    static let rawEd25519PublicKeyBytes = 32
}

private extension URL {
    var fileSize: Int64? {
        (try? resourceValues(forKeys: [.fileSizeKey]).fileSize).map { Int64($0) }
    }

    var lastModifiedTime: TimeInterval? {
        guard let values = try? resourceValues(forKeys: [.contentModificationDateKey]),
              let modifiedDate = values.contentModificationDate else {
            return nil
        }
        return modifiedDate.timeIntervalSince1970
    }
}

private extension UIApplication {
    func bettamindTopViewController() -> UIViewController? {
        let root = connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
        return root?.bettamindTopPresentedViewController()
    }
}

private extension UIViewController {
    func bettamindTopPresentedViewController() -> UIViewController {
        if let navigation = self as? UINavigationController,
           let visible = navigation.visibleViewController {
            return visible.bettamindTopPresentedViewController()
        }
        if let tab = self as? UITabBarController,
           let selected = tab.selectedViewController {
            return selected.bettamindTopPresentedViewController()
        }
        if let presented = presentedViewController {
            return presented.bettamindTopPresentedViewController()
        }
        return self
    }
}

private func jsonString(_ value: String) -> String {
    var result = "\""
    for scalar in value.unicodeScalars {
        switch scalar {
        case "\"":
            result += "\\\""
        case "\\":
            result += "\\\\"
        case "\u{08}":
            result += "\\b"
        case "\u{0C}":
            result += "\\f"
        case "\n":
            result += "\\n"
        case "\r":
            result += "\\r"
        case "\t":
            result += "\\t"
        default:
            if scalar.value < 0x20 {
                result += String(format: "\\u%04x", scalar.value)
            } else {
                result += String(scalar)
            }
        }
    }
    result += "\""
    return result
}
