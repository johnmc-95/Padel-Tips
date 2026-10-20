package org.example.padeltips.UI.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

/**
 * Pantalla de Directorio/Lista de Alumnos (Vista del Profesor).
 * * ARQUITECTURA: Implementa la parte "Maestro" del patrón de diseño "Master-Detail".
 * Su única responsabilidad es listar y delegar la navegación inyectando el objeto seleccionado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsListTeacher(
    coachId: String, // Se recibe por si en un futuro queremos filtrar solo los alumnos de este profe
    onVolver: () -> Unit,
    // Lambda que extrae el objeto completo hacia el (App.kt)
    onAlumnoSeleccionado: (Profile) -> Unit
) {
    // Estados
    var alumnos by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Carga inicial de datos
    // LaunchedEffect(Unit) garantiza que la petición de red se lance solo una vez
    // cuando la pantalla entra en la composición
    LaunchedEffect(Unit) {
        try {
            // Descargamos la lista filtrando directamente en la query SQL para minimizar
            // el payload (la cantidad de datos que viaja por la red).
            alumnos = SupabaseClient.client
                .from("profiles")
                .select { filter { eq("role", "student") } }
                .decodeList<Profile>()
        } catch (e: Exception) {
            println("Error al cargar alumnos: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Alumnos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { paddingValues ->
        // Contenedor principal
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Renderizado condicional
            if (isLoading) {
                // Feedback visual de carga
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (alumnos.isEmpty()) {
                // Empty State (Manejo de colecciones vacías)
                Text(
                    text = "No hay alumnos registrados aún.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // LazyColumn: Rendimiento óptimo. En lugar de instanciar todas las tarjetas en memoria
                // , recicla las vistas según hacemos scroll
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                    // Función 'items' de Compose para iterar sobre la lista
                    items(alumnos) { alumno ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                // Event Bubbling: Capturamos el click y subimos el objeto 'alumno' completo.
                                // Esto nos ahorra hacer un "SELECT * FROM profiles WHERE id=..." en la pantalla Detalle.
                                .clickable { onAlumnoSeleccionado(alumno) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar genérico
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null, // null porque es decorativo
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Nombre del alumno
                                Text(
                                    text = alumno.nombreCompleto ?: "Sin nombre",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}