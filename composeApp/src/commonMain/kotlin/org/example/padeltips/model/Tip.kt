package org.example.padeltips.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTO (Data Transfer Object) para la tabla 'tips' de Supabase
@Serializable
data class Tip(
    // Identificador único universal (UUID) generado por la base de datos
    val id: String,

    // Relación Opcional: Un tip puede estar ligado a una clase en particular (ej: "Mejora el saque que vimos hoy")
    // o ser un consejo general desconectado de una clase. Por eso es nullable (String?)
    @SerialName("class_id")
    val claseId: String?,

    // Relaciones (Claves foráneas): Nos indican quién envía y quién recibe
    // En SQL estas columnas enlazarán con la tabla 'profiles'
    @SerialName("student_id")
    val estudianteId: String,

    @SerialName("coach_id")
    val profesorId: String,

    // El cuerpo principal del mensaje
    @SerialName("content")
    val contenido: String,

    // Gestión de archivos multimedia
    @SerialName("media_url")
    val imagen: String?,

    // Las fechas siempre viajan mejor como Strings (ISO 8601) a través de JSON para evitar
    // problemas de parseo entre el backend (PostgreSQL) y el frontend (Kotlin)
    @SerialName("reminder_date")
    val fechaRecordatorio: String?,

    // Confirmación de lectura del Tip
    @SerialName("is_viewed")
    val confirmacionLectura: Boolean
)