package org.example.padeltips.UI.teacher

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
import padeltips.composeapp.generated.resources.version_profesor

// Definimos los colores corporativos.
val AzulCorporativo = Color(0xFF1E3A8A)

/**
 * Pantalla Principal del Profesor (Dashboard).
 * * ARQUITECTURA VISUAL: Rediseñada con el patrón "Modern Bottom Card" para
 * mantener la coherencia gráfica con la pantalla de Login.
 */
@Composable
fun MainTeacher(
    perfil: Profile?, // Recibimos la sesión actual inyectada
    irALogin: () -> Unit,
    irAClases: () -> Unit,
    irAAlumnos: () -> Unit,
    irAConsejos: () -> Unit,
    irABandeja: () -> Unit,
    irAMiCuenta: () -> Unit
) {
    // Scope ligado al ciclo de vida de este Composable para lanzar peticiones de red
    val coroutineScope = rememberCoroutineScope()

    // Contenedor principal
    // Usamos Box para superponer la tarjeta encima de la imagen de fondo
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
                    .weight(0.35f)
                    .background(AzulCorporativo)
            ) {
                Image(
                    painter = painterResource(Res.drawable.version_profesor),
                    contentDescription = "Ilustración de un profesor enseñando a jugar al pádel",
                    contentScale = ContentScale.Crop, // Vital para que no deforme
                    modifier = Modifier.fillMaxSize()
                )

                // Pequeño filtro oscuro para que contraste mejor con el borde de la tarjeta
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))

                // Etiqueta indicativa de rol flotando en la esquina inferior derecha
                // La subimos un poco (padding bottom) para que la curva de la tarjeta no la tape
                Surface(
                    color = AzulCorporativo.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 48.dp)
                ) {
                    Text(
                        text = "Versión Prof.",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Espacio hueco que ocupará la tarjeta
            Spacer(modifier = Modifier.weight(0.65f))
        }

        // Tarjeta de navegación
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.BottomCenter),
            // Bordes superiores muy redondeados
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Le da un toque 3D extra
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Saludo al usuario
                Text(
                    text = "Hola, ${perfil?.nombreCompleto ?: "Profesor"} 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AzulCorporativo,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Reutilización del color de los botones
                val coloresBotonNormal = ButtonDefaults.buttonColors(containerColor = AzulCorporativo)

                // Menú de navegación
                Button(onClick = irAClases, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Clases", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irAAlumnos, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Alumnos", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irAConsejos, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Consejos", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irABandeja, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Bandeja de Mensajes", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = irAMiCuenta, modifier = Modifier.fillMaxWidth().height(56.dp), colors = coloresBotonNormal) {
                    Text("Mi cuenta", style = MaterialTheme.typography.titleMedium)
                }

                // El Spacer con weight(1f) es un "muelle" que empuja el contenido hacia arriba
                // y el botón de Cerrar Sesión hacia abajo, rellenando el hueco que quede
                Spacer(modifier = Modifier.weight(1f))

                // Botón de cerrar sesión
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            SupabaseClient.client.auth.signOut()
                            irALogin()
                        }
                    }
                ) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }

                // Muelle final por si las pantallas sin marcos (bezelless) se comen el botón
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}