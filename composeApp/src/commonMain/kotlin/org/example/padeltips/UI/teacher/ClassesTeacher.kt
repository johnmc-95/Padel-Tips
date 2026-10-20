package org.example.padeltips.UI.teacher

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

@Serializable
data class ClaseReal(
    val id: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("address") val direccion: String? = null,
    @SerialName("court_number") val courtNumber: Int? = null,
    val status: String = "scheduled", // Valores posibles: scheduled, pending, cancelled, completed
    @SerialName("alumno") val alumno: Profile? = null
)

// DTO minificado para hacer actualizaciones parciales (PATCH/UPDATE) eficientes en BBDD
@Serializable
data class UpdateStatusClass(val status: String)

/**
 * Pantalla de Agenda del Profesor.
 * Gestiona clases confirmadas y aprueba/rechaza solicitudes entrantes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesTeacher(coachId: String, onVolver: () -> Unit, onAñadirClase: () -> Unit) {

    val coroutineScope = rememberCoroutineScope()

    // Estados de la interfaz
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    val tabs = listOf("Agenda", "Solicitudes")

    // Estado de datos
    var misClases by remember { mutableStateOf<List<ClaseReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados para modales
    var claseSeleccionada by remember { mutableStateOf<ClaseReal?>(null) }
    var mostrarConfirmacionCancelacion by remember { mutableStateOf(false) }
    // --- NUEVO: Estado para el pop-up de éxito al aceptar ---
    var mostrarExitoAceptacion by remember { mutableStateOf(false) }

    // Gatillo reactivo: Incrementar esta variable fuerza a LaunchedEffect a re-ejecutarse
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Carga de datos centralizada
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            misClases = SupabaseClient.client
                .from("classes")
                .select(columns = Columns.raw("*, alumno:profiles!student_id(*)")) {
                    filter { eq("coach_id", coachId) }
                }
                .decodeList<ClaseReal>()
        } catch (e: Exception) {
            println("Error al cargar las clases: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Filtrado en memoria: Es más rápido que hacer dos peticiones SQL separadas
    val clasesAgenda = misClases.filter { it.status == "scheduled" }
    val clasesSolicitudes = misClases.filter { it.status == "pending" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda de Clases") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    // Botón de recarga manual para forzar la sincronización con BBDD
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAñadirClase,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Clase", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Pestañas (TabRow)
            TabRow(selectedTabIndex = tabSeleccionada) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index },
                        text = {
                            // Dinamismo de UI: Mostramos un contador tipo "Notificación"
                            // si hay solicitudes pendientes. Ejemplo: "Solicitudes (3)"
                            val texto = if (index == 1 && clasesSolicitudes.isNotEmpty()) "$titulo (${clasesSolicitudes.size})" else titulo
                            Text(texto)
                        }
                    )
                }
            }

            // Contenido de la lista
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val listaAMostrar = if (tabSeleccionada == 0) clasesAgenda else clasesSolicitudes

                    if (listaAMostrar.isEmpty()) {
                        Text(
                            text = if (tabSeleccionada == 0) "No tienes clases programadas." else "No tienes solicitudes pendientes.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(listaAMostrar) { clase ->
                                // Delegamos el click para abrir el modal
                                TarjetaClaseReal(clase = clase, onClick = { claseSeleccionada = clase })
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) } // Salva el FAB
                        }
                    }
                }
            }
        }
    }

    // Añadimos la condición "!mostrarExitoAceptacion" para que este modal se cierre al abrirse el de éxito
    if (claseSeleccionada != null && !mostrarConfirmacionCancelacion && !mostrarExitoAceptacion) {
        val clase = claseSeleccionada!!

        // Limpieza de fechas
        val fechaLimpia = clase.startTime.substringBefore("T")
        // "2026-04-10T18:00:00Z" -> Corta la "T", coge lo de la derecha, y coge los 5 primeros caracteres ("18:00")
        val horaInicioLimpia = clase.startTime.substringAfter("T").take(5)

        // Comprobamos si existe la fecha de fin
        val horaFinLimpia = if (clase.endTime != null) {
            clase.endTime.substringAfter("T").take(5)
        } else {
            "??"
        }

        // Esta variable de estado determina si el modal actúa como visor de detalles
        // o como formulario de aprobación de solicitud.
        val esSolicitud = clase.status == "pending"

        AlertDialog(
            onDismissRequest = { claseSeleccionada = null },
            title = { Text(text = if (esSolicitud) "Nueva Solicitud" else "Detalles de la Clase", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("👤 Alumno: ${clase.alumno?.nombreCompleto ?: "Desconocido"}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📅 Fecha: $fechaLimpia")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("⏰ Hora: $horaInicioLimpia - $horaFinLimpia") // Mostramos el rango
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📍 Dirección: ${clase.direccion ?: "A convenir"}")

                    if (!esSolicitud) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🎾 Pista: ${clase.courtNumber ?: "Por definir"}")
                    }
                }
            },
            confirmButton = {
                if (esSolicitud) {
                    // Acción Positiva: Aceptar la clase
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                SupabaseClient.client.from("classes").update(UpdateStatusClass("scheduled")) {
                                    filter { eq("id", clase.id) }
                                }
                                mostrarExitoAceptacion = true
                            } catch (e: Exception) { println(e.message) }
                        }
                    }) {
                        Text("Aceptar Clase")
                    }
                } else {
                    // Si es una clase ya agendada, el botón solo cierra el modal
                    Button(onClick = { claseSeleccionada = null }) { Text("Cerrar") }
                }
            },
            dismissButton = {
                // Acción Negativa: Rechazar o Cancelar
                OutlinedButton(onClick = {
                    mostrarConfirmacionCancelacion = true
                }) {
                    Text(if (esSolicitud) "Rechazar" else "Cancelar Clase", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Mostrar mensaje de aceptación de clase de la solicitud enviada por el alumno
    if (mostrarExitoAceptacion) {
        AlertDialog(
            onDismissRequest = { /* Bloqueamos cierre accidental */ },
            title = { Text("¡Clase Aceptada!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("La solicitud ha sido confirmada correctamente. Ahora aparecerá en tu pestaña de Agenda y el alumno podrá verla como confirmada.") },
            confirmButton = {
                Button(onClick = {
                    // Limpiamos los estados y recargamos la lista
                    mostrarExitoAceptacion = false
                    claseSeleccionada = null
                    refreshTrigger++
                }) {
                    Text("Genial")
                }
            }
        )
    }

    // Confirmación de la cancelación de la clase
    if (mostrarConfirmacionCancelacion && claseSeleccionada != null) {
        val clase = claseSeleccionada!!
        val esSolicitud = clase.status == "pending"

        AlertDialog(
            onDismissRequest = { mostrarConfirmacionCancelacion = false },
            title = { Text(if (esSolicitud) "¿Rechazar solicitud?" else "¿Cancelar la clase?", color = MaterialTheme.colorScheme.error) },
            text = { Text(if (esSolicitud) "El alumno será notificado de que no has podido aceptar la clase." else "¿Estás seguro de que quieres cancelar esta clase? El alumno será notificado y esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                SupabaseClient.client.from("classes")
                                    .update(UpdateStatusClass("cancelled")) {
                                        filter { eq("id", clase.id) }
                                    }
                                mostrarConfirmacionCancelacion = false
                                claseSeleccionada = null
                                refreshTrigger++
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
}

/**
 * UI COMPONENT: Tarjeta de Clase.
 * Reacciona visualmente (cambia de color) si representa una solicitud pendiente.
 */
@Composable
fun TarjetaClaseReal(clase: ClaseReal, onClick: () -> Unit) {
    val esSolicitud = clase.status == "pending"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            // Condicional de color basado en la paleta global
            containerColor = if (esSolicitud) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (esSolicitud) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Fecha",
                    tint = if (esSolicitud) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // --- LIMPIEZA DE FECHAS (Tarjeta) ---
                val fechaLimpia = clase.startTime.substringBefore("T")
                val horaInicioLimpia = clase.startTime.substringAfter("T").take(5)

                val horaFinLimpia = if (clase.endTime != null) {
                    clase.endTime.substringAfter("T").take(5)
                } else {
                    "??"
                }

                // Ahora mostramos "18:00 - 19:30"
                Text(
                    text = "$fechaLimpia • $horaInicioLimpia - $horaFinLimpia",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (esSolicitud) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = clase.alumno?.nombreCompleto ?: "Alumno desconocido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val infoExtra = if (esSolicitud) "Nueva solicitud" else "Pista ${clase.courtNumber ?: "?"}"
                Text(
                    text = infoExtra,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}