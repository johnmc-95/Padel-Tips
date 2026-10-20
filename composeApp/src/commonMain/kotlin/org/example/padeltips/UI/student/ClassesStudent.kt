package org.example.padeltips.UI.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

// Data Classes preparadas para la serialización con Supabase (JSON).
// Usamos @SerialName para mapear las columnas de snake_case de SQL a camelCase en Kotlin.
@Serializable
data class ClaseAlumnoReal(
    val id: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("address") val direccion: String? = null,
    @SerialName("court_number") val courtNumber: Int? = null,
    val status: String = "scheduled",
    @SerialName("coach") val coach: Profile? = null
)

@Serializable
data class UpdateStatusClassStudent(val status: String)

@Serializable
data class MessageInsertStudent(
    @SerialName("coach_id") val coachId: String,
    @SerialName("student_id") val studentId: String,
    val content: String,
    val type: String
)

/**
 * Pantalla de Agenda del Alumno.
 * Gestiona la visualización de clases mediante un TabRow y el envío de mensajes asíncronos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesStudent(studentId: String, onVolver: () -> Unit, onSolicitarClase: () -> Unit) {

    val coroutineScope = rememberCoroutineScope()

    // Control de UI
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    val tabs = listOf("Próximas", "Historial")

    // Estados de Datos
    var misClases by remember { mutableStateOf<List<ClaseAlumnoReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // Gatillo para forzar la recarga

    // Usamos múltiples booleanos para asegurar que solo se muestra un flujo lógico a la vez
    var claseSeleccionada by remember { mutableStateOf<ClaseAlumnoReal?>(null) }
    var mostrarConfirmacionCancelacion by remember { mutableStateOf(false) }
    var mostrarDialogoMensaje by remember { mutableStateOf(false) }
    var textoMensaje by remember { mutableStateOf("") }
    var isSendingMessage by remember { mutableStateOf(false) }
    var mostrarExitoMensaje by remember { mutableStateOf(false) }

    // Descarga inicial de datos
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            // Consulta Relacional (Join) en Supabase:
            // Traemos las clases Y el perfil del coach asignado en una sola petición de red
            // optimizando el rendimiento.
            misClases = SupabaseClient.client
                .from("classes")
                .select(columns = Columns.raw("*, coach:profiles!coach_id(*)")) {
                    filter { eq("student_id", studentId) }
                }
                .decodeList<ClaseAlumnoReal>()
        } catch (e: Exception) {
            println("Error al cargar clases del alumno: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Filtrado en Cliente: Descargamos todo de golpe y dividimos las listas en local
    // Es más eficiente para colecciones pequeñas que hacer múltiples llamadas SQL
    val clasesProximas = misClases.filter { it.status == "scheduled" || it.status == "pending" }
    val clasesHistorial = misClases.filter { it.status == "cancelled" || it.status == "completed" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Agenda") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSolicitarClase,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Solicitar Clase", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Pestañas (TabRow)
            TabRow(selectedTabIndex = tabSeleccionada) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(selected = tabSeleccionada == index, onClick = { tabSeleccionada = index }, text = { Text(titulo) })
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val listaAMostrar = if (tabSeleccionada == 0) clasesProximas else clasesHistorial

                    if (listaAMostrar.isEmpty()) {
                        Text(
                            text = if (tabSeleccionada == 0) "No tienes próximas clases." else "Tu historial está vacío.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Usamos LazyColumn para renderizado eficiente de listas
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(listaAMostrar) { clase ->
                                TarjetaClaseAlumno(clase = clase, onClick = { claseSeleccionada = clase })
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) } // Margen inferior para el FAB
                        }
                    }
                }
            }
        }
    }

    // Flujo lógico de modales
    // El orden de las condiciones evita colisiones visuales

    // Detalles de la Clase
    if (claseSeleccionada != null && !mostrarConfirmacionCancelacion && !mostrarDialogoMensaje) {
        val clase = claseSeleccionada!!

        // Limpieza de fechas
        val fechaLimpia = clase.startTime.substringBefore("T")
        val horaInicioLimpia = clase.startTime.substringAfter("T").take(5)
        val horaFinLimpia = if (clase.endTime != null) {
            clase.endTime.substringAfter("T").take(5)
        } else "??"

        // Lógica de presentación para colores y textos
        val estadoVisible = when(clase.status) {
            "scheduled" -> "CONFIRMADA"
            "completed" -> "COMPLETADA"
            "cancelled" -> "CANCELADA"
            "pending" -> "PENDIENTE"
            else -> clase.status.uppercase()
        }
        val colorEstado = when(clase.status) {
            "scheduled" -> MaterialTheme.colorScheme.primary
            "cancelled" -> MaterialTheme.colorScheme.error
            "completed" -> MaterialTheme.colorScheme.secondary
            "pending" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurface
        }

        AlertDialog(
            onDismissRequest = { claseSeleccionada = null },
            title = { Text(text = "Detalles de la Clase", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Estado: $estadoVisible", color = colorEstado, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("👤 Profe: ${clase.coach?.nombreCompleto ?: "Desconocido"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📅 Fecha: $fechaLimpia")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("⏰ Hora: $horaInicioLimpia - $horaFinLimpia") // Mostramos rango
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📍 Lugar: ${clase.direccion ?: "Por definir"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🎾 Pista: ${clase.courtNumber ?: "Por definir"}")

                    // Solo permitimos acciones destructivas si la clase está activa
                    if (clase.status == "scheduled" || clase.status == "pending") {
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { mostrarDialogoMensaje = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Contactar al Profe")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { mostrarConfirmacionCancelacion = true }, // Saltamos al modal 2
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar Clase", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { claseSeleccionada = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Confirmación de Cancelación
    if (mostrarConfirmacionCancelacion && claseSeleccionada != null) {
        val clase = claseSeleccionada!!
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionCancelacion = false },
            title = { Text("¿Cancelar la clase?", color = MaterialTheme.colorScheme.error) },
            text = { Text("¿Estás seguro de que quieres cancelar esta clase? El profesor será notificado y esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // Ejecutamos un UPDATE en SQL mediante Supabase
                                SupabaseClient.client.from("classes")
                                    .update(UpdateStatusClassStudent("cancelled")) {
                                        filter { eq("id", clase.id) }
                                    }
                                mostrarConfirmacionCancelacion = false
                                claseSeleccionada = null
                                refreshTrigger++ // Forzamos recarga para que pase a "Historial" visualmente
                            } catch (e: Exception) {
                                println("Error al cancelar: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionCancelacion = false }) {
                    Text("No, mantenerla")
                }
            }
        )
    }

    // Enviar Mensaje
    if (mostrarDialogoMensaje && claseSeleccionada != null) {
        val clase = claseSeleccionada!!
        AlertDialog(
            // Bloqueamos la salida si está cargando (para evitar inconsistencias de estado)
            onDismissRequest = { if (!isSendingMessage) mostrarDialogoMensaje = false },
            title = { Text("Mensaje para ${clase.coach?.nombreCompleto ?: "el profe"}") },
            text = {
                Column {
                    Text("Escribe aquí tu duda, aviso de retraso o comentario sobre esta clase:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textoMensaje,
                        onValueChange = { textoMensaje = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5,
                        placeholder = { Text("Ej: Hola, llegaré 5 minutos tarde...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = textoMensaje.isNotBlank() && !isSendingMessage,
                    onClick = {
                        coroutineScope.launch {
                            isSendingMessage = true
                            try {
                                // Forzamos explícitamente el type='message' para que no tome el valor
                                // por defecto de la DB ('tip').
                                val nuevoMensaje = MessageInsertStudent(
                                    coachId = clase.coach?.id ?: "",
                                    studentId = studentId,
                                    content = textoMensaje,
                                    type = "message"
                                )
                                SupabaseClient.client.from("tips").insert(nuevoMensaje)

                                // Limpieza de estado
                                textoMensaje = ""
                                mostrarDialogoMensaje = false
                                claseSeleccionada = null
                                mostrarExitoMensaje = true

                            } catch (e: Exception) {
                                println("Error al enviar mensaje: ${e.message}")
                            } finally {
                                isSendingMessage = false
                            }
                        }
                    }
                ) {
                    if (isSendingMessage) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Enviar Mensaje")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSendingMessage,
                    onClick = {
                        mostrarDialogoMensaje = false
                        textoMensaje = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mensaje de Éxito
    if (mostrarExitoMensaje) {
        AlertDialog(
            onDismissRequest = { mostrarExitoMensaje = false },
            title = { Text("¡Mensaje Enviado!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("Tu mensaje se ha enviado con éxito. El profesor te responderá a la mayor brevedad posible.") },
            confirmButton = {
                Button(onClick = { mostrarExitoMensaje = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

// Componente Stateless (Sin Estado) para renderizar cada celda de la lista.
@Composable
fun TarjetaClaseAlumno(clase: ClaseAlumnoReal, onClick: () -> Unit) {

    val estadoVisible = when(clase.status) {
        "scheduled" -> "CONFIRMADA"
        "completed" -> "COMPLETADA"
        "cancelled" -> "CANCELADA"
        "pending" -> "PENDIENTE"
        else -> clase.status.uppercase()
    }

    val esCancelada = clase.status == "cancelled"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (esCancelada) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Fecha",
                    tint = if (esCancelada) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Limpieza de fechas
                val fechaLimpia = clase.startTime.substringBefore("T")
                val horaInicioLimpia = clase.startTime.substringAfter("T").take(5)
                val horaFinLimpia = if (clase.endTime != null) {
                    clase.endTime.substringAfter("T").take(5)
                } else "??"

                // Mostramos formato "Inicio - Fin"
                Text(
                    text = "$fechaLimpia • $horaInicioLimpia - $horaFinLimpia",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = clase.coach?.nombreCompleto ?: "Profesor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val textoEstado = if (clase.status != "scheduled") " • $estadoVisible" else ""
                Text(
                    text = (clase.direccion ?: "Lugar por definir") + textoEstado,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (esCancelada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}