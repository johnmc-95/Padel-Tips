package org.example.padeltips.repository

import io.github.jan.supabase.postgrest.from
import org.example.padeltips.data.SupabaseClient
import org.example.padeltips.model.Profile

object UserRepository {

    // Función obtener que tipo de rol tiene el usuario
    suspend fun obtenerPerfilUsuario(userId: String): Profile? {
        return try {
            // Le decimos a Supabase: "Ve a la tabla profiles"
            val perfil = SupabaseClient.client.from("profiles")
                .select {
                    // Filtra buscando la fila donde el id sea igual al userId que te pase
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>() // Traduce el JSON a nuestra data class Profile

            perfil // Devolvemos el perfil encontrado

        } catch (e: Exception) {
            println("🚨 ERROR AL OBTENER PERFIL: ${e.message}")
            null // Si hay error, devolvemos nulo
        }
    }
}
