package org.example.padeltips.UI.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
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
data class TipAlumnoReal(
    val id: String,
    val content: String,
    @SerialName("coach") val coach: Profile? = null
)

/**
 * Pantalla de Tips Personalizados.
 * * ARQUITECTURA: Implementa el patrón clásico de "Gestión de Estados de UI"
 * (Loading, Error, Empty, Success) para dar un buen feedback al usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalTipsStudent(studentId: String, onVolver: () -> Unit) {

    // Estados de la UI
    var misTips by remember { mutableStateOf<List<TipAlumnoReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) } // Permite mostrar errores de red

    // LaunchedEffect(Unit): Al pasarle 'Unit', le decimos a Compose que ejecute
    // esta corrutina solo UNA VEZ cuando la pantalla se crea por primera vez
    LaunchedEffect(Unit) {
        try {
            misTips = SupabaseClient.client
                .from("tips")
                // Trae el tip y el perfil del profe
                .select(columns = Columns.raw("*, coach:profiles!coach_id(*)")) {
                    filter {
                        // Filtro de seguridad: Solo bajamos los tips cuyo student_id coincida con el nuestro
                        eq("student_id", studentId)
                    }
                }
                .decodeList<TipAlumnoReal>()
        } catch (e: Exception) {
            // Capturamos la excepción para no crashear la app y avisar al usuario
            mensajeError = "Error al cargar tus tips: ${e.message}"
            println(mensajeError)
        } finally {
            // El bloque 'finally' se ejecuta siempre, haya ido bien o haya saltado una excepción.
            // Ideal para quitar el spinner de carga.
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Tips Personalizados") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Box es perfecto aquí para poder usar 'Alignment.Center' en los mensajes de carga o error
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Árbol de decisiones de UI
            if (isLoading) {
                // 1. Estado: Cargando
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (mensajeError != null) {
                // 2. Estado: Error de Red / Base de datos
                Text(
                    text = mensajeError!!, // !! es seguro aquí porque sabemos que no es null por el if
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = MaterialTheme.colorScheme.error // Rojo por defecto del tema
                )
            } else if (misTips.isEmpty()) {
                // 3. Estado: Vacío (La llamada fue bien, pero no hay datos)
                Text(
                    text = "Aún no tienes correcciones personalizadas.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 4. Estado: Éxito (Tenemos datos, los pintamos de forma eficiente)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(misTips) { tip ->
                        TarjetaTipAlumno(tip)
                    }
                }
            }
        }
    }
}

/**
 * Componente Stateless para pintar un consejo personalizado.
 */
@Composable
fun TarjetaTipAlumno(tip: TipAlumnoReal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Heredará el color de fondo adaptado a nuestro tema principal
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Tip",
                    tint = MaterialTheme.colorScheme.primary // Estrella del color corporativo
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    // Operador Elvis: Por si el perfil del profe se borró o viene nulo
                    text = "Profe: ${tip.coach?.nombreCompleto ?: "Tu entrenador"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
