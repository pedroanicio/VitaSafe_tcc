package com.example.monitoramentosaudetcc

import android.media.MediaPlayer
import android.provider.Settings
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(id: String) {
    var bpm by remember { mutableStateOf(0) }
    var oxi by remember { mutableStateOf(0) }
    var temp by remember { mutableStateOf(0.0) }
    var queda by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val dbRef = FirebaseDatabase.getInstance().getReference("idosos/$id/leitura")

    // MediaPlayer para tocar som se queda = true
    val mediaPlayer = remember {
        MediaPlayer.create(context, Settings.System.DEFAULT_NOTIFICATION_URI)
    }

    LaunchedEffect(true) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bpm = snapshot.child("bpm").getValue(Int::class.java) ?: 0
                oxi = snapshot.child("oxigenacao").getValue(Int::class.java) ?: 0
                temp = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                val novaQueda = snapshot.child("queda").getValue(Boolean::class.java) ?: false
                if (novaQueda && !queda) {
                    mediaPlayer.start()
                }
                queda = novaQueda
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val bpmHistorico = remember { mutableStateListOf<Pair<String, Int>>() }

    LaunchedEffect(true) {
        FirebaseDatabase.getInstance()
            .getReference("idosos/$id/historico/2025-07-08").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<Pair<String, Int>>()
                snapshot.children.forEach {
                    val bpmValue = it.child("bpm").getValue(Int::class.java) ?: 0
                    val hora = it.child("hora").getValue(String::class.java) ?: ""
                    lista.add(Pair(hora, bpmValue))
                }
                bpmHistorico.clear()
                bpmHistorico.addAll(lista.sortedBy { it.first })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Obter a data atual no formato esperado
    val dataAtual = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

    LaunchedEffect(true) {
        FirebaseDatabase.getInstance()
            .getReference("idosos/$id/historico/$dataAtual")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lista = mutableListOf<Pair<String, Int>>()
                    snapshot.children.forEach {
                        val bpmValue = it.child("bpm").getValue(Int::class.java) ?: 0
                        val hora = it.child("hora").getValue(String::class.java) ?: ""
                        if (hora.isNotBlank()) lista.add(Pair(hora, bpmValue))
                    }
                    bpmHistorico.clear()
                    bpmHistorico.addAll(lista.sortedBy { it.first })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Monitoramento", style = MaterialTheme.typography.headlineSmall)
        val bpmMax = bpmHistorico.maxOfOrNull { it.second }
        val bpmMin = bpmHistorico.minOfOrNull { it.second }

        CircularIndicator("BPM", bpm.toFloat(), 200f, Color(0xFF2196F3), Icons.Default.Favorite, bpmMin = bpmMin, bpmMax = bpmMax)

        CircularIndicator("Oxigenação", oxi.toFloat(), 100f, Color(0xFF4CAF50), Icons.Default.Air)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Temperatura", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("${String.format("%.1f", temp)} ºC", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
        }

        if (queda) CardAlertaQueda()

        Spacer(Modifier.height(8.dp))
        GraficoBarras(bpmHistorico)
    }
}
@Composable
fun CircularIndicator(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bpmMin: Int? = null,
    bpmMax: Int? = null
) {
    val animatedValue by animateFloatAsState(targetValue = value)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    progress = (animatedValue / maxValue).coerceIn(0f, 1f),
                    color = color,
                    strokeCap = StrokeCap.Round,
                    strokeWidth = 8.dp,
                    modifier = Modifier.size(60.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    Text("${animatedValue.toInt()}", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                }
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(36.dp))
            }

            if (bpmMin != null && bpmMax != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mín: $bpmMin BPM", style = MaterialTheme.typography.bodySmall)
                    Text("Máx: $bpmMax BPM", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CardAlertaQueda() {
    // Anima a cor do alerta para piscar entre dois tons
    val infiniteTransition = rememberInfiniteTransition()
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFF44336),
        targetValue = Color(0xFFFFCDD2),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = animatedColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("ALERTA DE QUEDA!", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Queda detectada", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alerta de Queda",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun GraficoBarras(dados: List<Pair<String, Int>>) {
    if (dados.isEmpty()) {
        Text("Aguardando dados do histórico...")
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(8.dp)
    ) {
        val paddingLeft = 40.dp.toPx()
        val paddingBottom = 30.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingRight = 16.dp.toPx()

        val width = size.width - paddingLeft - paddingRight
        val height = size.height - paddingTop - paddingBottom

        val maxY = 150f
        val minY = 0f
        val barWidth = width / dados.size

        // Grades horizontais
        val stepsY = 4
        for (i in 0..stepsY) {
            val y = paddingTop + i * (height / stepsY)
            val valor = maxY - i * ((maxY - minY) / stepsY)
            drawLine(
                color = Color.LightGray,
                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                end = androidx.compose.ui.geometry.Offset(size.width - paddingRight, y),
                strokeWidth = 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${valor.toInt()}",
                8f,
                y + 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 28f
                }
            )
        }

        // Barras
        dados.forEachIndexed { i, (hora, bpm) ->
            val x = paddingLeft + i * barWidth + 10f
            val yRatio = ((bpm - minY) / (maxY - minY)).coerceIn(0f, 1f)
            val barHeight = yRatio * height
            val top = size.height - paddingBottom - barHeight

            drawRect(
                color = Color(0xFF2196F3),
                topLeft = androidx.compose.ui.geometry.Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth - 20f, barHeight)
            )

            // Legenda X
            if (i % (dados.size / 4).coerceAtLeast(1) == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    hora,
                    x,
                    size.height,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 22f
                    }
                )
            }
        }

        // Título
        drawContext.canvas.nativeCanvas.drawText(
            "Histórico de BPM",
            size.width / 2 - 100,
            20f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 36f
                isFakeBoldText = true
            }
        )
    }
}