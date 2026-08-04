package com.example.monitoramentosaudetcc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaIdososScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var tipoUsuario by remember { mutableStateOf<String?>(null) }
    var nomeUsuario by remember { mutableStateOf<String>("") }
    var idososPermitidos by remember { mutableStateOf(listOf<String>()) }
    var idosos by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    val db = FirebaseDatabase.getInstance()

    // Buscar dados do usuário logado
    LaunchedEffect(uid) {
        if (uid != null) {
            db.getReference("usuarios").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        tipoUsuario = snapshot.child("tipo").getValue(String::class.java)
                        nomeUsuario = snapshot.child("nome").getValue(String::class.java) ?: ""
                        if (tipoUsuario == "responsavel") {
                            val lista = snapshot.child("idosos").children.mapNotNull { it.getValue(String::class.java) }
                            idososPermitidos = lista
                        } else if (tipoUsuario == "medico") {
                            val clinica = snapshot.child("clinica").getValue(String::class.java)
                            idososPermitidos = listOf("ALL_$clinica")
                        } else {
                            idososPermitidos = listOf("ALL")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    // Buscar lista de idosos
    LaunchedEffect(idososPermitidos) {
        if (idososPermitidos.isNotEmpty()) {
            db.getReference("idosos").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lista = mutableListOf<Pair<String, String>>()
                    snapshot.children.forEach { idosoSnapshot ->
                        val id = idosoSnapshot.key ?: ""
                        val nome = idosoSnapshot.child("nome").getValue(String::class.java) ?: "Sem Nome"
                        val clinica = idosoSnapshot.child("clinica").getValue(String::class.java) ?: ""

                        val podeMostrar = when {
                            idososPermitidos.contains("ALL") -> true
                            idososPermitidos.any { it.startsWith("ALL_") } -> {
                                val clinicaPermitida = idososPermitidos.first().substring(4)
                                clinica == clinicaPermitida
                            }
                            else -> idososPermitidos.contains(id)
                        }

                        if (podeMostrar) {
                            lista.add(Pair(id, nome))
                        }
                    }
                    idosos = lista
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bem-vindo, $nomeUsuario") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("lista") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text("Idosos cadastrados", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                if (tipoUsuario == "medico") {
                    Text(
                        "Asilo de Ouro Branco",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                if (idosos.isEmpty()) {
                    Text("Nenhum idoso disponível para este usuário.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(idosos.size) { index ->
                            val (id, nome) = idosos[index]
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("dashboard/$id")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = nome.firstOrNull()?.uppercase() ?: "",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(nome, style = MaterialTheme.typography.titleMedium)
                                        Text("Clique para monitorar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { navController.navigate("cadastroIdoso") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Cadastrar")
            }
        }
    }
}