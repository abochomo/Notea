# Notea - proyecto base ejecutable

Este ZIP contiene una base ejecutable para Android Studio con:
- Jetpack Compose + Material 3
- Hilt
- Room
- AlarmManager + WorkManager
- Notas operativas
- Recordatorios operativos con notificación local
- Home, calendario y detalle de día
- Base de datos y estructura para rutinas

## 1. Crear el proyecto en Android Studio

1. Abre Android Studio.
2. Pulsa **New Project**.
3. Elige **Empty Activity**.
4. Configura:
   - **Name**: `Notea`
   - **Package name**: `com.notea`
   - **Language**: `Kotlin`
   - **Minimum SDK**: `26`
5. Finaliza la creación.
6. Cierra el proyecto recién creado si Android Studio intenta sincronizarlo con sus archivos por defecto.

## 2. Sustituir los archivos del proyecto

1. Descomprime este ZIP en una carpeta.
2. En Android Studio, abre la carpeta raíz `NoteaProject` como proyecto.
3. Espera a que Android Studio detecte Gradle.
4. Pulsa **Sync Project with Gradle Files**.

## 3. Dónde va cada fichero

### Raíz del proyecto
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`

### Módulo app
- `app/build.gradle.kts`

### Manifest
- `app/src/main/AndroidManifest.xml`

### Código Kotlin
Todo el código va bajo:
- `app/src/main/java/com/notea/...`

### Recursos
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`

## 4. Qué ficheros tocar primero

Si quieres entender el proyecto rápido, abre primero en este orden:

1. `gradle/libs.versions.toml`
2. `app/build.gradle.kts`
3. `app/src/main/AndroidManifest.xml`
4. `app/src/main/java/com/notea/NoteaApplication.kt`
5. `app/src/main/java/com/notea/MainActivity.kt`
6. `app/src/main/java/com/notea/navigation/NoteaNavHost.kt`
7. `app/src/main/java/com/notea/presentation/home/HomeScreen.kt`
8. `app/src/main/java/com/notea/data/local/db/NoteaDatabase.kt`

## 5. Qué funciona ya

### Operativo
- Crear y editar notas
- Marcar notas favoritas y archivadas
- Crear y editar recordatorios
- Programar notificaciones locales
- Reprogramar recordatorios al reiniciar el dispositivo
- Ver actividad en calendario a partir de notas y recordatorios
- Guardado local con Room

### Base preparada
- Entidades de rutinas
- DAO de rutinas
- Repositorio de rutinas
- Pantalla de creación/edición de rutina
- Pantallas placeholder para constructor de preguntas, respuesta e historial

## 6. Cómo ejecutar la app

1. Crea un emulador API 35 o 36.
2. Pulsa **Run**.
3. Al abrirse la app:
   - concede notificaciones si Android lo pide
   - crea una nota
   - crea un recordatorio futuro
4. Comprueba la notificación cuando llegue la hora.

## 7. Si falla la sincronización

### Error con JDK
Configura **Gradle JDK = 17** en:
- `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`

### Error con dependencias
Pulsa:
- **File > Sync Project with Gradle Files**

### Error de notificaciones
En Android 13+ concede el permiso de notificaciones.
En Android 14+ las alarmas exactas pueden no estar concedidas por defecto: la app degrada a alarmas inexactas.

## 8. Siguiente paso recomendado

Si quieres completar rutinas + historial + respuesta dinámica, sigue este orden:
1. `RoutineEditorViewModel`
2. `RoutineDao`
3. `RoutineRepositoryImpl`
4. añadir `RoutineQuestionEntity`, `QuestionOptionEntity`, `RoutineExecutionEntity`, `AnswerEntity` a la UI
5. implementar `QuestionBuilderScreen`
6. implementar `RoutineAnswerScreen`
7. extender `CalendarRepositoryImpl`

