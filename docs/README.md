# Documentación de MediApi

Aquí queda registrado qué decidimos, qué falló y cómo se arregló, y qué ideas siguen en evaluación. La idea es que cualquiera que llegue al proyecto, incluyéndote a ti dentro de unos meses, entienda por qué cada cosa es como es.

| Carpeta | Para qué sirve |
|---|---|
| [`adr/`](adr/) | **Decisiones de arquitectura** (ADR, *Architecture Decision Record*): qué decidimos, por qué y qué alternativas descartamos. |
| [`problemas/`](problemas/) | **Problemas resueltos**: el síntoma (con captura), la causa y la solución paso a paso. |
| [`ideas/`](ideas/) | **Ideas y debates**: temas que conversamos, pendientes y hallazgos que todavía no son decisiones. |
| [`img/`](img/) | Capturas que usan los documentos. |

## Cómo agregar un documento

1. Copia la plantilla de la carpeta (`0000-plantilla.md`) y ponle el número que sigue: `0002-...md`, `0003-...md`.
2. Guarda las capturas en `img/` con el prefijo del documento, por ejemplo `problemas-0002-error-login.png`, y enlázalas así: `![Error al iniciar sesión](../img/problemas-0002-error-login.png)`.
3. Agrega el documento al índice de abajo.

## Índice

### Decisiones

- [0001 — Monorepo para backend y frontend](adr/0001-monorepo.md)

### Problemas resueltos

- [0001 — IntelliJ no reconoce el SDK de Java](problemas/0001-sdk-java-intellij.md)

### Ideas y debates

- [Registro de ideas y debates](ideas/ideas-y-debates.md)
