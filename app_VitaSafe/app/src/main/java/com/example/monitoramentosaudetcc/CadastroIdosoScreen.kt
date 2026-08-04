package com.example.monitoramentosaudetcc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import java.util.*

@Composable
fun CadastroIdosoScreen(navController: NavController) {
    var nome by remember { mutableStateOf("") }
    var clinica by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    Column(
        modifier = Modifier
            .padding(16.dp)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Text("Cadastro de Idoso", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = clinica,
            onValueChange = { clinica = it },
            label = { Text("Clínica") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val id = UUID.randomUUID().toString()
            val db = FirebaseDatabase.getInstance().reference

            // Salva o idoso no nó /idosos
            db.child("idosos").child(id).setValue(
                mapOf(
                    "nome" to nome,
                    "clinica" to clinica,
                    "leitura" to mapOf(
                        "bpm" to 0,
                        "oxigenacao" to 0,
                        "temperatura" to 0,
                        "queda" to false
                    )
                )
            )

            // se for um responsável, adiciona o id no /usuarios/$uid/idosos
            if (uid != null) {
                val userRef = db.child("usuarios").child(uid).child("idosos")
                userRef.get().addOnSuccessListener { snapshot ->
                    val type = object : GenericTypeIndicator<MutableList<String>>() {}
                    val lista = snapshot.getValue(type) ?: mutableListOf()
                    lista.add(id)
                    userRef.setValue(lista)
                }
            }

            navController.navigate("dashboard/$id")
        }) {
            Text("Cadastrar")
        }
    }
}