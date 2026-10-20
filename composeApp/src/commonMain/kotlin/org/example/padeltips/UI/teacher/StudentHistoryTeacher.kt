package org.example.padeltips.UI.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

/**
 * Pantalla del Historial de Clases de un Alumno Específico (Vista del Profesor).
 * * LÓGICA DE NEGOCIO: Muestra la relación "Many-to-Many" (muchos a muchos)
 * entre el profesor y el alumno a través de la tabla 'classes'.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHistoryTeacher(
    coachId: String,
    alumno: Profile, // Recibimos el perfil para el título y el ID
    onVolver: () -> Unit
) {
    // Reutilizamos el modelo 'ClaseReal' definido previamente en ClassesTeacher.kt
    var historialClases by remember { mutableStateOf<List<ClaseReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargamos los datos
    // Lanzamos la corrutina cuando el ID del alumno cambia
    LaunchedEffect(alumno.id) {
        try {
            // Filtro AND múltiple en Supabase
            val clasesDescargadas = SupabaseClient.client
                .from("classes")
                .select(columns = Columns.raw("*, alumno:profiles!student_id(*)")) {
                    filter {
                        // Que el profesor sea el que está logueado
                        eq("coach_id", coachId)
                        // Que el alumno sea el de la ficha que estamos mirando
                        eq("student_id", alumno.id)
                    }
                }
                .decodeList<ClaseReal>()

            // Ordenamiento Cronológico
            // sortedByDescending pone las fechas más altas (recientes o futuras) primero.
            historialClases = clasesDescargadas.sortedByDescending { it.startTime }

        } catch (e: Exception) {
            println("Error al cargar historial: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Si el nombre es "Juan Perez Gomez", extraemos solo "Juan" para que no sature la barra superior.
                    Text(
                        text = "Historial: ${alumno.nombreCompleto?.substringBefore(" ") ?: "Alumno"}",
                        maxLines = 1 // Evita que salte de línea si aún así es muy largo
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Contenedor principal con gestión de estados (Cargando / Vacío / Lista)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (historialClases.isEmpty()) {
                Text(
                    text = "Aún no hay clases registradas con este alumno.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historialClases) { clase ->
                        TarjetaClaseHistorial(clase)
                    }
                }
            }
        }
    }
}

/**
 * COMPONENTE DE UI: Tarjeta de Clase Histórica.
 * * DISEÑO: Usa Destructuring Declarations para asignar color y texto de estado.
 */
@Composable
fun TarjetaClaseHistorial(clase: ClaseReal) {
    // Limpieza de fechas
    // Parseo de fecha ISO 8601 a un formato legible
    val fechaLimpia = clase.startTime.substringBefore("T")
    val horaInicioLimpia = clase.startTime.substringAfter("T").take(5)

    // Comprobamos si existe la fecha de fin y la formateamos igual
    val horaFinLimpia = if (clase.endTime != null) {
        clase.endTime.substringAfter("T").take(5)
    } else {
        "??"
    }

    // Lógica de presentación
    // Permite retornar dos valores de un 'when' de forma limpia.
    val (estadoVisual, colorEstado) = when(clase.status) {
        "completed" -> Pair("COMPLETADA", MaterialTheme.colorScheme.secondary)
        "cancelled" -> Pair("CANCELADA", MaterialTheme.colorScheme.error)
        "scheduled" -> Pair("CONFIRMADA", MaterialTheme.colorScheme.primary)
        "pending" -> Pair("PENDIENTE", MaterialTheme.colorScheme.tertiary)
        else -> Pair(clase.status.uppercase(), MaterialTheme.colorScheme.onSurface)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icono de estado
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = colorEstado.copy(alpha = 0.2f),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Fecha",
                    tint = colorEstado,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Detalles de la clase
            Column(modifier = Modifier.weight(1f)) {

                // Fila superior: Fecha a la izquierda, Etiqueta de Estado a la derecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mostramos el formato completo: Fecha • Inicio - Fin
                    Text(
                        text = "$fechaLimpia • $horaInicioLimpia - $horaFinLimpia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = estadoVisual,
                        color = colorEstado,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = clase.direccion ?: "Lugar por definir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Renderizado condicional del número de pista
                if (clase.courtNumber != null) {
                    Text(
                        text = "Pista: ${clase.courtNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}