package com.voll.api.domain.paciente;

import com.voll.api.domain.direccion.DatosDireccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DatosRegistroPaciente(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "9\\d{8}") String telefono,
        @NotBlank @Pattern(regexp = "\\d{8}") String documento,
        @NotNull @Valid DatosDireccion direccion
) {
}
