package com.github.worn

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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
import com.github.worn.ui.util.ShortcutCommand
import com.github.worn.ui.util.readImageBytes
import com.github.worn.ui.util.shortcutCommandFor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sharedPhoto by mutableStateOf<SharedPhoto?>(null)
    private var shortcut by mutableStateOf<ShortcutCommand?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Transparent bars with `auto` styles: the system picks light or dark bar icons from the
        // current uiMode, so status-bar glyphs stay legible in both themes. The default overload
        // assumes a light scrim and would leave dark icons on the dark page.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            App(
                sharedPhoto = sharedPhoto,
                onSharedPhotoConsumed = { sharedPhoto = null },
                shortcut = shortcut,
                onShortcutConsumed = { shortcut = null },
            )
        }
    }

    /**
     * singleTask launch mode routes a share or a launcher shortcut into the running instance rather
     * than a new one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        shortcutCommandFor(intent.action)?.let { shortcut = it }
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
