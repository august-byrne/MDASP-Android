package com.augustbyrne.mdaspcompanion

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import java.util.*

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    coroutineScope: CoroutineScope,
    navController: NavHostController,
    snackbarState: SnackbarHostState
) {
    val context = LocalContext.current
    //val audioModel by viewModel.audioModel
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = "scanner"
    ) {
        composable("scanner") {
            ScanListUI(viewModel) { scan: ScanResult ->
                viewModel.device.value = scan.device
                ConnectionManager.connect(scan.device, context)
                navController.navigate("mdasp_controller")
            }
        }
        composable(
            route = "mdasp_controller"
        ) {
            MDASPControlUI(viewModel) {
                navController.popBackStack()
            }
        }
    }
}
