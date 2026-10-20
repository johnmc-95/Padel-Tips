package org.example.padeltips.UI.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


data class CategoriaPadelStudent(val nombre: String, val subcategorias: List<String>)

// Categorías y subcategorías
val listaCategoriasStudent = listOf(
    CategoriaPadelStudent("Ataque", listOf("Volea", "Bandeja/Víbora", "Bloqueo", "Saque", "Remate")),
    CategoriaPadelStudent("Defensa", listOf("Derecha", "Revés", "Paredes", "Globos", "Resto", "Chiquita")),
    CategoriaPadelStudent("Posicionamiento en pista", listOf("Ataque", "Defensa", "Transiciones")),
    CategoriaPadelStudent("Mentalidad", emptyList())
)

/**
 * Pantalla de Biblioteca de Consejos Generales.
 * Muestra una lista de categorías expandibles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralAdvicesStudent(
    onVolver: () -> Unit,
    // Pasamos dos Strings: La categoría padre y la subcategoría seleccionada
    onVerConsejos: (String, String) -> Unit
) {
    // Scaffold: El esqueleto estándar de Material Design
    // Nos ahorra tener que calcular dónde va la barra superior y dónde empieza el contenido
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca de Pádel") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        // LazyColumn: El equivalente moderno al RecyclerView.
        // Fundamental para el rendimiento: solo renderiza en memoria los elementos que caben en la pantalla.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Añade margen automático entre elementos
        ) {

            // "item" individual para la cabecera fija
            item {
                Text(
                    text = "Aprende la teoría y mejora tu técnica",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // "items" repite el componente por cada elemento de la lista
            items(listaCategoriasStudent) { categoria ->
                // Propagación de eventos (Event Bubbling):
                // La tarjeta hija nos avisa de qué subcategoría se clicó, y nosotros lo subimos a App.kt
                TarjetaCategoriaStudent(categoria = categoria) { subcategoria ->
                    onVerConsejos(categoria.nombre, subcategoria)
                }
            }
        }
    }
}

// Tarjeta expandible
@Composable
fun TarjetaCategoriaStudent(
    categoria: CategoriaPadelStudent,
    onSubcategoriaClick: (String) -> Unit // Lambda para devolver la interacción al padre
) {
    // Estado local: Guarda si el acordeón está abierto o cerrado
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Al hacer clic en toda la tarjeta, invertimos el booleano
            .clickable { expandido = !expandido },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Cabecera de la tarjeta
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Separa texto a la izquierda e icono a la derecha
            ) {
                Text(text = categoria.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                // Cambiamos el icono dinámicamente según el estado
                Icon(
                    imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir"
                )
            }

            // Contenido desplegable
            // AnimatedVisibility: Envuelve el contenido y Compose calcula las transiciones de altura automáticamente
            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                    // Separador visual suave
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Lógica de renderizado condicional
                    if (categoria.subcategorias.isEmpty()) {
                        // Si no hay subcategorías (ej. "Mentalidad"), mostramos un botón directo
                        TextButton(onClick = { onSubcategoriaClick(categoria.nombre) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ver todos los consejos", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    } else {
                        // Si hay subcategorías (ej. "Volea", "Remate"), creamos un botón por cada una
                        categoria.subcategorias.forEach { subcategoria ->
                            TextButton(onClick = { onSubcategoriaClick(subcategoria) }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "• $subcategoria",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}