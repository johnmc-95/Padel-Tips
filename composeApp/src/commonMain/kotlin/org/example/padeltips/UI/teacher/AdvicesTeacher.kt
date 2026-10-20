package org.example.padeltips.UI.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

// Modelo estático para estructurar la "Biblioteca de Conocimiento"
data class CategoriaPadel(val nombre: String, val subcategorias: List<String>)

// Árbol de categorías estáticas (Inmutable) actualizado
val listaCategorias = listOf(
    CategoriaPadel("Ataque", listOf("Volea", "Bandeja/Víbora", "Bloqueo", "Saque", "Remate")),
    CategoriaPadel("Defensa", listOf("Derecha", "Revés", "Paredes", "Globos", "Resto", "Chiquita")),
    CategoriaPadel("Posicionamiento en pista", listOf("Ataque", "Defensa", "Transiciones")),
    // Si la categoría no tiene subcategorías, el acordeón mostrará un botón "Ver todos"
    CategoriaPadel("Mentalidad", emptyList())
)

@Serializable
data class TipPersonalizadoReal(
    val id: String,
    val content: String,
    @SerialName("alumno") val alumno: Profile? = null
)

/**
 * Pantalla Principal de Gestión de Consejos para el Profesor.
 * * ARQUITECTURA: Combina un patrón de navegación interna por pestañas (TabRow)
 * con un FAB (Floating Action Button) de comportamiento dinámico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvicesTeacher(
    coachId: String,
    onVolver: () -> Unit,
    onAñadirConsejoGeneral: () -> Unit,
    onAñadirConsejoPersonalizado: () -> Unit,
    onVerConsejos: (String, String) -> Unit
) {
    // Estados de la interfaz
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    val tabs = listOf("Generales", "Personalizados")

    // Estados de datos
    var misTipsPersonalizados by remember { mutableStateOf<List<TipPersonalizadoReal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ejecutamos Lazy Loading para hacer una carga diferida
    // LaunchedEffect se re-ejecuta cada vez que cambia 'tabSeleccionada'.
    // Condicionamos la llamada de red a que el usuario realmente haya abierto la Pestaña 1 (Personalizados)
    // ahorrando ancho de banda y lecturas en base de datos si solo entra a mirar los Generales.
    LaunchedEffect(tabSeleccionada) {
        if (tabSeleccionada == 1) {
            isLoading = true
            try {
                // Consulta SQL en Supabase: Nos traemos todo lo escrito por el profesor.
                val todosMisTips = SupabaseClient.client
                    .from("tips")
                    // 'alumno:profiles!student_id(*)' -> Nombra la relación como 'alumno',
                    // búscalo en la tabla 'profiles' usando nuestra columna 'student_id', y tráete todo (*).
                    .select(columns = Columns.raw("*, alumno:profiles!student_id(*)")) {
                        filter {
                            eq("coach_id", coachId)
                        }
                    }
                    .decodeList<TipPersonalizadoReal>()

                // Filtrado Lógico en Cliente (Kotlin):
                // Un tip solo se considera "Personalizado" si tiene un alumno asociado (no es null).
                misTipsPersonalizados = todosMisTips.filter { it.alumno != null }

            } catch (e: Exception) {
                println("Error al cargar tips personalizados: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consejos y Tips") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } }
            )
        },
        floatingActionButton = {
            // FAB Dinámico: La acción del botón "+" cambia según la pestaña que estemos mirando.
            FloatingActionButton(
                onClick = {
                    if (tabSeleccionada == 0) onAñadirConsejoGeneral()
                    else onAñadirConsejoPersonalizado()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Consejo", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de pestañas (TabRow)
            TabRow(selectedTabIndex = tabSeleccionada) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index },
                        text = { Text(titulo) }
                    )
                }
            }

            // Pestañas
            if (tabSeleccionada == 0) {
                // PESTAÑA 0: Biblioteca General Estática
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Biblioteca de Conocimiento",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(listaCategorias) { categoria ->
                        TarjetaCategoria(categoria = categoria) { subcategoria ->
                            onVerConsejos(categoria.nombre, subcategoria)
                        }
                    }
                    // Margen de seguridad para que el FAB no tape el último elemento de la lista
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                // PESTAÑA 1: Consejos Personalizados
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (misTipsPersonalizados.isEmpty()) {
                        Text(
                            text = "Aún no has enviado correcciones personalizadas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(misTipsPersonalizados) { tip ->
                                TarjetaTipPersonalizado(tip)
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * UI COMPONENT: Tarjeta para representar un consejo personalizado enviado.
 */
@Composable
fun TarjetaTipPersonalizado(tip: TipPersonalizadoReal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "Alumno", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Para: ${tip.alumno?.nombreCompleto ?: "Desconocido"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = tip.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * UI COMPONENT: Acordeón para las categorías estáticas.
 * * NOTA: Componente reutilizable e independiente.
 */
@Composable
fun TarjetaCategoria(categoria: CategoriaPadel, onSubcategoriaClick: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = categoria.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir"
                )
            }

            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (categoria.subcategorias.isEmpty()) {
                        TextButton(onClick = { onSubcategoriaClick(categoria.nombre) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ver todos los consejos", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    } else {
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