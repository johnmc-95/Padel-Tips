package org.example.padeltips.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    // Esta variable 'client' es la forma de hablar con la base de datos
    val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.KEY
    ) {

        install(Auth)      // Importamos para el Login/Registro
        install(Postgrest) // Importamos para leer y escribir en las tablas a través del lenguaje SQL
    }
}