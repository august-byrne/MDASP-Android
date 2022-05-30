package com.augustbyrne.mdaspcompanion

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.augustbyrne.mdaspcompanion.ble.ConnectionEventListener
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

    private val myViewModel: MainViewModel by viewModels()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivitiesIfAvailable(application)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // remove system insets as we will handle these ourselves
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ConnectionManager.registerListener(myViewModel.connectionEventListener)

        setContent {
            val navController = rememberNavController()
            //val navBackStackEntry by navController.currentBackStackEntryAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            NavGraph(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                viewModel = myViewModel,
                coroutineScope = coroutineScope,
                navController = navController,
                snackbarState = snackbarHostState
            )
        }
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun onDestroy() {
        ConnectionManager.unregisterListener(myViewModel.connectionEventListener)
        super.onDestroy()
    }
}