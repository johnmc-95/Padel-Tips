package org.example.padeltips.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTO (Data Transfer Object) para la tabla 'profiles' de Supabase.

@Serializable // Permite la conversión bidireccional: Objeto Kotlin - Texto JSON
data class Profile(

    // El ID principal. En Supabase suele coincidir exactamente con el UUID de autenticación (auth.users).
    val id: String,

    // Mapeo de snake_case (BBDD) a camelCase (Kotlin)
    @SerialName("full_name")
    val nombreCompleto: String,

    @SerialName("role")
    val rol: String, // Control de acceso basado en roles: "coach" o "student"

    // Null Safety: El '?' indica que la base de datos permite que esta celda esté vacía (NULL).
    // Si intentáramos guardar un null en un String normal, la app crashearía instantáneamente.
    @SerialName("avatar_url")
    val avatar: String?,

    // Valores por defecto (= null):
    // Esto es un salvavidas. Si añades columnas nuevas a la BBDD (como 'email' o 'level')
    // pero los usuarios antiguos no las tienen rellenadas, al descargar el JSON a Kotlin
    // no dará error de "Missing Field", simplemente le asignará 'null'. Es muy buena práctica
    val email: String? = null,
    val level: String? = null

)