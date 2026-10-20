package org.example.padeltips.UI.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient

@Serializable
data class TipInsert(
    @SerialName("coach_id") val coachId: String,
    @SerialName("student_id") val studentId: String? = null,
    val content: String,
    val categoria: String?,
    val subcategoria: String?
)

/**
 * Pantalla para añadir un Consejo General (Biblioteca) desde el Profesor.
 * Destaca por el uso de Dropdowns en cascada (Selectores dependientes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGeneralAdviceTeacher(coachId: String, onVolver: () -> Unit, onConsejoGuardado: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // Estados del formulario
    var contenido by remember { mutableStateOf("") }

    // Estados visuales de los menús (abierto/cerrado)
    var expandedCategoria by remember { mutableStateOf(false) }
    var expandedSubcategoria by remember { mutableStateOf(false) }

    // Lógica de selectores en cascada
    // 1. Iniciamos con la primera categoría estática para evitar estados nulos complejos.
    var categoriaSeleccionada by remember { mutableStateOf(listaCategorias[0]) }
    // 2. Iniciamos con la primera subcategoría (si existe). El operador Elvis nos salva si la lista está vacía.
    var subcategoriaSeleccionada by remember { mutableStateOf(categoriaSeleccionada.subcategorias.firstOrNull() ?: "") }

    // Estados de Control
    var isLoading by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarExitoMensaje by remember { mutableStateOf(false) } // Control del Modal de Éxito

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Consejo General") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Selector principal de categoría
            ExposedDropdownMenuBox(
                expanded = expandedCategoria,
                onExpandedChange = { expandedCategoria = !expandedCategoria }
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada.nombre,
                    onValueChange = {},
                    readOnly = true, // Bloquea escritura manual
                    label = { Text("Bloque") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategoria,
                    onDismissRequest = { expandedCategoria = false }
                ) {
                    listaCategorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.nombre) },
                            onClick = {
                                // Almacenamos la nueva selección
                                categoriaSeleccionada = cat

                                // Reset
                                // Al cambiar la categoría padre, reseteamos la subcategoría hija automáticamente
                                // para evitar estados inconsistentes
                                subcategoriaSeleccionada = cat.subcategorias.firstOrNull() ?: ""

                                expandedCategoria = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector secundario de la subcategoría
            // Renderizado Condicional: Solo construimos este componente de UI si la categoría actual tiene subcategorías.
            // Si eliges "Mentalidad" (que no tiene), este menú desaparece de la pantalla al instante.
            if (categoriaSeleccionada.subcategorias.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedSubcategoria,
                    onExpandedChange = { expandedSubcategoria = !expandedSubcategoria }
                ) {
                    OutlinedTextField(
                        value = subcategoriaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Golpe / Situación") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubcategoria) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubcategoria,
                        onDismissRequest = { expandedSubcategoria = false }
                    ) {
                        categoriaSeleccionada.subcategorias.forEach { subcat ->
                            DropdownMenuItem(
                                text = { Text(subcat) },
                                onClick = {
                                    subcategoriaSeleccionada = subcat
                                    expandedSubcategoria = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Campo de texto principal
            OutlinedTextField(
                value = contenido,
                onValueChange = { contenido = it },
                label = { Text("Escribe tu consejo aquí...") },
                modifier = Modifier
                    .fillMaxWidth()
                    // El modificador 'weight(1f)' aquí hace que el campo de texto se expanda verticalmente
                    // ocupando todo el espacio disponible entre los selectores y el botón de guardar.
                    .weight(1f),
                maxLines = 10,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Manejo de Errores Visuales
            mensajeError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botón de guardar
            Button(
                onClick = {
                    // Validación en Frontend
                    if (contenido.isBlank()) {
                        mensajeError = "No puedes guardar un consejo vacío."
                        return@Button
                    }

                    coroutineScope.launch {
                        isLoading = true
                        mensajeError = null
                        try {
                            // Empaquetado del DTO
                            val nuevoTip = TipInsert(
                                coachId = coachId,
                                content = contenido,
                                categoria = categoriaSeleccionada.nombre,
                                // En BBDD, si no hay subcategoría (string vacío), guardamos null en lugar de ""
                                subcategoria = subcategoriaSeleccionada.ifEmpty { null }
                            )

                            // Inserción en Supabase
                            SupabaseClient.client.from("tips").insert(nuevoTip)

                            // Éxito: Desplegamos el modal de feedback
                            mostrarExitoMensaje = true

                        } catch (e: Exception) {
                            mensajeError = "Error al guardar: ${e.message}"
                        } finally {
                            isLoading = false // Liberamos el botón
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("Guardar Consejo", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Mensaje de éxito al profesor
    if (mostrarExitoMensaje) {
        AlertDialog(
            // Bloqueado para obligar a darle a Entendido
            onDismissRequest = { /* No-operación */ },
            title = { Text("¡Consejo Guardado!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("Tu consejo general ha sido añadido a la biblioteca con éxito y ya está disponible para tus alumnos.") },
            confirmButton = {
                Button(onClick = {
                    mostrarExitoMensaje = false
                    onConsejoGuardado() // Callback para navegar hacia atrás
                }) {
                    Text("Entendido")
                }
            }
        )
    }
}
