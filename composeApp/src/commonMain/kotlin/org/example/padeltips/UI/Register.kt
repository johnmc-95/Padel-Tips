package org.example.padeltips.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.padeltips.data.SupabaseClient

/**
 * Pantalla de Registro de Usuarios.
 * * La arquitectura es gestionar la creación de credenciales en Auth y la inserción
 * inicial de metadatos (que un Trigger de la BBDD pasará a la tabla 'profiles').
 * * INCLUYE: Validación de datos en el cliente (Frontend) mediante Regex.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(irALogin: () -> Unit) {
    // Estados del formulario
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Si es profesor se mantiene igual
    var isCoach by remember { mutableStateOf(false) }

    // Estados para el menú desplegable del nivel del alumno
    var expandedNivel by remember { mutableStateOf(false) }
    val opcionesNivel = listOf("Iniciación", "Intermedio", "Avanzado")
    var nivelSeleccionado by remember { mutableStateOf(opcionesNivel[0]) }

    // Respuestas si son un error o un éxito
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var mostrarExito by remember { mutableStateOf(false) }


    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Campos de texto
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre y Apellidos") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Selección de rol por parte del usuario
        Text("¿Cómo vas a usar la app?", style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = !isCoach, onClick = { isCoach = false })
            Text("Soy Alumno")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = isCoach, onClick = { isCoach = true })
            Text("Soy Profesor")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selección de nivel
        // Solo instanciamos este árbol de UI si el usuario selecciona que es Alumno
        if (!isCoach) {
            ExposedDropdownMenuBox(
                expanded = expandedNivel,
                onExpandedChange = { expandedNivel = !expandedNivel }
            ) {
                OutlinedTextField(
                    value = nivelSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tu nivel de pádel") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNivel) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedNivel,
                    onDismissRequest = { expandedNivel = false }
                ) {
                    opcionesNivel.forEach { seleccion ->
                        DropdownMenuItem(
                            text = { Text(seleccion) },
                            onClick = {
                                nivelSeleccionado = seleccion
                                expandedNivel = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Manejo de errores
        mensajeError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Botón de registro
        Button(
            onClick = {
                // Validaciones

                // Comprobar que no haya campos vacíos
                if (nombre.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    mensajeError = "Por favor, rellena todos los campos obligatorios."
                    return@Button
                }

                // Validar el formato del correo electrónico usando una Expresión Regular (Regex)
                val emailRegex = "^[A-Za-z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-z]{2,}\$".toRegex()
                if (!email.matches(emailRegex)) {
                    mensajeError = "Por favor, introduce un correo electrónico válido."
                    return@Button
                }

                // Validaciones de contraseñas
                if (password != confirmPassword) {
                    mensajeError = "Las contraseñas no coinciden."
                    return@Button
                }
                if (password.length < 6) {
                    mensajeError = "La contraseña debe tener al menos 6 caracteres."
                    return@Button
                }


                coroutineScope.launch {
                    isLoading = true
                    mensajeError = null

                    try {
                        // Petición a Supabase Auth
                        SupabaseClient.client.auth.signUpWith(Email) {
                            this.email = email
                            this.password = password

                            // Inyección de Metadatos:
                            // Estos datos viajan en formato JSON a Supabase y son capturados
                            // por un Trigger de SQL para rellenar la tabla 'profiles' automáticamente.
                            data = buildJsonObject {
                                put("full_name", nombre)
                                put("role", if (isCoach) "coach" else "student")
                                put("email", email) // Guardamos el email también en la tabla pública
                                if (!isCoach) {
                                    put("level", nivelSeleccionado)
                                }
                            }
                        }

                        // Disparamos el pop-up
                        mostrarExito = true

                    } catch (e: Exception) {
                        // Si falla algo a nivel de red o servidor que no hemos pillado en el frontend
                        mensajeError = "No se ha podido crear la cuenta. Comprueba tu conexión o si el correo ya existe."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Registrarse")
            }
        }

        TextButton(onClick = { irALogin() }, enabled = !isLoading) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }

    // Pop up de confirmación
    if (mostrarExito) {
        AlertDialog(
            onDismissRequest = { /* Bloqueado para forzar clic */ },
            title = { Text("¡Registro completado!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("Tu cuenta ha sido creada con éxito. Ya puedes iniciar sesión en la aplicación.") },
            confirmButton = {
                Button(onClick = {
                    mostrarExito = false
                    irALogin() // Ahora sí, viajamos a la pantalla de Login
                }) {
                    Text("Ir al Login")
                }
            }
        )
    }
}