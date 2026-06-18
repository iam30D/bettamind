package org.bettamind.android

import android.os.Bundle
import androidx.activity.compose.setContent
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import org.bettamind.shared.BettamindApp

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        setContent {
            BettamindApp()
        }
    }
}
