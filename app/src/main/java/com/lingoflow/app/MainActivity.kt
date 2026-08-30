package com.lingoflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.settings.InterfaceStyle
import com.lingoflow.app.domain.model.settings.ThemeMode
import com.lingoflow.app.domain.repository.SettingsRepository
import com.lingoflow.app.ui.i18n.LocalStrings
import com.lingoflow.app.ui.i18n.stringsFor
import com.lingoflow.app.ui.navigation.LingoFlowNavHost
import com.lingoflow.app.ui.theme.LingoFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the system splash on screen until the persisted theme/language
        // has been read, so Editorial/Chinese users never see an
        // English/system-theme frame flash before the real UI.
        val splashScreen = installSplashScreen()
        val settingsLoaded = mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !settingsLoaded.value }
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = null)
            settingsLoaded.value = settings != null

            val themeMode = settings?.themeMode ?: ThemeMode.SYSTEM
            val interfaceStyle = settings?.interfaceStyle ?: InterfaceStyle.MODERN
            val strings = stringsFor(settings?.appLanguage ?: AppLanguage.ENGLISH)

            CompositionLocalProvider(LocalStrings provides strings) {
                LingoFlowTheme(themeMode = themeMode, interfaceStyle = interfaceStyle) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LingoFlowNavHost()
                    }
                }
            }
        }
    }
}
