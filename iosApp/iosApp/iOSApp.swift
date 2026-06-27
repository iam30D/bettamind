import Darwin
import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let nativeAiBridge: BettamindIosNativeAiBridge

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewControllerWithNativeAiBridge(nativeAiBridge: nativeAiBridge)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

@main
struct BettamindIOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let nativeAiBridge = BettamindIosNativeAiBridge()

    init() {
        IosStorageValidationLauncher.runIfRequested()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                ComposeView(nativeAiBridge: nativeAiBridge)
                    .ignoresSafeArea()
                    .privacySensitive()

                if scenePhase != .active {
                    Color(.systemBackground)
                        .ignoresSafeArea()
                }
            }
        }
    }
}

private enum IosStorageValidationLauncher {
    static func runIfRequested() {
        guard ProcessInfo.processInfo.environment["BETTAMIND_IOS_STORAGE_VALIDATION"] == "1" else {
            return
        }

        let keychainAccessGroup = ProcessInfo.processInfo.environment["BETTAMIND_IOS_STORAGE_KEYCHAIN_ACCESS_GROUP"]
        let result = IosEncryptedStorageAppValidationKt.runIosEncryptedStorageAppValidationWithAccessGroup(
            keychainAccessGroup: keychainAccessGroup
        )
        let resultURL = URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)
            .appendingPathComponent("bettamind-ios-storage-validation.txt")

        do {
            try "\(result)\n".write(to: resultURL, atomically: true, encoding: .utf8)
        } catch {
            print("BETTAMIND_IOS_STORAGE_VALIDATION_RESULT FAIL: could not write result file: \(error)")
            fflush(stdout)
            Darwin.exit(1)
        }

        print("BETTAMIND_IOS_STORAGE_VALIDATION_RESULT \(result)")
        fflush(stdout)
        let exitCode: Int32 = result.hasPrefix("PASS:") ? 0 : 1
        Darwin.exit(exitCode)
    }
}
