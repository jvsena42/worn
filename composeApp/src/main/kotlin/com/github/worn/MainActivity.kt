package com.github.worn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.github.worn.ui.util.SharedPhoto
import com.github.worn.ui.util.readImageBytes
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sharedPhoto by mutableStateOf<SharedPhoto?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)

        setContent {
            App(
                sharedPhoto = sharedPhoto,
                onSharedPhotoConsumed = { sharedPhoto = null },
            )
        }
    }

    /** singleTask launch mode routes a share into the running instance rather than a new one. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val uri = intent
            ?.takeIf { it.action == Intent.ACTION_SEND && it.type?.startsWith("image/") == true }
            ?.let { IntentCompat.getParcelableExtra(it, Intent.EXTRA_STREAM, Uri::class.java) }
            ?: return

        lifecycleScope.launch {
            sharedPhoto = SharedPhoto(readImageBytes(this@MainActivity, uri))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
