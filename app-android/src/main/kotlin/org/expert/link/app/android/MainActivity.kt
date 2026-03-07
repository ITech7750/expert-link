package org.expert.link.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.expert.link.app.shared.ui.ExpertLinkApp

/** Android entry point приложения. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpertLinkApp(AndroidPlatformServices(this))
        }
    }
}
