# Notea

Notas, recordatorios y rutinas en una sola app para Android, con calendario y formato Markdown.
Kotlin + Jetpack Compose (Material 3), datos locales con Room. Android 8.0 (API 26) o superior.

## Qué hace

- **Notas** en Markdown con editor cómodo: barra de formato (casillas, listas, títulos, negrita, cursiva,
  tachado, citas, código, enlaces, separadores), resaltado en vivo, continuación automática de listas y
  vista renderizada con casillas pulsables y tablas. Guardado automático, fijar, archivar, etiquetas y compartir.
- **Recordatorios**: una nota con fecha y hora. La notificación lleva el título de la nota y al tocarla se abre
  la nota completa. Acciones «Hecho» y «Posponer 15 min» desde la propia notificación.
- **Rutinas**: diario periódico (diaria, semanal, mensual o días concretos) a la hora que elijas, como nota
  libre con plantilla o como **cuestionario** (texto, párrafo, número, opción única, casillas, desplegable,
  escala, estrellas, sí/no, hora y fecha). Las preguntas de opciones incluyen siempre «Otro», y su texto se
  puede convertir en opción permanente con un toque.
- **Inicio**: todo junto con búsqueda, filtros por tipo, orden, vista en lista o cuadrícula y una tarjeta
  «Para hoy» con las rutinas pendientes y los recordatorios del día.
- **Calendario**: un color por tipo (nota, recordatorio, rutina); las rutinas respondidas se ven como punto
  relleno y las asignadas sin responder como anillo. Desde cada día se crean notas, recordatorios y rutinas,
  y se responden o modifican rutinas de cualquier fecha, pasada o futura.
- **Ficha de rutina**: rachas, cumplimiento, días sin responder, resumen por pregunta, historial y
  exportación a CSV.
- **Ajustes**: tema claro/oscuro/sistema, siete paletas más colores dinámicos (Android 12+), negro puro,
  estado de permisos y copia de seguridad completa en JSON (exportar e importar).

## Compilar

Con el SDK de Android y JDK 17 instalados:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk (minificado)
```

El workflow `.github/workflows/build.yml` compila en cada push a `master` o `v2` y publica los APK en la
release `latest` del repositorio.

Todas las compilaciones se firman con `keystore/notea.jks`, incluida en el repo para que cualquier APK
se pueda instalar encima de otro sin perder datos. Es una clave pública: si algún día publicas la app,
sustitúyela por una privada.

## Estructura

```
app/src/main/java/com/notea/app/
  data/     Room (notas, rutinas, respuestas), modelo de cuestionarios, recurrencias, preferencias, copias
  md/       Núcleo Markdown en Kotlin puro: análisis por bloques y operaciones de edición
  notify/   Alarmas (AlarmManager), notificaciones y receptores (arranque, acciones)
  ui/       Pantallas Compose: inicio, editor, calendario, rutinas, respuestas, ajustes, tema
```
