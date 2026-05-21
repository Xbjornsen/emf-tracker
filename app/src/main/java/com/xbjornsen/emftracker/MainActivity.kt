package com.xbjornsen.emftracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.xbjornsen.emftracker.ui.navigation.AppNavigation
import com.xbjornsen.emftracker.ui.theme.EMFTrackerTheme
import com.xbjornsen.emftracker.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EMFTrackerTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
