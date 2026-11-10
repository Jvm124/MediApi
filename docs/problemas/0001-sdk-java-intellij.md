# 0001 — IntelliJ no reconoce el SDK de Java

- **Fecha:** 2026-11-09
- **Área:** IDE (IntelliJ IDEA 2026.3)
- **Estado:** resuelto en la terminal; falta confirmarlo dentro de IntelliJ

## Síntoma

Después de cambiar de herramienta o de instalación de Java, IntelliJ dejó de reconocer el SDK del proyecto y no podía ejecutarlo.

> 📸 Captura pendiente: guárdala como `docs/img/problemas-0001-error-sdk.png` y enlázala aquí.

## Diagnóstico

| Qué se revisó | Resultado |
|---|---|
| `java -version` | Temurin 21.0.12.1 ✅ |
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` ✅ |
| SDK del proyecto (`.idea/misc.xml`) | `temurin-21`, nivel de lenguaje 21 ✅ |
| `./mvnw -DskipTests compile` | Compila sin errores ✅ |
| SDKs registrados en IntelliJ | Hay 3 y dos apuntan a carpetas que ya no existen ❌ |

SDKs registrados en IntelliJ (`jdk.table.xml`):

| Nombre | Ruta | ¿Existe? |
|---|---|---|
| `21` | `%USERPROFILE%\Escritorio\jdks\jdk-21_windows-x64_bin\jdk-21.0.9` | ❌ se borró |
| `21 (2)` | `C:\Program Files\Java\jdk-21` | ❌ no existe |
| `temurin-21` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` | ✅ |

## Causa

Al cambiar de JDK se borraron las carpetas de las instalaciones anteriores, pero IntelliJ las siguió teniendo registradas como SDKs. Si el proyecto, el Maven Runner o el importador de Maven usan uno de esos SDKs rotos, IntelliJ no puede compilar ni ejecutar, aunque Java funcione bien desde la terminal.

## Solución

1. **File → Project Structure → Platform Settings → SDKs**: elimina `21` y `21 (2)`. Deja solo `temurin-21`.
2. **Project Structure → Project**: en *SDK* elige `temurin-21` y en *Language level* elige `21`.
3. **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**: en *JRE* elige `Use Project JDK`.
4. **Maven → Importing**: en *JDK for importer* elige `Use Project JDK`.
5. Pulsa **Reload All Maven Projects** (🔄 en la ventana de Maven).

## Cómo comprobar que quedó resuelto

- Ejecuta `ApiApplication` desde IntelliJ y verifica que arranca sin errores de SDK.
- En la terminal, dentro de `backend/`: `./mvnw -DskipTests compile` debe terminar en `BUILD SUCCESS`.

## Cómo evitarlo en el futuro

- Antes de borrar la carpeta de un JDK, quítalo primero de *Project Structure → SDKs*.
- Mantén un solo JDK 21 instalado con su instalador (Temurin) y con `JAVA_HOME` apuntando a él.
- Si necesitas varias versiones de Java, dales nombres claros en IntelliJ (`temurin-21`, `temurin-25`) para no confundirlas.
