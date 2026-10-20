package org.example.padeltips.UI.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

@Serializable
data class ClaseInsert(
    @SerialName("coach_id") val coachId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("address") val address: String,
    @SerialName("court_number") val courtNumber: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassTeacher(coachId: String, onVolver: () -> Unit, onClaseGuardada: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    var fecha by remember { mutableStateOf("") }
    var horaInicio by remember { mutableStateOf("") }
    var horaFin by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var pista by remember { mutableStateOf("") }

    var mostrarDatePicker by remember { mutableStateOf(false) }
    // Bloquear fechas anteriores
    // Obtenemos el tiempo actual usando nuestra nueva función multiplataforma
    val currentMillis = remember { org.example.padeltips.UI.getCurrentTimeMillis() }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Restamos un día al tiempo actual para asegurar que el día de HOY sea seleccionable
                // pero todo lo anterior a hoy queda bloqueado.
                return utcTimeMillis >= (currentMillis - 86400000L)
            }
        }
    )

    var mostrarTimePickerInicio by remember { mutableStateOf(false) }
    val timePickerStateInicio = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)

    var mostrarTimePickerFin by remember { mutableStateOf(false) }
    val timePickerStateFin = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = true)

    var alumnos by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var alumnoSeleccionado by remember { mutableStateOf<Profile?>(null) }
    var expandedMenu by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarExitoMensaje by remember { mutableStateOf(false) }

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
                title = { Text("Nueva Clase") },
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

            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    value = alumnoSeleccionado?.nombreCompleto ?: "Selecciona un alumno",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Alumno") },
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

            OutlinedTextField(
                value = fecha,
                onValueChange = { },
                readOnly = true,
                label = { Text("Fecha de la clase") },
                trailingIcon = {
                    IconButton(onClick = { mostrarDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = horaInicio,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Inicio") },
                    modifier = Modifier.weight(1f).clickable { mostrarTimePickerInicio = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = horaFin,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Fin") },
                    modifier = Modifier.weight(1f).clickable { mostrarTimePickerFin = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección / Club") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pista,
                onValueChange = { pista = it },
                label = { Text("Nº de Pista") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            mensajeError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (alumnoSeleccionado == null || fecha.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty() || direccion.isEmpty()) {
                        mensajeError = "Por favor, rellena los campos principales."
                        return@Button
                    }

                    // Validación de horas
                    val partesInicio = horaInicio.split(":")
                    val partesFin = horaFin.split(":")

                    if (partesInicio.size == 2 && partesFin.size == 2) {
                        // Convertimos las horas completas a minutos totales desde medianoche
                        val minutosInicioTotal = (partesInicio[0].toInt() * 60) + partesInicio[1].toInt()
                        val minutosFinTotal = (partesFin[0].toInt() * 60) + partesFin[1].toInt()

                        val diferencia = minutosFinTotal - minutosInicioTotal

                        if (diferencia <= 0) {
                            mensajeError = "❌ La hora de fin debe ser posterior a la hora de inicio."
                            return@Button
                        }

                        if (diferencia > 120) { // 120 minutos = 2 horas
                            mensajeError = "❌ La duración de la clase no puede ser mayor a 2 horas."
                            return@Button
                        }
                    }

                    coroutineScope.launch {
                        isLoading = true
                        mensajeError = null
                        try {
                            val startTimestamp = "${fecha}T${horaInicio}:00Z"
                            val endTimestamp = "${fecha}T${horaFin}:00Z"
                            val courtNum = pista.toIntOrNull()

                            val nuevaClase = ClaseInsert(
                                coachId = coachId,
                                studentId = alumnoSeleccionado!!.id,
                                startTime = startTimestamp,
                                endTime = endTimestamp,
                                address = direccion,
                                courtNumber = courtNum
                            )

                            SupabaseClient.client.from("classes").insert(nuevaClase)
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
                else Text("Guardar Clase", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Calendario
    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        fecha = millisToDateString(millis)
                    }
                    mostrarDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Reloj Hora Inicio
    if (mostrarTimePickerInicio) {
        AlertDialog(
            onDismissRequest = { mostrarTimePickerInicio = false },
            title = { Text("Hora de Inicio") },
            text = { TimePicker(state = timePickerStateInicio) },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerStateInicio.hour
                    val m = timePickerStateInicio.minute

                    // --- RESTRICCIÓN DE HORARIO ---
                    if (h < 9 || h > 22 || (h == 22 && m > 0)) {
                        mensajeError = "❌ Las clases solo pueden ser entre las 09:00 y las 22:00."
                        horaInicio = "" // Vaciamos por si acaso
                    } else {
                        mensajeError = null // Borramos errores previos
                        val hStr = h.toString().padStart(2, '0')
                        val mStr = m.toString().padStart(2, '0')
                        horaInicio = "$hStr:$mStr"
                    }
                    mostrarTimePickerInicio = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarTimePickerInicio = false }) { Text("Cancelar") }
            }
        )
    }

    // Reloj Hora Fin
    if (mostrarTimePickerFin) {
        AlertDialog(
            onDismissRequest = { mostrarTimePickerFin = false },
            title = { Text("Hora de Fin") },
            text = { TimePicker(state = timePickerStateFin) },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerStateFin.hour
                    val m = timePickerStateFin.minute

                    // --- RESTRICCIÓN DE HORARIO ---
                    if (h < 9 || h > 22 || (h == 22 && m > 0)) {
                        mensajeError = "❌ Las clases solo pueden ser entre las 09:00 y las 22:00."
                        horaFin = ""
                    } else {
                        mensajeError = null
                        val hStr = h.toString().padStart(2, '0')
                        val mStr = m.toString().padStart(2, '0')
                        horaFin = "$hStr:$mStr"
                    }
                    mostrarTimePickerFin = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarTimePickerFin = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarExitoMensaje) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¡Clase Guardada!", color = MaterialTheme.colorScheme.primary) },
            text = { Text("La clase se ha añadido correctamente a tu agenda y el alumno ya puede verla en su aplicación.") },
            confirmButton = {
                Button(onClick = {
                    mostrarExitoMensaje = false
                    onClaseGuardada()
                }) {
                    Text("Entendido")
                }
            }
        )
    }
}

// Helper Multiplataforma para Fechas
fun millisToDateString(millis: Long): String {
    val days = (millis / 86400000L).toInt()

    var year = 1970
    var remainingDays = days

    while (true) {
        val daysInYear = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
        if (remainingDays >= daysInYear) {
            remainingDays -= daysInYear
            year++
        } else {
            break
        }
    }

    val isLeap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val monthDays = intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    var month = 0
    while (true) {
        if (remainingDays >= monthDays[month]) {
            remainingDays -= monthDays[month]
            month++
        } else {
            break
        }
    }

    val day = remainingDays + 1
    val monthStr = (month + 1).toString().padStart(2, '0')
    val dayStr = day.toString().padStart(2, '0')

    return "$year-$monthStr-$dayStr"
}