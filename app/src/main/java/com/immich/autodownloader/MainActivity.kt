package com.immich.autodownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.immich.autodownloader.ui.ImmichSyncRoot
import com.immich.autodownloader.ui.theme.ImmichAutoDownloaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImmichAutoDownloaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val app = application as ImmichSyncApp
                    ImmichSyncRoot(container = app.container)
                }
            }
        }
    }
}
