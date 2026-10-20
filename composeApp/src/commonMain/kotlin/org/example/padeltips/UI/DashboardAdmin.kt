package org.example.padeltips.UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

@Serializable
data class ClaseAdminReal(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("student_id") val studentId: String,
    val status: String
)

/**
 * Panel de Control (Dashboard) del Administrador.
 * * LÓGICA: Usa funciones de colecciones de Kotlin (count, filter, maxByOrNull)
 * para calcular KPIs en tiempo real en memoria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAdmin(irALogin: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // Estado de datos
    var todosLosPerfiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var todasLasClases by remember { mutableStateOf<List<ClaseAdminReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) } // Chivato de errores

    LaunchedEffect(Unit) {
        try {
            // Descargamos absolutamente todos los usuarios
            todosLosPerfiles = SupabaseClient.client.from("profiles").select().decodeList<Profile>()

            // Descargamos todas las clases usando nuestro nuevo modelo
            todasLasClases = SupabaseClient.client.from("classes").select().decodeList<ClaseAdminReal>()

        } catch (e: Exception) {
            mensajeError = e.message
            println("Error de Admin: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Cálculo de estadísticas
    val totalAlumnos = todosLosPerfiles.count { it.rol == "student" }
    val profesores = todosLosPerfiles.filter { it.rol == "coach" }

    // Desglose del estado de las clases (usamos 'status' en vez de 'estado')
    val clasesCompletadas = todasLasClases.count { it.status == "completed" }
    val clasesCanceladas = todasLasClases.count { it.status == "cancelled" }
    val clasesPendientes = todasLasClases.size - clasesCompletadas - clasesCanceladas

    // Calcular quién es el profesor más activo
    val topProfesor = if (todasLasClases.isNotEmpty()) {
        profesores.maxByOrNull { profe -> todasLasClases.count { it.coachId == profe.id } }
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Control (Admin)") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            SupabaseClient.client.auth.signOut()
                            irALogin()
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (mensajeError != null) {
            // Si la base de datos falla, mostramos el error aquí para no volvernos locos
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Error al cargar: $mensajeError", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta de Resumen Global
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            Text("Resumen Global de la Plataforma", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("🎾 Total Alumnos: $totalAlumnos", style = MaterialTheme.typography.bodyLarge)
                            Text("🎓 Total Profesores: ${profesores.size}", style = MaterialTheme.typography.bodyLarge)
                            Text("📅 Total Clases Registradas: ${todasLasClases.size}", style = MaterialTheme.typography.bodyLarge)

                            // --- Desglose de Clases ---
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("✅ Completadas: $clasesCompletadas", style = MaterialTheme.typography.bodySmall)
                                Text("⏳ Pendientes: $clasesPendientes", style = MaterialTheme.typography.bodySmall)
                                Text("❌ Canceladas: $clasesCanceladas", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Text("Rendimiento por Profesor", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                }

                // Lista de Profesores y sus estadísticas
                items(profesores) { profe ->
                    val clasesDelProfe = todasLasClases.filter { it.coachId == profe.id }
                    val alumnosUnicos = clasesDelProfe.map { it.studentId }.distinct().size

                    // ¿Es este el profesor estrella?
                    val esTopProfe = topProfesor != null && profe.id == topProfesor.id && clasesDelProfe.isNotEmpty()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        // Si es el Top Profe, le damos un color ligeramente distinto para destacarlo
                        colors = CardDefaults.cardColors(
                            containerColor = if (esTopProfe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cambiamos el icono genérico de Info por un Avatar redondo
                            Surface(
                                shape = CircleShape,
                                color = if (esTopProfe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(profe.nombreCompleto ?: "Profesor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                    // Insignia de Top Profesor
                                    if (esTopProfe) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Profesor más activo",
                                            tint = Color(0xFFFFC107), // Color Oro/Amarillo
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Clases impartidas: ${clasesDelProfe.size}", style = MaterialTheme.typography.bodyMedium)
                                Text("Alumnos distintos: $alumnosUnicos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}