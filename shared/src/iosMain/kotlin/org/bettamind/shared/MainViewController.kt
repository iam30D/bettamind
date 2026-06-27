package org.bettamind.shared

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.remember

fun MainViewController() = ComposeUIViewController {
    val services = remember { createIosBettamindAppServices() }
    BettamindApp(services = services)
}

fun MainViewControllerWithNativeAiBridge(nativeAiBridge: IosNativeAiBridge) = ComposeUIViewController {
    val services = remember { createIosBettamindAppServices(nativeAiBridge) }
    BettamindApp(services = services)
}
