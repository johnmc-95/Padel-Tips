package org.example.padeltips.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTO (Data Transfer Object) para la tabla 'classes' de Supabase.
// La etiqueta @Serializable es obligatoria para que Kotlin sepa cómo
// convertir este objeto en un JSON (al subir a Supabase) y viceversa (al descargar)
@Serializable
data class PadelClass(
    val id: String, // UUID generado automáticamente por Supabase

    // Mapeo ORM (Object-Relational Mapping):
    // Le decimos al serializador que cuando lea el JSON busque la clave "coach_id",
    // pero que en nuestro código Kotlin la llame "profesorId" para respetar el estándar camelCase
    @SerialName("coach_id")
    val profesorId: String,

    @SerialName("student_id")
    val estudianteId: String,

    // Usamos String para las marcas de tiempo (Timestamps ISO 8601)
    // porque es la forma más segura de mandarlas por la red sin problemas de zonas horarias
    @SerialName("start_time")
    val horaComienzo: String,

    @SerialName("end_time")
    val horaFinal: String,

    // Null Safety: Este campo es el único opcional (Int?).
    // Refleja que en la base de datos esta columna permite nulos (NULL),
    // ya que al solicitar una clase puede que aún no se sepa en qué pista se va a jugar,
    // si no se pondría al lado del tipo de dato el ? daría un error en Kotlin
    @SerialName("court_number")
    val pista: Int?,

    @SerialName("status")
    val estado: String // scheduled, pending, cancelled, completed...
)