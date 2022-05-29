package com.augustbyrne.mdaspcompanion

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.compiler.plugins.kotlin.ComposeFqNames.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import kotlin.coroutines.coroutineContext

const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
const val LOCATION_PERMISSION_REQUEST_CODE = 2


@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

    private val myViewModel: MainViewModel by viewModels()

    /*******************************************
     * Properties
     *******************************************/

/*    private var isScanning = false
        set(value) {
            field = value
            //runOnUiThread { scan_button.text = if (value) "Stop Scan" else "Start Scan" }
        }*/

    //private val scanResults = mutableListOf<ScanResult>()
/*    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }*/

    /*******************************************
     * Activity function overrides
     *******************************************/

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //DynamicColors.applyToActivitiesIfAvailable(application)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // remove system insets as we will handle these ourselves
        WindowCompat.setDecorFitsSystemWindows(window, false)

/*        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = bluetoothManager.adapter
        val btScanner = btAdapter.bluetoothLeScanner*/

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

    override fun onResume() {
        super.onResume()
        //ConnectionManager.registerListener(connectionEventListener)

/*        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }*/
    }
/*

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    startBleScan(myViewModel, this, btScanner)
                }
            }
        }
    }
*/

    /*******************************************
     * Private functions
     *******************************************/

/*    @SuppressLint("MissingPermission")
    private fun promptEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
    }*/
}
