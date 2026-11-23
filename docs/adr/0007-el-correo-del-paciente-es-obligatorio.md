# 0007 — El correo del paciente es obligatorio, también en el alta por mostrador

- **Fecha:** 2026-11-23
- **Estado:** aceptada

## Contexto

Un paciente se puede registrar de dos maneras:

- **El registro web** (`DatosRegistroPaciente`): el paciente crea su cuenta, y el correo es obligatorio (`@NotBlank @Email`).
- **El alta por mostrador** (`DatosRegistroPacientePorStaff`): recepción o un administrador registran al paciente, sin cuenta web. El correo solo llevaba `@Email`, que deja pasar un correo nulo o vacío.

Pero la tabla `pacientes` exige el correo desde la migración V2 (`email VARCHAR(100) NOT NULL UNIQUE`). Al terminar BUG-04 de la [revisión inicial](../revisiones/0001-revision-inicial.md) quedó pendiente qué hacer, porque con el código anterior:

- Un alta sin correo pasaba la validación y llegaba hasta la base, que la rechazaba al guardar. El backend respondía 409 con «Ya existe un registro con esos datos (por ejemplo, el mismo correo o documento).», aunque no había nada repetido.
- Un alta con el correo vacío (`""`) sí se guardaba, porque un texto vacío no es NULL. Desde entonces, ningún otro paciente podía darse de alta con el correo vacío: «Ya existe un paciente con ese email».

El formulario del panel de recepción ya pedía el correo siempre, así que esto solo pasaba al llamar a la API directamente, por ejemplo desde Swagger.

## Decisión

**El correo es obligatorio en el alta por mostrador, como en el registro web y en la tabla.**

- `DatosRegistroPacientePorStaff.email` lleva `@NotBlank @Email`, igual que `DatosRegistroPaciente`.
- Sin correo, o con uno vacío o en blanco, `POST /pacientes/registro-staff` responde 400 con el error en el campo `email`, y el alta no llega a `PacienteService`.
- `PacienteService.registrarPorStaff` revisa siempre si el correo ya está registrado. Antes se saltaba esa revisión cuando el correo venía nulo.
- La tabla no cambia, así que no hace falta ninguna migración.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Una migración V10 que vuelva opcional el correo en `pacientes` | Se podría registrar a quien no tiene correo, por ejemplo a una persona mayor. MySQL deja guardar varios NULL en una columna UNIQUE, así que el correo seguiría siendo único para quien lo tiene. | Cambia la tabla y el modelo. Habría que revisar todo el código que usa el correo del paciente, por si llega vacío, y cambiar también el formulario del panel. |
| Dejarlo como estaba | No hay que tocar nada. | El backend y la base dicen cosas distintas: un alta sin correo falla con un mensaje que no explica el motivo, y un correo vacío se guarda. |
| **Exigir el correo en el alta por mostrador** (elegida) | Una sola regla, la misma en la tabla, en los dos registros y en el formulario. No hace falta migración. `PacienteControllerTest` la prueba con la seguridad real. | Quien no tiene correo no se puede registrar en el mostrador. |

## Consecuencias

- Recepción le pide un correo a cada paciente nuevo. El formulario del panel ya lo pedía, así que en la pantalla no cambia nada.
- Si un día hace falta registrar pacientes sin correo, se hará con una migración nueva y un ADR que reemplace a este.
