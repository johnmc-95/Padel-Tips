package org.example.padeltips.UI.student

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile
import org.jetbrains.compose.resources.painterResource
import padeltips.composeapp.generated.resources.Res
import padeltips.composeapp.generated.resources.version_alumno

// Definimos el color corporativo
val AzulCorporativo = Color(0xFF1E3A8A)

/**
 * Pantalla Principal del Alumno
 * * NOTA ARQUITECTURA: Usamos "State Hoisting" (Elevación de estado).
 * * ARQUITECTURA VISUAL: Utiliza el patrón "Modern Bottom Card" para mantener
 * una coherencia gráfica total con el Dashboard del Profesor y el Login.
 */
@Composable
fun MainStudent(
    perfil: Profile?, // El perfil del usuario logueado (puede ser null mientras carga)
    irALogin: () -> Unit,
    irAMisTips: () -> Unit,
    irAClases: () -> Unit,
    irAConsejos: () -> Unit,
    irAMiCuenta: () -> Unit
) {
    // Necesitamos este scope para lanzar tareas asíncronas sin congelar la interfaz de usuario
    val coroutineScope = rememberCoroutineScope()

    // Contenedor principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        // Capa inferior
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f) // Ocupa exactamente el 35% superior
                    .background(AzulCorporativo)
            ) {
                Image(
                    painter = painterResource(Res.drawable.version_alumno),
                    contentDescription = "Ilustración de un alumno jugando al pádel",
                    // ContentScale.Crop es clave aquí, rellena todo el espacio cortando lo que sobra sin deformar la imagen
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Filtro oscuro suave para integrar mejor el diseño
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))

                // Etiqueta flotante. Ajustamos el bottom padding para que la tarjeta no la tape.
                Surface(
                    color = AzulCorporativo.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 48.dp)
                ) {
                    Text(
                        text = "Versión Alumno",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Hueco reservado para la tarjeta inferior
            Spacer(modifier = Modifier.weight(0.65f))
        }

        // Capa superior
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) // Límite del 70% para garantizar que la imagen se vea siempre
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            // Aquí sí metemos padding amplio para que los botones y textos respiren
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Hola, ${perfil?.nombreCompleto ?: "Alumno"} 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AzulCorporativo,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Reutilizamos esta variable para los botones para no repetir código y optimizar
                val coloresBotonNormal = ButtonDefaults.buttonColors(containerColor = AzulCorporativo)

                // Botones con separación
                Button(onClick = irAMisTips, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Mis Tips", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irAClases, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Clases", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irAConsejos, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Consejos", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irAMiCuenta, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Mi cuenta", style = MaterialTheme.typography.titleMedium)
                }

                // El modificador weight(1f) actúa como un muelle: ocupa todo el espacio libre
                // y empuja el botón de Cerrar Sesión hacia abajo del todo.
                Spacer(modifier = Modifier.weight(1f))

                // Botón de cerrar sesión
                TextButton(
                    onClick = {
                        // Lanzamos la corrutina para hacer la petición de red a Supabase
                        coroutineScope.launch {
                            SupabaseClient.client.auth.signOut()
                            irALogin()
                        }
                    }
                ) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }

                // Muelle extra por si el móvil no tiene apenas marcos inferiores
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}