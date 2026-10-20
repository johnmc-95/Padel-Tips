# PadelTips

**PadelTips** es una aplicación móvil multiplataforma desarrollada con Kotlin Multiplatform y Compose Multiplatform. Su principal utilidad es ofrecer a los entusiastas del pádel una plataforma centralizada para acceder a consejos, estrategias y recursos que les ayuden a mejorar su juego. La aplicación integra un sistema de autenticación de usuarios y utiliza Supabase como su backend, gestionando tanto la base de datos para los consejos como la autenticación de usuarios de forma eficiente.

## Aspectos Clave

*   **Plataforma Cruzada**: Desarrollada para Android e iOS a partir de una única base de código Kotlin.
*   **Interfaz de Usuario Moderna**: Utiliza Jetpack Compose Multiplatform para una experiencia de usuario nativa y consistente en ambas plataformas.
*   **Autenticación de Usuarios**: Implementa un sistema de autenticación robusto utilizando Supabase Auth.
*   **Backend como Servicio (BaaS)**: Se integra con Supabase para la gestión de la base de datos (Postgrest) y la autenticación de usuarios.
*   **Comunicación de Red**: Emplea el cliente HTTP Ktor para todas las interacciones de red con el backend.

## Tecnologías Utilizadas

*   **Kotlin Multiplatform**: Para compartir lógica de negocio y UI entre Android e iOS.
*   **Jetpack Compose Multiplatform**: Para construir la interfaz de usuario declarativa.
*   **Supabase**: Como backend principal, proporcionando:
    *   **Supabase Auth**: Para la gestión de usuarios y autenticación.
    *   **Supabase Postgrest**: Para la interacción con la base de datos PostgreSQL.
*   **Ktor Client**: Para las operaciones de red.
*   **Gradle Kotlin DSL**: Para la configuración del proyecto.

## Estructura del Proyecto

El proyecto sigue una estructura modular típica de Kotlin Multiplatform:

*   `composeApp`: Contiene el código compartido para las aplicaciones de Compose Multiplatform. Incluye los siguientes subdirectorios:
    *   `commonMain`: Código común para todos los destinos (Android, iOS).
    *   `androidMain`: Código específico de Android.
    *   `iosMain`: Código específico de iOS.

*   `iosApp`: Contiene la aplicación iOS y el código SwiftUI de entrada.

## Cómo Ponerlo a Funcionar

### Prerrequisitos

*   **Android Studio**: Para el desarrollo y ejecución en Android.
*   **Xcode**: Para el desarrollo y ejecución en iOS (solo macOS).
*   **JDK 11 o superior**: Para compilar el proyecto Kotlin.
*   **Configuración de Supabase**: Necesitarás una instancia de Supabase configurada con tu base de datos y autenticación. Asegúrate de tener las claves necesarias.

### Compilar y Ejecutar la Aplicación Android

1.  Abre el proyecto en Android Studio.
2.  Sincroniza el proyecto con los archivos Gradle.
3.  Selecciona la configuración de ejecución `composeApp` en la barra de herramientas del IDE.
4.  Haz clic en el botón de `Run` (o `Ctrl+R` / `Cmd+R`).

Alternativamente, puedes compilar y ejecutar desde la terminal:

*   **macOS/Linux**:
    ```shell
    ./gradlew :composeApp:installDebug
    ./gradlew :composeApp:runDebug
    ```
*   **Windows**:
    ```shell
    .\gradlew.bat :composeApp:installDebug
    .\gradlew.bat :composeApp:runDebug
    ```

### Compilar y Ejecutar la Aplicación iOS

1.  Abre el directorio `iosApp` en Xcode.
2.  Selecciona el esquema `iosApp` y un simulador o dispositivo de destino.
3.  Haz clic en el botón `Run` en Xcode (o `Cmd+R`).

### Configuración de Supabase

La aplicación necesita la URL del proyecto de Supabase y una clave publicable. Gradle genera un archivo Kotlin compartido para Android e iOS con esos valores, tomándolos de una propiedad Gradle, una variable de entorno o `local.properties` (en ese orden).

#### Desarrollo local

Añade estas propiedades a `local.properties`, en la raíz del proyecto:

```properties
SUPABASE_URL=https://<tu-proyecto>.supabase.co
SUPABASE_KEY=<tu-clave-publicable>
```

Sustituye los marcadores por los valores de tu proyecto y no incluyas comillas ni los signos `< >`. `local.properties` ya está excluido de Git por `.gitignore`.

#### Integración continua

Configura `SUPABASE_URL` y `SUPABASE_KEY` como variables protegidas del entorno del trabajo de CI. No incluyas sus valores en el YAML del pipeline ni en el repositorio. En PowerShell puedes probarlo localmente así:

```powershell
$env:SUPABASE_URL = "https://<tu-proyecto>.supabase.co"
$env:SUPABASE_KEY = "<tu-clave-publicable>"
.\gradlew.bat :composeApp:assembleDebug
```

La clave publicable se incorpora al binario de la app y se puede extraer: no es un secreto. **Nunca incluyas una clave `secret` o `service_role` en una aplicación cliente.** Protege los datos con políticas RLS y permisos mínimos en Supabase.

Si una clave privada llegó a un commit o a GitHub, elimínala desde Supabase y revisa el historial: borrarla del README no la elimina de commits anteriores.
