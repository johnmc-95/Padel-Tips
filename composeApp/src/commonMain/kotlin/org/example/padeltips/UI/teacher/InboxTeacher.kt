package org.example.padeltips.UI.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

@Serializable
data class MensajeAlumno(
    val id: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    // JOIN con la tabla de perfiles para traernos el remitente (el alumno)
    @SerialName("alumno") val alumno: Profile? = null
)

/**
 * Pantalla Bandeja de Entrada del Profesor.
 * Muestra los mensajes enviados por los alumnos relacionados con las clases o dudas generales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTeacher(coachId: String, onVolver: () -> Unit) {
    // Estados de la UI
    var mensajes by remember { mutableStateOf<List<MensajeAlumno>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ejecutamos un Trigger para forzar la recarga de datos al pulsar el botón de actualizar
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Efecto secundario que se lanza al iniciar y cada vez que refreshTrigger cambia
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            // Consulta a Supabase con filtro doble (AND lógico por defecto)
            val resultado = SupabaseClient.client
                .from("tips")
                .select(columns = Columns.raw("*, alumno:profiles!student_id(*)")) {
                    filter {
                        eq("coach_id", coachId)
                        // FILTRO CRÍTICO: Excluimos los consejos técnicos para mostrar solo mensajes
                        eq("type", "message")
                    }
                }
                .decodeList<MensajeAlumno>()

            // Procesamiento en cliente: Ordenamos la lista cronológicamente descendente
            // (Los mensajes más nuevos aparecerán arriba del todo).
            mensajes = resultado.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            println("Error al cargar mensajes: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bandeja de Entrada") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    // Botón manual de refresco
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Contenedor principal
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // --- ÁRBOL DE DECISIÓN DE ESTADOS ---
            if (isLoading) {
                // Estado 1: Loading
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (mensajes.isEmpty()) {
                // Estado 2: Bandeja Vacía
                // Es buena práctica de UX mostrar un icono amigable cuando no hay contenido
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Sin mensajes",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant // Color suave
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes mensajes de tus alumnos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Estado 3: Con mensajes
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mensajes) { mensaje ->
                        TarjetaMensaje(mensaje)
                    }
                }
            }
        }
    }
}

/**
 * COMPONENTE DE UI: Tarjeta de Mensaje.
 */
@Composable
fun TarjetaMensaje(mensaje: MensajeAlumno) {
    // --- PARSEO DE TIMESTAMP ---
    // La BBDD devuelve la fecha en formato ISO 8601 (ej: 2026-04-02T12:20:14.000Z)
    // Limpiamos los strings para separar la fecha y quedarnos con horas y minutos.
    val fechaLimpia = mensaje.createdAt?.substringBefore("T") ?: "Recientemente"
    val horaLimpia = mensaje.createdAt?.substringAfter("T")?.substringBeforeLast(":")?.substringBeforeLast(".") ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera del mensaje: Remitente a la izquierda, Fecha a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "De: ${mensaje.alumno?.nombreCompleto ?: "Alumno desconocido"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$fechaLimpia $horaLimpia",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cuerpo del mensaje
            Text(
                text = mensaje.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}