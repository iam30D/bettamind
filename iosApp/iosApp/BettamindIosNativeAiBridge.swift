import Foundation
import Shared

final class BettamindIosNativeAiBridge: NSObject, IosNativeAiBridge {
    func installerAvailable() -> Bool {
        false
    }

    func runtimeAvailable() -> Bool {
        false
    }

    func installedManifestJson() -> String? {
        nil
    }

    func requestUserInstall(callback: IosModelPackInstallCallback) -> Bool {
        callback.onInstallFailed(failureName: "PlatformVerifierUnavailable")
        return false
    }

    func removeInstalledModel() -> Bool {
        guard let root = installedRootIfPresent() else {
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
        false
    }

    func generate(prompt: String) -> String? {
        nil
    }

    func closeRuntime() {
    }

    private func installedRootIfPresent() -> URL? {
        guard let supportRoot = try? FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: false
        ) else {
            return nil
        }
        let root = supportRoot
            .appendingPathComponent("model-packs", isDirectory: true)
            .appendingPathComponent("installed", isDirectory: true)
        return FileManager.default.fileExists(atPath: root.path) ? root : nil
    }
}
