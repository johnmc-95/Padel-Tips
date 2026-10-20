package org.example.padeltips.UI

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile
import org.jetbrains.compose.resources.painterResource
import padeltips.composeapp.generated.resources.*

// Reutilizamos el azul corporativo para nuestra pantalla de Login
val AzulCorporativoLogin = Color(0xFF1E3A8A)

/**
 * Pantalla de Login de PadelTips.
 * * ARQUITECTURA VISUAL: Utiliza un diseño de "Bottom Card" (Tarjeta inferior)
 * superpuesta sobre una imagen de cabecera a sangre para un look más moderno.
 */
@Composable
fun Login(irARegistro: () -> Unit, onLoginExito: (Profile) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Usamos Box en lugar de Column para poder usar capas.
    // Así la tarjeta blanca se puede "subir" encima de la imagen de fondo.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        // Fondo e imagen de cabcera
        Column(modifier = Modifier.fillMaxSize()) {
            // La imagen ocupa un 40% de la pantalla superior
            Box(modifier = Modifier.fillMaxWidth().weight(0.4f)) {
                // La imagen de la pista tocando de lado a lado
                Image(
                    painter = painterResource(Res.drawable.pista_padel),
                    contentDescription = "Ilustración de PadelTips",
                    contentScale = ContentScale.Crop, // Crop es vital para que no queden márgenes blancos
                    modifier = Modifier.fillMaxSize()
                )
                // Filtro semitransparente elegante encima de la foto
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            }
            // El 60% restante lo dejamos vacío porque lo tapará la tarjeta
            Spacer(modifier = Modifier.weight(0.6f))
        }

        // Tarjeta del formulario
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter), // La anclamos abajo del todo
            // Aquí está la magia: redondeamos mucho las esquinas de arriba y nada las de abajo
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            // Usamos tu Column original pero ahora vive dentro de la tarjeta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Hemos movido tu título original aquí para que corone la tarjeta
                Text(
                    text = "🎾 PadelTips",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = AzulCorporativoLogin
                )

                Spacer(modifier = Modifier.height(60.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }, // Icono añadido
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                mensajeError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                mensajeExito?.let {
                    Text(text = it, color = ColorExito(), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Creamos el botón de entrada
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            mensajeError = "Por favor, rellena todos los campos"
                            return@Button
                        }

                        coroutineScope.launch {
                            isLoading = true
                            mensajeError = null
                            mensajeExito = null

                            val emailLogin = email
                            val passwordLogin = password

                            try {
                                SupabaseClient.client.auth.signInWith(Email) {
                                    this.email = emailLogin
                                    this.password = passwordLogin
                                }

                                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                                if (usuarioActual != null) {
                                    val miPerfil = org.example.padeltips.repository.UserRepository.obtenerPerfilUsuario(usuarioActual.id)

                                    if (miPerfil != null) {
                                        mensajeExito = "¡Sesión iniciada con éxito!"
                                        onLoginExito(miPerfil)
                                    } else {
                                        mensajeError = "Error: No se encontró tu perfil."
                                    }
                                }

                            } catch (e: Exception) {
                                println("🚨 ERROR LOGIN: ${e.message}")
                                e.printStackTrace()
                                mensajeError = "Credenciales incorrectas o error de red."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulCorporativoLogin),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Entrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = { irARegistro() }
                ) {
                    Text("¿No tienes cuenta? Regístrate aquí", color = AzulCorporativoLogin)
                }

                // Un pequeño muelle al final por si el teclado empuja la pantalla en móviles pequeños
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ColorExito() = Color(0xFF4CAF50)