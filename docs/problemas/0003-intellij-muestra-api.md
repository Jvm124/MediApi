# 0003 — IntelliJ muestra el módulo como `backend [api]`

- **Fecha:** 2026-11-09
- **Área:** IDE
- **Estado:** resuelto

## Síntoma

En el panel *Project* de IntelliJ, la carpeta del backend aparecía como `backend [api]`, y la pestaña del `pom.xml` decía `pom.xml (api)`. Cambiar `spring.application.name` a `backend` en `application.properties` no lo arregló.

## Causa

IntelliJ toma el nombre del módulo del `pom.xml` (`<artifactId>` y `<name>`), no de `application.properties`. Cuando ese nombre no coincide con el de la carpeta, lo muestra entre corchetes. El proyecto venía del curso con `<artifactId>api</artifactId>`.

`spring.application.name` es otra cosa: es el nombre de la aplicación cuando está corriendo. Spring lo usa, por ejemplo, en los logs y en las herramientas de monitoreo.

## Solución

1. En `backend/pom.xml`, cambiar `<artifactId>` y `<name>` de `api` a `backend`.
2. En IntelliJ, recargar Maven: con el botón *Load Maven Changes* que aparece sobre el `pom.xml`, o en la ventana *Maven* con *Reload All Maven Projects*.
3. Si la configuración de ejecución `ApiApplication` marca un error de módulo, abrir *Run → Edit Configurations…* y elegir el módulo `backend` en el campo `-cp`.

También cambia el nombre del `.jar` que genera Maven: ahora es `backend-0.0.1-SNAPSHOT.jar`.

## Cómo comprobar que quedó resuelto

- El panel *Project* muestra `backend`, sin corchetes.
- `./mvnw test` muestra `Building backend 0.0.1-SNAPSHOT`.

## Cómo evitarlo en el futuro

Al crear un proyecto con Spring Initializr, elegir desde el principio el *Artifact* que se quiere ver en el IDE y en el `.jar`.
