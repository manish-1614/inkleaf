package com.inkleaf.app.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.inkleaf.app.data.preferences.ReaderPreferencesRepository
import com.inkleaf.app.data.saf.SafDocumentRepository
import com.inkleaf.app.ui.home.HomeScreen
import com.inkleaf.app.ui.reader.ReaderScreen
import com.inkleaf.app.ui.theme.InkleafTheme
import com.inkleaf.app.ui.theme.ReaderThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var safRepository: SafDocumentRepository
    private lateinit var preferencesRepository: ReaderPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("InkleafCrash", "Uncaught exception on thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        super.onCreate(savedInstanceState)
        
        safRepository = SafDocumentRepository(applicationContext)
        preferencesRepository = ReaderPreferencesRepository(applicationContext)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val themeMode by preferencesRepository.themeFlow.collectAsState(initial = ReaderThemeMode.SEPIA)
            var openDocumentUri by remember { mutableStateOf<Uri?>(null) }

            // Handle incoming intents (Android open-with)
            LaunchedEffect(intent) {
                if (intent?.action == android.content.Intent.ACTION_VIEW) {
                    intent.data?.let { uri ->
                        openDocumentUri = uri
                        val metadata = safRepository.getDocumentMetadata(uri)
                        metadata?.let {
                            preferencesRepository.addRecentDocument(it.uriString, it.displayName)
                        }
                    }
                }
            }

            InkleafTheme(readerThemeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUri = openDocumentUri
                    if (currentUri != null) {
                        ReaderScreen(
                            documentUri = currentUri,
                            safRepository = safRepository,
                            preferencesRepository = preferencesRepository,
                            themeMode = themeMode,
                            onThemeChange = { newTheme ->
                                coroutineScope.launch {
                                    preferencesRepository.setThemeMode(newTheme)
                                }
                            },
                            onBack = { openDocumentUri = null }
                        )
                    } else {
                        HomeScreen(
                            preferencesRepository = preferencesRepository,
                            onDocumentSelect = { uri ->
                                coroutineScope.launch {
                                    openDocumentUri = uri
                                    val metadata = safRepository.getDocumentMetadata(uri)
                                    metadata?.let {
                                        preferencesRepository.addRecentDocument(it.uriString, it.displayName)
                                    }
                                }
                            },
                            currentTheme = themeMode,
                            onThemeChange = { newTheme ->
                                coroutineScope.launch {
                                    preferencesRepository.setThemeMode(newTheme)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
