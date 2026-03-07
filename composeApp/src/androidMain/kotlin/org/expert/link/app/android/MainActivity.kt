package org.expert.link.app.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.platform.initializeAppPlatformContext
import org.expert.link.app.shared.ui.ExpertLinkApp

/** Android entry point приложения. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeAppPlatformContext(applicationContext)
        requestCallPermissionsIfNeeded()
        setContent {
            ExpertLinkApp(AppPlatformServices())
        }
    }

    private fun requestCallPermissionsIfNeeded() {
        val required = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )
        val missing = required.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 4201)
        }
    }
}
