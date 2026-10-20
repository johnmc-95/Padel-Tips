package org.example.padeltips.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

@Serializable
data class UpdateProfileName(
    @SerialName("full_name") val nombreCompleto: String
)

/**
 * Pantalla de Configuración de "Mi Cuenta".
 * Permite al usuario actualizar sus datos y cuenta con la opción crítica de Eliminar Cuenta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccount(perfil: Profile?,
              onVolver: () -> Unit,
              onCuentaEliminada: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    val correoActual = SupabaseClient.client.auth.currentUserOrNull()?.email ?: "Correo no disponible"

    // Estados de los campos
    var nombre by remember { mutableStateOf(perfil?.nombreCompleto ?: "") }
    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    // Estados de control y modales
    var isLoading by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarExito by remember { mutableStateOf(false) }

    // Estados para el flujo de borrado de cuenta
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }
    var mostrarExitoBorrado by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Cuenta") },
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

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Correo (No editable)
            OutlinedTextField(
                value = correoActual,
                onValueChange = {},
                readOnly = true,
                label = { Text("Correo Electrónico (No editable)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre completo
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nueva contraseña
            OutlinedTextField(
                value = nuevaPassword,
                onValueChange = { nuevaPassword = it },
                label = { Text("Nueva Contraseña (Opcional)") },
                placeholder = { Text("Déjalo en blanco para no cambiarla") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirmación de contraseña
            if (nuevaPassword.isNotEmpty()) {
                OutlinedTextField(
                    value = confirmarPassword,
                    onValueChange = { confirmarPassword = it },
                    label = { Text("Confirmar Nueva Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmarPassword.isNotEmpty() && nuevaPassword != confirmarPassword
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            mensajeError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de guardar
            Button(
                onClick = {
                    if (nombre.isBlank()) {
                        mensajeError = "El nombre no puede estar vacío."
                        return@Button
                    }
                    if (nuevaPassword.isNotEmpty()) {
                        if (nuevaPassword != confirmarPassword) {
                            mensajeError = "Las contraseñas no coinciden. Por favor, revísalas."
                            return@Button
                        }
                        if (nuevaPassword.length < 6) {
                            mensajeError = "Por seguridad, la contraseña debe tener al menos 6 caracteres."
                            return@Button
                        }
                    }

                    coroutineScope.launch {
                        isLoading = true
                        mensajeError = null
                        try {
                            perfil?.id?.let { miId ->
                                SupabaseClient.client.from("profiles")
                                    .update(UpdateProfileName(nombre)) {
                                        filter { eq("id", miId) }
                                    }
                            }
                            if (nuevaPassword.isNotEmpty()) {
                                SupabaseClient.client.auth.updateUser {
                                    password = nuevaPassword
                                }
                            }
                            mostrarExito = true

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
                else Text("Guardar Cambios", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de eliminar cuenta
            OutlinedButton(
                onClick = { mostrarConfirmacionBorrar = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !isLoading
            ) {
                Text("Eliminar Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Modal de éxito al actualizar perfil
    if (mostrarExito) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¡Datos actualizados!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("Tu perfil se ha guardado correctamente. Los cambios en tu nombre se verán en toda la app la próxima vez que inicies sesión.") },
            confirmButton = {
                Button(onClick = {
                    mostrarExito = false
                    onVolver()
                }) {
                    Text("Genial")
                }
            }
        )
    }

    // Pop up para confirmar la eliminación de la cuenta
    if (mostrarConfirmacionBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            title = { Text("¿Eliminar cuenta definitivamente?", color = MaterialTheme.colorScheme.error) },
            text = { Text("Si eliminas tu cuenta, se borrarán de forma permanente todos tus datos (perfil, historial de clases y mensajes). Esta acción NO se puede deshacer.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                mensajeError = null

                                // Llamamos al script remoto (RPC) para borrar en BD
                                SupabaseClient.client.postgrest.rpc("delete_user_account")

                                // Ocultamos la advertencia y mostramos el ÉXITO (sin salir aún)
                                mostrarConfirmacionBorrar = false
                                mostrarExitoBorrado = true

                            } catch (e: Exception) {
                                mensajeError = "Error al eliminar la cuenta: ${e.message}"
                                mostrarConfirmacionBorrar = false
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text("Sí, eliminar mi cuenta")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionBorrar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Pop up de éxito de eliminación de la cuenta
    if (mostrarExitoBorrado) {
        AlertDialog(
            onDismissRequest = { /* Bloqueado para asegurar lectura */ },
            title = { Text("Cuenta eliminada", color = MaterialTheme.colorScheme.primary) },
            text = { Text("Tu cuenta y todos tus datos han sido eliminados correctamente. ¡Esperamos volver a verte pronto por las pistas!") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        // Destruimos la sesión local y lo mandamos al Login
                        SupabaseClient.client.auth.signOut()
                        mostrarExitoBorrado = false
                        onCuentaEliminada()
                    }
                }) {
                    Text("Entendido")
                }
            }
        )
    }
}