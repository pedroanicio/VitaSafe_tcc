package com.example.monitoramentosaudetcc

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("cadastroUsuario") { CadastroUsuarioScreen(navController) }
        composable("lista") { ListaIdososScreen(navController) }
        composable("dashboard/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            DashboardScreen(id)
        }

        composable("bluetoothProvision") {
            // Gere um ID temporário e passe adiante
            val id = UUID.randomUUID().toString()
            BluetoothProvisionScreen(navController)
        }
        composable("cadastroWifi/{id}/{mac}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val mac = backStackEntry.arguments?.getString("mac") ?: ""
            CadastroWiFiScreen(navController, id, mac)
        }
        composable("cadastroIdoso") {
            CadastroIdosoScreen(navController)
        }
    }
}