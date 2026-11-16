# 0006 — La ficha del médico o del paciente sigue a su cuenta

- **Fecha:** 2026-11-16
- **Estado:** aceptada

## Contexto

Un médico o un paciente con acceso web tiene dos estados:

- **La cuenta** (`Usuario.estado`): ACTIVO, SUSPENDIDO o BAJA. Decide quién puede iniciar sesión.
- **La ficha** (`Medico.activo` o `Paciente.activo`): decide quién aparece en las listas y a quién se le puede reservar.

Cada acción cambiaba solo uno de los dos. Es el hallazgo BUG-05 de la [revisión inicial](../revisiones/0001-revision-inicial.md):

- Dar de baja a un paciente desactivaba su ficha, pero su cuenta seguía ACTIVO en el panel de usuarios.
- «Reincorporar» la cuenta de un médico dado de baja la dejaba ACTIVO, pero su ficha seguía desactivada: el médico no podía entrar ni aparecía para reservar, y no había forma de volver a activar su ficha.
- Suspender o dar de baja la cuenta de un médico no desactivaba su ficha: se le podía seguir reservando, aunque no podía entrar a atender.
- Si la cuenta de un médico ya estaba de baja, darlo de baja en «Médicos activos» fallaba y no se guardaba nada.

Los dos estados hacen falta: un paciente dado de alta en mostrador tiene ficha pero no cuenta, y un administrador o una recepcionista tienen cuenta pero no ficha.

## Decisión

**La ficha solo está activa si su cuenta está ACTIVA.** La regla vive en un solo lugar, la clase `Usuario`: `suspender()`, `reactivar()` y `darDeBaja()` cambian la cuenta y su ficha a la vez.

| Acción | Cuenta | Ficha |
|---|---|---|
| Suspender | SUSPENDIDO | inactiva |
| Dar de baja, en el panel de usuarios | BAJA | inactiva |
| Reactivar o «Reincorporar» | ACTIVO | activa |
| Dar de baja a un médico o a un paciente | BAJA, si tiene cuenta | inactiva |

- **Suspender también desactiva la ficha.** Un médico que no puede entrar no ve su agenda ni puede marcar consultas como atendidas, así que no debe recibir reservas nuevas. Al reactivarlo vuelve todo.
- Las bajas desde la ficha (`MedicoService.eliminar` y `PacienteService.eliminar`) desactivan la ficha y dan de baja la cuenta, si la hay. Si la cuenta ya estaba de baja, no la tocan, en vez de fallar.
- Cuando un paciente se da de baja a sí mismo, el servicio cambia la cuenta que le devuelve el repositorio. La de la sesión la cargó el filtro de seguridad, fuera de la transacción, y sus cambios no se guardarían.
- `Medico` y `Paciente` cambian su ficha con `activar()` y `desactivar()`. Antes solo existían `eliminacionLogicaMedico()` y `eliminacionLogicaPaciente()`, pero una suspensión no es una baja.
- Con la ficha desactivada, los mensajes dicen «El médico está suspendido o dado de baja» y «El paciente está suspendido o dado de baja».
- `Usuario.isEnabled()` sigue mirando la cuenta y la ficha, por si alguien cambia los datos a mano.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Un único estado, la otra solución que proponía la revisión | Una sola fuente de verdad. | No alcanza: un paciente de mostrador no tiene cuenta y un administrador no tiene ficha. Habría que cambiar tablas y consultas. |
| Que suspender solo bloquee el acceso y deje la ficha activa | Coincide con lo que decía el aviso del botón «Suspender». | Al médico suspendido se le podría seguir reservando, aunque no puede entrar a atender. |
| Un servicio aparte que cambie los dos estados | `Usuario` no tendría lógica nueva. | La regla quedaría lejos de los estados que cambia, y sus pruebas necesitarían repositorios simulados. |
| **La ficha sigue a su cuenta, con la regla en `Usuario`** (elegida) | Una sola regla, en un solo lugar. `UsuarioTest` la prueba sin Spring ni base de datos. | Los estados hay que cambiarlos con estos métodos, nunca a mano. |

## Consecuencias

- Un médico suspendido no aparece en «Médicos activos» ni para reservar. Sigue en el panel de usuarios, con la cuenta SUSPENDIDO, y desde ahí se reactiva. El aviso del botón «Suspender» lo advierte.
- Las consultas ya programadas de quien se suspende o se da de baja no se cancelan solas. Es otra decisión, anotada en [ideas y debates](../ideas/ideas-y-debates.md).
- Las cuentas y fichas que ya estaban desincronizadas las corrige una sola vez la migración `V8__sincronizar-ficha-con-cuenta.sql`, con la misma regla. Solo apaga: desactiva la ficha cuya cuenta está suspendida o de baja, y pasa a BAJA la cuenta ACTIVO cuya ficha está desactivada, como hace ahora la baja desde la ficha. Nadie gana acceso; si hace falta, un administrador «Reincorpora» la cuenta y vuelven las dos. Una vez aplicada, la V8 no se edita nunca ([ADR 0004](0004-vaciar-v5-y-reparar-su-checksum.md)).
- Un paciente de mostrador dado de baja sigue sin forma de volver, porque no tiene cuenta que reactivar. Va con BUG-11.
