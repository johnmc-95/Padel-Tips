package org.example.padeltips.UI.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient

@Serializable
data class TipReal(
    val id: String,
    val content: String,
    val categoria: String? = null,
    val subcategoria: String? = null
)

/**
 * Pantalla que lista los Consejos Generales (Biblioteca) del Profesor.
 * Renderiza dinámicamente el contenido basado en la categoría y subcategoría que recibe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralTipsListTeacher(
    categoria: String,
    subcategoria: String,
    onVolver: () -> Unit
) {
    // Estados de la interfaz
    var listaConsejos by remember { mutableStateOf<List<TipReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Lazy loading
    // LaunchedEffect atado a variables (keys): Si la categoría cambiara sin cerrar
    // la pantalla, este bloque se dispararía de nuevo.
    LaunchedEffect(categoria, subcategoria) {
        try {
            // Consulta directa a Supabase
            listaConsejos = SupabaseClient.client
                .from("tips")
                .select {
                    // WHERE clause dinámico
                    filter {
                        eq("categoria", categoria)
                        // Si una categoría no tiene subcategorías (ej. "Mentalidad"),
                        // en la pantalla anterior le pasamos el mismo nombre como subcategoría.
                        // Aquí lo detectamos para no filtrar por subcategoría si son iguales.
                        if (subcategoria != categoria) {
                            eq("subcategoria", subcategoria)
                        }
                    }
                }
                .decodeList<TipReal>()
        } catch (e: Exception) {
            println("Error al cargar los consejos: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Formateamos el título dependiendo de si hay subcategoría real o no
                title = { Text(if (subcategoria == categoria) categoria else "$categoria: $subcategoria") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Box nos facilita centrar el ProgressIndicator y el mensaje de "Vacío"
        // superponiéndolos al fondo si es necesario
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Árbol de Renderizado Condicional
            if (isLoading) {
                // Estado 1: Petición de red en curso
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (listaConsejos.isEmpty()) {
                // Estado 2: Petición OK, pero sin resultados
                Text(
                    text = "Aún no has escrito ningún consejo aquí.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Estado 3: Lista poblada
                // LazyColumn solo renderiza las tarjetas visibles en pantalla, ahorrando RAM
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(listaConsejos) { tip ->
                        TarjetaConsejo(tip)
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * COMPONENTE DE UI: Tarjeta de Consejo
 * * DISEÑO: Usamos surfaceVariant para que herede automáticamente un tono suave
 * derivado del Azul Corporativo principal (gracias al MaterialTheme centralizado).
 */
@Composable
fun TarjetaConsejo(tip: TipReal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}