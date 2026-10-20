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
import org.example.padeltips.model.Profile

@Serializable
data class TipInsertPersonal(
    @SerialName("coach_id") val coachId: String,
    @SerialName("student_id") val studentId: String,
    val content: String,
    val type: String
)

/**
 * Pantalla para añadir un Tip Privado dirigido a un solo alumno.
 * * ARQUITECTURA: Pantalla Reutilizable. Al aceptar 'alumnoInicial' con valor por defecto null,
 * podemos llamarla desde el menú general (se abrirá vacía) o desde la ficha de un alumno (se abrirá pre-seleccionado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalAdviceTeacher(
    coachId: String,
    alumnoInicial: Profile? = null,
    onVolver: () -> Unit,
    onConsejoGuardado: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Estado del formulario
    var contenido by remember { mutableStateOf("") }

    // Lista de alumnos descargada de la base de datos
    var alumnos by remember { mutableStateOf<List<Profile>>(emptyList()) }

    // Inicializamos el estado con el alumno que nos pasan por parámetro (si lo hay)
    var alumnoSeleccionado by remember { mutableStateOf<Profile?>(alumnoInicial) }
    var expandedMenu by remember { mutableStateOf(false) }

    // Estados de control
    var isLoading by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarExitoMensaje by remember { mutableStateOf(false) }

    // Descargamos la lista completa de alumnos para rellenar el Dropdown.
    // Aunque ya tengamos un 'alumnoInicial', necesitamos descargar la lista por si el profe
    // se equivoca y quiere cambiar el destinatario sobre la marcha.
    LaunchedEffect(Unit) {
        try {
            alumnos = SupabaseClient.client
                .from("profiles")
                .select { filter { eq("role", "student") } }
                .decodeList<Profile>()
        } catch (e: Exception) {
            println("Error al cargar alumnos: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Tip Privado") },
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

            // Desplegable del alumno
            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    // Mostramos el nombre del alumno seleccionado o un texto orientativo
                    value = alumnoSeleccionado?.nombreCompleto ?: "Selecciona un alumno",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Alumno Destinatario") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    alumnos.forEach { alumno ->
                        DropdownMenuItem(
                            text = { Text(alumno.nombreCompleto ?: "Sin nombre") },
                            onClick = {
                                alumnoSeleccionado = alumno
                                expandedMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido del mensaje
            OutlinedTextField(
                value = contenido,
                onValueChange = { contenido = it },
                label = { Text("Escribe tu corrección técnica aquí...") },
                modifier = Modifier.fillMaxWidth().weight(1f), // Expande para ocupar la pantalla restante
                maxLines = 10,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feedback visual de error
            mensajeError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Acción de enviar
            Button(
                onClick = {
                    // Validación básica en local
                    if (alumnoSeleccionado == null || contenido.isBlank()) {
                        mensajeError = "Por favor, selecciona un alumno y escribe un consejo."
                        return@Button
                    }

                    coroutineScope.launch {
                        isLoading = true
                        mensajeError = null
                        try {
                            // Construcción del Payload
                            val nuevoTip = TipInsertPersonal(
                                coachId = coachId,
                                studentId = alumnoSeleccionado!!.id, // !! es seguro por la validación anterior
                                content = contenido,
                                // OBLIGATORIO: Forzamos la etiqueta para que la BBDD sepa que es privado
                                type = "personal"
                            )

                            // Petición de escritura en Supabase
                            SupabaseClient.client.from("tips").insert(nuevoTip)

                            // Detonamos el modal de éxito
                            mostrarExitoMensaje = true

                        } catch (e: Exception) {
                            mensajeError = "Error al guardar: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("Enviar al Alumno", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Modal de éxito
    if (mostrarExitoMensaje) {
        AlertDialog(
            onDismissRequest = { /* Bloqueado para asegurar la lectura */ },
            title = { Text("¡Consejo Enviado!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("El consejo técnico se ha enviado correctamente a la bandeja del alumno.") },
            confirmButton = {
                Button(onClick = {
                    mostrarExitoMensaje = false
                    onConsejoGuardado() // Callback para retroceder en la navegación
                }) {
                    Text("Entendido")
                }
            }
        )
    }
}