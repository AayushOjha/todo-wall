package com.mohannic.taskarma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mohannic.taskarma.ui.theme.TaskarmaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Smooth splash exit
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.view.animate()
                .alpha(0f)
                .setDuration(400L)
                .withEndAction { splashScreenView.remove() }
                .start()
        }

        enableEdgeToEdge()

        setContent {
            AppRoot()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current

    // ── Persistent state (read once, then managed locally) ────────────────────
    var isDarkMode by remember { mutableStateOf(UserPreferences.isDarkMode(context)) }
    var userName   by remember { mutableStateOf(UserPreferences.getUserName(context)) }
    var onboarded  by remember { mutableStateOf(UserPreferences.isOnboardingDone(context)) }

    TaskarmaTheme(darkTheme = isDarkMode) {
        AnimatedContent(
            targetState = onboarded,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "root_nav"
        ) { isOnboarded ->
            if (!isOnboarded) {
                OnboardingScreen(
                    onFinished = { name ->
                        UserPreferences.setUserName(context, name)
                        UserPreferences.setOnboardingDone(context)
                        userName  = name
                        onboarded = true
                    }
                )
            } else {
                TodoScreen(
                    modifier      = Modifier.fillMaxSize(),
                    isDarkMode    = isDarkMode,
                    onToggleDarkMode = {
                        val newDark = !isDarkMode
                        isDarkMode  = newDark
                        UserPreferences.setDarkMode(context, newDark)
                        // Force a wallpaper re-render by invalidating the stored hash
                        UserPreferences.setLastWallpaperHash(context, 0)
                    },
                    userName      = userName,
                    onUserNameChanged = { newName ->
                        userName = newName
                        UserPreferences.setUserName(context, newName)
                    }
                )
            }
        }
    }
}
