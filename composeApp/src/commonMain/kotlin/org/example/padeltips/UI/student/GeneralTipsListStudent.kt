package org.example.padeltips.UI.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
data class TipGeneralReal(
    val id: String,
    val content: String,
    val categoria: String? = null,
    val subcategoria: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("coach") val coach: Profile? = null
)

/**
 * Pantalla que lista los consejos generales de una categoría concreta.
 * No gestiona su propia navegación, delegando la vuelta atrás al lambda onVolver.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralTipsListStudent(categoria: String, subcategoria: String, onVolver: () -> Unit) {
    // Variables de Estado
    var listaConsejos by remember { mutableStateOf<List<TipGeneralReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // NUEVO: Estado para cazar chivatazos de error de Supabase
    var mensajeError by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect atado a variables:
    LaunchedEffect(categoria, subcategoria) {
        try {
            isLoading = true
            mensajeError = null

            val tipsDescargados = SupabaseClient.client
                .from("tips")
                .select(columns = Columns.raw("*, coach:profiles!coach_id(*)")) {
                    filter {
                        eq("categoria", categoria)
                        if (subcategoria != categoria) {
                            eq("subcategoria", subcategoria)
                        }
                    }
                }
                .decodeList<TipGeneralReal>()

            // Filtramos en local los que no tienen ID de alumno asignado
            listaConsejos = tipsDescargados.filter { it.studentId == null }

        } catch (e: Exception) {
            // Si falla, lo mostramos en pantalla
            mensajeError = "Error de conexión o permisos: ${e.message}"
            println(mensajeError)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (subcategoria == categoria) categoria else "$categoria: $subcategoria") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Gestión de los 4 estados: Cargando, Error, Vacío y Con Datos.
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (mensajeError != null) {
                // Estado de error
                Text(
                    text = mensajeError!!,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (listaConsejos.isEmpty()) {
                Text(
                    text = "Aún no hay consejos en esta categoría.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(listaConsejos) { tip ->
                        TarjetaConsejoGeneralAlumno(tip)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// Componente Stateless para pintar cada consejo
@Composable
fun TarjetaConsejoGeneralAlumno(tip: TipGeneralReal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                // Operador Elvis para cubrir el caso de que el perfil del profesor haya sido borrado de la BBDD
                text = "Profe: ${tip.coach?.nombreCompleto ?: "Entrenador"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}