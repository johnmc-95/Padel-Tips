package org.example.padeltips.UI.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

/**
 * Pantalla que muestra todos los alumnos registrados (Vista del Profesor).
 * * ARQUITECTURA: Pantalla "Maestro" (Master view). Su única misión es consultar
 * una lista a la base de datos, pintarla de forma eficiente y avisar cuando
 * el usuario hace clic en un elemento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsTeacher(
    onVolver: () -> Unit,
    // State Hoisting: Subimos el evento de clic al padre (App.kt) pasándole el objeto completo.
    onAlumnoClick: (Profile) -> Unit
) {

    // Gestión de estados
    // Usamos 'remember' para que Compose no borre la lista cada vez que redibuja la pantalla
    var listaAlumnos by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // LaunchedEffect(Unit) lanza una corrutina automáticamente al entrar en esta pantalla.
    // Al pasarle 'Unit', le garantizamos a Compose que solo queremos que se ejecute UNA vez.
    LaunchedEffect(Unit) {
        try {
            // Llamada asíncrona a la API de Supabase
            listaAlumnos = SupabaseClient.client
                .from("profiles") // Tabla de perfiles
                .select {
                    // Lógica de negocio: Filtramos directamente en el servidor (SQL WHERE)
                    // para no descargar los perfiles de otros profesores.
                    filter { eq("role", "student") }
                }
                // Decodificamos el JSON que nos devuelve Supabase directamente a nuestra data class 'Profile'
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
                title = { Text("Mis Alumnos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Árbol de decisiones
            if (isLoading) {
                // Estado 1: Loading (Feedback de carga)
                CircularProgressIndicator()
            } else if (listaAlumnos.isEmpty()) {
                // Estado 2: Empty State (Previene que el usuario crea que la app está rota)
                Text("Aún no tienes alumnos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Estado 3: Lista de Datos
                // LazyColumn = Rendimiento óptimo (Solo renderiza lo que cabe en la pantalla)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Añade margen automático entre las tarjetas
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Bucle LIMPIO, iterando directamente sobre la data class Profile
                    items(listaAlumnos) { perfilAlumno ->
                        TarjetaAlumnoReal(alumno = perfilAlumno) {
                            // Cuando se pulsa la tarjeta, disparamos el callback
                            onAlumnoClick(perfilAlumno)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * COMPONENTE DE UI: Tarjeta de Alumno.
 * Componente tonto (Stateless), solo pinta lo que le pasan y avisa cuando lo tocan.
 */
@Composable
fun TarjetaAlumnoReal(alumno: Profile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Modificador clickable: Convierte cualquier elemento en un botón
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape, // Lo hace redondo automáticamente
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            // Usamos weight(1f) para que los textos ocupen todo el ancho sobrante y no empujen al icono
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alumno.nombreCompleto ?: "Sin nombre",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Toca para ver la ficha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}