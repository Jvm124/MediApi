# 0002 — No se puede borrar la carpeta vieja del backend

- **Fecha:** 2026-11-09
- **Área:** sistema operativo (Windows)
- **Estado:** resuelto. La carpeta ya no existe (comprobado el 2026-11-09).

## Síntoma

Después de mudar el proyecto al monorepo, la carpeta vieja `Escritorio\Alura\api` no se pudo enviar a la Papelera. Windows respondió "Acceso denegado" incluso al intentar renombrarla. La carpeta vieja del frontend (`WebstormProjects\reserva-medica-web`) sí se borró sin problemas.

## Diagnóstico

| Qué se revisó | Resultado |
|---|---|
| IntelliJ IDEA o WebStorm abiertos | No |
| Procesos de Java o Maven corriendo | No |
| Algún proceso con esa ruta en su línea de comandos | No |
| Desde dónde arrancó la sesión de Claude Code | Desde `Escritorio\Alura\api\api` |

## Causa

En Windows no se puede borrar ni renombrar una carpeta si algún programa la tiene como **directorio de trabajo**. La sesión de Claude Code se abrió dentro de esa carpeta, así que la mantiene ocupada aunque después se haya cambiado a `MediApi`. También la bloquearía una terminal abierta ahí.

## Solución

1. Cierra la sesión de Claude Code que se abrió en la carpeta vieja y cualquier terminal que esté ubicada ahí.
2. En el Explorador, borra `Escritorio\Alura\api`. Irá a la Papelera.
3. Si sigue sin dejarte, reinicia Windows y vuelve a intentarlo.

Antes de borrarla se comprobó que todo su contenido ya está en `MediApi`: todos los commits (incluido el merge del PR #6), la configuración `.idea` y el `HELP.md`. Lo demás es `target/`, que se regenera al compilar.

## Cómo evitarlo en el futuro

- Antes de mover o borrar un proyecto, cierra los IDEs, las terminales y las sesiones de Claude que estén dentro de él.
- Abre las sesiones de Claude en la raíz del monorepo (`Escritorio\MediApi`).
