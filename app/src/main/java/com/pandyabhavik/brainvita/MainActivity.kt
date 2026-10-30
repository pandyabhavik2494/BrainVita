package com.pandyabhavik.brainvita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pandyabhavik.brainvita.ui.GameScreen
import com.pandyabhavik.brainvita.ui.GameViewModel
import com.pandyabhavik.brainvita.ui.theme.BrainVitaTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Recomputed whenever the Fold opens or closes, so the layout follows the screen in use.
            val sizeClass = calculateWindowSizeClass(this)
            val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(application))
            BrainVitaTheme {
                GameScreen(viewModel, sizeClass)
            }
        }
    }
}
