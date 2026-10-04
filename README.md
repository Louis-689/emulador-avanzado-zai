# Flashito para Android

Aplicación Android 7+ que añade **Flashito** al menú de selección de texto mediante `ACTION_PROCESS_TEXT`.

## Uso

1. Instala el APK publicado en Releases o generado por GitHub Actions.
2. Abre Flashito y configura endpoint, API key y modelo.
3. Selecciona una pregunta en Chrome u otra aplicación.
4. Toca **Flashito** en el menú de selección.

## Compilación

Requiere Java 17 y Gradle 8.9:

```text
gradle assembleDebug --no-daemon
```

También puedes ejecutar manualmente el workflow `build-apk` de GitHub Actions.

Las credenciales personales se almacenan en el dispositivo y nunca deben añadirse al repositorio.

