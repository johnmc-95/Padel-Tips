package org.example.padeltips

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.example.padeltips.UI.teacher.AddClassTeacher
import org.example.padeltips.UI.teacher.AddGeneralAdviceTeacher
import org.example.padeltips.UI.teacher.AddPersonalAdviceTeacher
import org.example.padeltips.UI.Login
import org.example.padeltips.UI.MyAccount
import org.example.padeltips.UI.student.MainStudent
import org.example.padeltips.UI.teacher.MainTeacher
import org.example.padeltips.UI.Register
import org.example.padeltips.UI.student.ClassesStudent
import org.example.padeltips.UI.student.GeneralAdvicesStudent
import org.example.padeltips.UI.student.GeneralTipsListStudent
import org.example.padeltips.UI.student.PersonalTipsStudent
import org.example.padeltips.UI.student.RequestClassStudent
import org.example.padeltips.UI.teacher.ClassesTeacher
import org.example.padeltips.UI.teacher.StudentsTeacher
import org.example.padeltips.UI.teacher.AdvicesTeacher
import org.example.padeltips.UI.teacher.GeneralTipsListTeacher
import org.example.padeltips.UI.teacher.InboxTeacher
import org.example.padeltips.UI.teacher.StudentDetailTeacher
import org.example.padeltips.UI.teacher.StudentHistoryTeacher
import org.example.padeltips.UI.teacher.StudentsListTeacher
import org.example.padeltips.UI.DashboardAdmin
import org.example.padeltips.model.Profile
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val AzulCorporativo = Color(0xFF1E3A8A)
val AzulDestacado = Color(0xFF3B82F6)
val AzulOscuro = Color(0xFF172554)

// Creamos nuestro Tema personalizado para toda la aplicación
val MiTemaPadel = lightColorScheme(
    primary = AzulCorporativo,
    onPrimary = Color.White,
    secondary = AzulDestacado,
    onSecondary = Color.White,
    tertiary = AzulOscuro,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black
)

// Añadimos las rutas
enum class Rutas {
    LOGIN,
    REGISTRO,
    MAIN_TEACHER,
    MAIN_STUDENT,
    DASHBOARD_ADMIN,
    CLASSES_TEACHER,
    STUDENTS_TEACHER,
    ADVICES_TEACHER,
    STUDENT_DETAIL_TEACHER,
    ADD_CLASS_TEACHER,
    ADD_GENERAL_ADVICE,
    GENERAL_TIPS_LIST,
    ADD_PERSONAL_ADVICE,
    PERSONAL_TIPS_STUDENT,
    GENERAL_ADVICES_STUDENT,
    GENERAL_TIPS_LIST_STUDENT,
    CLASSES_STUDENT,
    REQUEST_CLASS_STUDENT,
    MY_ACCOUNT,
    STUDENTS_LIST_TEACHER,
    ADD_PERSONAL_ADVICE_TEACHER,
    STUDENT_HISTORY_TEACHER,
    INBOX_TEACHER
}

