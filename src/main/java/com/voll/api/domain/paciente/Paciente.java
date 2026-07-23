package com.voll.api.domain.paciente;

import com.voll.api.domain.direccion.Direccion;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "pacientes")
@Entity(name = "Paciente")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private Boolean activo;
    private String email;
    private String telefono;
    private String documento;

    @Embedded
    private Direccion direccion;

    public Paciente(DatosRegistroPaciente datos) {
        this.nombre = datos.nombre();
        this.activo = true;
        this.email = datos.email();
        this.telefono = datos.telefono();
        this.documento = datos.documento();
        this.direccion = new Direccion(datos.direccion());

    }

    public void actualizarInformacionesPaciente(@Valid DatosActualizarPaciente datos) {
        this.nombre = datos.nombre();
        this.telefono = datos.telefono();
        this.direccion.actualizarDireccion(datos.direccion());
    }

    public void eliminacionLogicaPaciente() {
        this.activo = false;
    }
}
