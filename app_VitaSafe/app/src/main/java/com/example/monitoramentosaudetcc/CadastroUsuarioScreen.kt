package com.example.monitoramentosaudetcc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


@Composable
fun CadastroUsuarioScreen(navController: NavController) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("responsavel") }
    var clinica by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseDatabase.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cadastro", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome completo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))

        Text("Selecione o tipo do usuário:")
        Spacer(Modifier.height(8.dp))
        Row {
            RadioButton(
                selected = tipo == "responsavel",
                onClick = { tipo = "responsavel" }
            )
            Text("Responsável", modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = tipo == "medico",
                onClick = { tipo = "medico" }
            )
            Text("Médico", modifier = Modifier.padding(start = 4.dp))
        }

        // Campo da clínica só se for médico
        if (tipo == "medico") {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = clinica,
                onValueChange = { clinica = it },
                label = { Text("Clínica / Asilo") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                auth.createUserWithEmailAndPassword(email, senha)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: ""
                        val userData = hashMapOf<String, Any>(
                            "nome" to nome,
                            "email" to email,
                            "tipo" to tipo
                        ).apply {
                            if (tipo == "responsavel") {
                                this["idosos"] = listOf<String>()
                            }
                            if (tipo == "medico") {
                                this["clinica"] = clinica
                            }
                        }
                        db.getReference("usuarios").child(uid).setValue(userData)
                        navController.navigate("login") {
                            popUpTo("cadastroUsuario") { inclusive = true }
                        }
                    }
                    .addOnFailureListener {
                        erro = "Erro ao cadastrar: ${it.message}"
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Criar conta")
        }

        erro?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}