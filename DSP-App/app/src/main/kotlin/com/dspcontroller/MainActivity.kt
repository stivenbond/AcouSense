package com.dspcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dspcontroller.ui.navigation.NavGraph
import com.dspcontroller.ui.theme.DspControllerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity host for the DSP Controller application.
 *
 * Uses edge-to-edge rendering and delegates all navigation to
 * the Compose [NavGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DspControllerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }
}
