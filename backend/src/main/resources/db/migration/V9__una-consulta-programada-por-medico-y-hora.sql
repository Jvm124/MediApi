-- BUG-06: un médico no puede tener dos consultas programadas a la misma fecha y hora.
-- ValidadorMedicoConOtraConsultaEnElMismoHorario ya lo revisa, pero si dos reservas llegan a la vez, las dos pasan la
-- revisión antes de que alguna se guarde. Con esta regla, la base guarda la primera y rechaza la segunda.
-- medico_programado vale medico_id mientras la consulta está PROGRAMADA, y NULL cuando se cancela o se atiende.
-- Una regla UNIQUE no compara los NULL, así que una consulta cancelada o atendida deja libre su horario.
-- Si la base ya tuviera dos consultas programadas del mismo médico a la misma hora, esta migración falla y no cambia
-- nada. La consulta para encontrarlas está en docs/revisiones/0001-revision-inicial.md, en BUG-06.
ALTER TABLE consultas
    ADD COLUMN medico_programado BIGINT
        GENERATED ALWAYS AS (CASE WHEN estado = 'PROGRAMADA' THEN medico_id END) STORED,
    ADD CONSTRAINT uk_consultas_medico_fecha UNIQUE (medico_programado, fecha);