@Composable
fun App() {
    var pantallaActual by remember { mutableStateOf(Rutas.LOGIN) }
    var categoriaFiltro by remember { mutableStateOf("") }
    var subcategoriaFiltro by remember { mutableStateOf("") }
    var perfilActual by remember { mutableStateOf<Profile?>(null) }
    var alumnoSeleccionado by remember { mutableStateOf<Profile?>(null) }

    MaterialTheme(colorScheme = MiTemaPadel) {
        when (pantallaActual) {
            Rutas.LOGIN -> {
                Login(
                    irARegistro = { pantallaActual = Rutas.REGISTRO },
                    onLoginExito = { perfil ->
                        perfilActual = perfil
                        // Redirigimos según el rol (Student, Coach o Admin)
                        when (perfil.rol) {
                            "coach" -> pantallaActual = Rutas.MAIN_TEACHER
                            "admin" -> pantallaActual = Rutas.DASHBOARD_ADMIN
                            else -> pantallaActual = Rutas.MAIN_STUDENT
                        }
                    }
                )
            }
            Rutas.REGISTRO -> {
                Register(
                    irALogin = { pantallaActual = Rutas.LOGIN }
                )
            }

            Rutas.DASHBOARD_ADMIN -> {
                DashboardAdmin(
                    irALogin = { pantallaActual = Rutas.LOGIN }
                )
            }

            Rutas.MAIN_TEACHER -> {
                MainTeacher(
                    perfil = perfilActual,
                    irALogin = { pantallaActual = Rutas.LOGIN },
                    irAClases = { pantallaActual = Rutas.CLASSES_TEACHER },
                    irAAlumnos = { pantallaActual = Rutas.STUDENTS_LIST_TEACHER },
                    irAConsejos = { pantallaActual = Rutas.ADVICES_TEACHER },
                    irABandeja = { pantallaActual = Rutas.INBOX_TEACHER },
                    irAMiCuenta = { pantallaActual = Rutas.MY_ACCOUNT }
                )
            }
            Rutas.MAIN_STUDENT -> {
                MainStudent(
                    perfil = perfilActual,
                    irALogin = { pantallaActual = Rutas.LOGIN },
                    irAMisTips = { pantallaActual = Rutas.PERSONAL_TIPS_STUDENT },
                    irAClases = { pantallaActual = Rutas.CLASSES_STUDENT },
                    irAConsejos = { pantallaActual = Rutas.GENERAL_ADVICES_STUDENT },
                    irAMiCuenta = { pantallaActual = Rutas.MY_ACCOUNT }
                )
            }
            Rutas.CLASSES_TEACHER -> {
                perfilActual?.id?.let { miCoachId ->
                    ClassesTeacher(
                        coachId = miCoachId,
                        onVolver = { pantallaActual = Rutas.MAIN_TEACHER },
                        onAñadirClase = { pantallaActual = Rutas.ADD_CLASS_TEACHER }
                    )
                }
            }
            Rutas.ADD_CLASS_TEACHER -> {
                perfilActual?.id?.let { coachId ->
                    AddClassTeacher(
                        coachId = coachId,
                        onVolver = { pantallaActual = Rutas.CLASSES_TEACHER },
                        onClaseGuardada = { pantallaActual = Rutas.CLASSES_TEACHER }
                    )
                }
            }
            Rutas.STUDENTS_TEACHER -> {
                StudentsTeacher(
                    onVolver = { pantallaActual = Rutas.MAIN_TEACHER },
                    onAlumnoClick = { alumnoClicado ->
                        alumnoSeleccionado = alumnoClicado
                        pantallaActual = Rutas.STUDENT_DETAIL_TEACHER
                    }
                )
            }
            Rutas.STUDENT_DETAIL_TEACHER -> {
                if (alumnoSeleccionado != null) {
                    StudentDetailTeacher(
                        alumno = alumnoSeleccionado!!,
                        onVolver = { pantallaActual = Rutas.STUDENTS_LIST_TEACHER },
                        irAEscribirConsejo = {
                            pantallaActual = Rutas.ADD_PERSONAL_ADVICE_TEACHER
                        },
                        irAHistorial = {
                            pantallaActual = Rutas.STUDENT_HISTORY_TEACHER
                        }
                    )
                }
            }
            Rutas.ADVICES_TEACHER -> {
                perfilActual?.id?.let { miCoachId ->
                    AdvicesTeacher(
                        coachId = miCoachId,
                        onVolver = { pantallaActual = Rutas.MAIN_TEACHER },
                        onAñadirConsejoGeneral = { pantallaActual = Rutas.ADD_GENERAL_ADVICE },
                        onAñadirConsejoPersonalizado = { pantallaActual = Rutas.ADD_PERSONAL_ADVICE },
                        onVerConsejos = { cat, subcat ->
                            categoriaFiltro = cat
                            subcategoriaFiltro = subcat
                            pantallaActual = Rutas.GENERAL_TIPS_LIST
                        }
                    )
                }
            }
            Rutas.ADD_GENERAL_ADVICE -> {
                perfilActual?.id?.let { miCoachId ->
                    AddGeneralAdviceTeacher(
                        coachId = miCoachId,
                        onVolver = { pantallaActual = Rutas.ADVICES_TEACHER },
                        onConsejoGuardado = { pantallaActual = Rutas.ADVICES_TEACHER }
                    )
                }
            }
            Rutas.GENERAL_TIPS_LIST -> {
                GeneralTipsListTeacher(
                    categoria = categoriaFiltro,
                    subcategoria = subcategoriaFiltro,
                    onVolver = { pantallaActual = Rutas.ADVICES_TEACHER }
                )
            }
            Rutas.ADD_PERSONAL_ADVICE -> {
                perfilActual?.id?.let { miCoachId ->
                    AddPersonalAdviceTeacher(
                        coachId = miCoachId,
                        onVolver = { pantallaActual = Rutas.ADVICES_TEACHER },
                        onConsejoGuardado = { pantallaActual = Rutas.ADVICES_TEACHER }
                    )
                }
            }
            Rutas.STUDENTS_LIST_TEACHER -> {
                perfilActual?.id?.let { miCoachId ->
                    StudentsListTeacher(
                        coachId = miCoachId,
                        onVolver = { pantallaActual = Rutas.MAIN_TEACHER },
                        onAlumnoSeleccionado = { alumno ->
                            alumnoSeleccionado = alumno
                            pantallaActual = Rutas.STUDENT_DETAIL_TEACHER
                        }
                    )
                }
            }

            Rutas.ADD_PERSONAL_ADVICE_TEACHER -> {
                if (perfilActual != null) {
                    AddPersonalAdviceTeacher(
                        coachId = perfilActual!!.id,
                        alumnoInicial = alumnoSeleccionado,
                        onVolver = {
                            pantallaActual = if (alumnoSeleccionado != null) Rutas.STUDENT_DETAIL_TEACHER else Rutas.MAIN_TEACHER
                        },
                        onConsejoGuardado = {
                            pantallaActual = if (alumnoSeleccionado != null) Rutas.STUDENT_DETAIL_TEACHER else Rutas.MAIN_TEACHER
                        }
                    )
                }
            }
            Rutas.STUDENT_HISTORY_TEACHER -> {
                if (perfilActual != null && alumnoSeleccionado != null) {
                    StudentHistoryTeacher(
                        coachId = perfilActual!!.id,
                        alumno = alumnoSeleccionado!!,
                        onVolver = { pantallaActual = Rutas.STUDENT_DETAIL_TEACHER }
                    )
                }
            }
            Rutas.INBOX_TEACHER -> {
                if (perfilActual != null) {
                    InboxTeacher(
                        coachId = perfilActual!!.id,
                        onVolver = { pantallaActual = Rutas.MAIN_TEACHER }
                    )
                }
            }
            Rutas.PERSONAL_TIPS_STUDENT -> {
                perfilActual?.id?.let { studentId ->
                    PersonalTipsStudent(
                        studentId = studentId,
                        onVolver = { pantallaActual = Rutas.MAIN_STUDENT }
                    )
                }
            }
            Rutas.CLASSES_STUDENT -> {
                perfilActual?.id?.let { miStudentId ->
                    ClassesStudent(
                        studentId = miStudentId,
                        onVolver = { pantallaActual = Rutas.MAIN_STUDENT },
                        onSolicitarClase = { pantallaActual = Rutas.REQUEST_CLASS_STUDENT }
                    )
                }
            }
            Rutas.REQUEST_CLASS_STUDENT -> {
                perfilActual?.id?.let { miStudentId ->
                    RequestClassStudent(
                        studentId = miStudentId,
                        onVolver = { pantallaActual = Rutas.CLASSES_STUDENT },
                        onSolicitudEnviada = { pantallaActual = Rutas.CLASSES_STUDENT }
                    )
                }
            }
            Rutas.GENERAL_ADVICES_STUDENT -> {
                GeneralAdvicesStudent(
                    onVolver = { pantallaActual = Rutas.MAIN_STUDENT },
                    onVerConsejos = { cat, subcat ->
                        categoriaFiltro = cat
                        subcategoriaFiltro = subcat
                        pantallaActual = Rutas.GENERAL_TIPS_LIST_STUDENT
                    }
                )
            }

            Rutas.GENERAL_TIPS_LIST_STUDENT -> {
                GeneralTipsListStudent(
                    categoria = categoriaFiltro,
                    subcategoria = subcategoriaFiltro,
                    onVolver = { pantallaActual = Rutas.GENERAL_ADVICES_STUDENT }
                )
            }
            Rutas.MY_ACCOUNT -> {
                MyAccount(
                    perfil = perfilActual,
                    onVolver = {
                        when (perfilActual?.rol) {
                            "coach" -> pantallaActual = Rutas.MAIN_TEACHER
                            "admin" -> pantallaActual = Rutas.DASHBOARD_ADMIN
                            else -> pantallaActual = Rutas.MAIN_STUDENT
                        }
                    },
                    onCuentaEliminada = {
                        perfilActual = null // Limpiamos la memoria
                        pantallaActual = Rutas.LOGIN
                    }
                )
            }
        }
    }
}