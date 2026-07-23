package com.voll.api.domain.medico;

import com.voll.api.domain.direccion.DatosDireccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DatosRegistroMedico(

        @NotBlank String nombre,

        @NotBlank @Email String email,

        @NotBlank @Pattern(regexp = "9\\d{8}") String telefono,

        @NotBlank @Pattern(regexp = "\\d{8}") String documento,

        @NotNull Especialidad especialidad,

        @NotNull @Valid DatosDireccion direccion,

        @NotBlank String contrasenia
) {

}
