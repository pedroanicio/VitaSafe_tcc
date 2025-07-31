package com.example.monitoramentosaudetcc

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.UUID

import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BluetoothProvisionScreen (navController: NavController) {
    val context = LocalContext.current
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val devices = remember { mutableStateListOf<BluetoothDevice>() }

    val scanner = bluetoothAdapter?.bluetoothLeScanner
    val scanning = remember { mutableStateOf(false) }

    val ssid = "Eric"
    val password = "pedrolucas02"

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("Permissao", "${it.key} = ${it.value}")
        }
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (!devices.any { it.address == device.address }) {
                    devices.add(device)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val bluetoothScanPermission = Manifest.permission.BLUETOOTH_SCAN
        val bluetoothConnectPermission = Manifest.permission.BLUETOOTH_CONNECT
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        val hasScan = ContextCompat.checkSelfPermission(context, bluetoothScanPermission) == PackageManager.PERMISSION_GRANTED
        val hasConnect = ContextCompat.checkSelfPermission(context, bluetoothConnectPermission) == PackageManager.PERMISSION_GRANTED
        val hasFineLocation = ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(context, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "Por favor, ative a localização", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        if (!bluetoothAdapter?.isEnabled!!) {
            Toast.makeText(context, "Por favor, ative o Bluetooth", Toast.LENGTH_LONG).show()
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(enableBtIntent)
        } else if (!hasScan || !hasConnect || (!hasFineLocation && !hasCoarseLocation)) {
            launcher.launch(arrayOf(bluetoothScanPermission, bluetoothConnectPermission, fineLocationPermission, coarseLocationPermission))
        } else {
            scanner?.startScan(scanCallback)
            scanning.value = true
            delay(10000) // Para a varredura após 10 segundos
            scanner?.stopScan(scanCallback)
            scanning.value = false
        }
    }

    LaunchedEffect(Unit) {
        scanning.value = true
        scanner?.startScan(scanCallback)
    }

    DisposableEffect(Unit) {
        onDispose {
            scanning.value = false
            scanner?.stopScan(scanCallback)
        }
    }

    fun connectAndSend(device: BluetoothDevice) {
        device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))

                    if (characteristic != null) {
                        val command = "SSID=$ssid;PASS=$password"
                        characteristic.value = command.toByteArray()
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            val result = gatt.writeCharacteristic(characteristic)
                            Log.d("BLE", "Tentando escrever characteristic: $result")
                            Log.d("BLE", "Comando enviado: $command")
                        }
                    } else {
                        Toast.makeText(context, "Characteristic não encontrada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Erro ao descobrir serviços", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(context, "Dados enviados com sucesso!", Toast.LENGTH_SHORT).show()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.disconnect()
                    }
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Falha ao enviar dados", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Conectar dispositivo Bluetooth", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Text("Procurando dispositivos...")
        } else {
            LazyColumn {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                connectAndSend(device)
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(device.name ?: "Dispositivo sem nome")
                            Spacer(Modifier.weight(1f))
                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}