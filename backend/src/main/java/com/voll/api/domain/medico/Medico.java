package com.voll.api.domain.medico;

import com.voll.api.domain.direccion.Direccion;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

@Table(name = "medicos")
@Entity(name = "Medico")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Medico {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private Boolean activo;
        private String nombre;
        private String email;
        private String telefono;
        private String documento;

        @Enumerated(EnumType.STRING)
        private Especialidad especialidad;

        @Embedded
        private Direccion direccion;

        public Medico(DatosRegistroMedico datos) {
                this.nombre = datos.nombre();
                this.email = datos.email();
                this.activo = true;
                this.telefono = datos.telefono();
                this.documento = datos.documento();
                this.especialidad = datos.especialidad();
                this.direccion = new Direccion(datos.direccion());
        }

        public void actualizarInformacionesMedico(@Valid DatosActualizarMedico datos) {
                if (datos.nombre() != null) {
                        this.nombre = datos.nombre();
                }
                if (datos.telefono() != null) {
                        this.telefono = datos.telefono();
                }
                if (datos.especialidad() != null) {
                        this.especialidad = datos.especialidad();
                }
                if (datos.direccion() != null) {
                        this.direccion.actualizarDireccion(datos.direccion());
                }
        }

        public void actualizarPerfil(DatosActualizarPerfilMedico datos) {
                if (datos.telefono() != null) {
                        this.telefono = datos.telefono();
                }
                if (datos.direccion() != null) {
                        this.direccion.actualizarDireccion(datos.direccion());
                }
        }

        // Con la ficha activa, el médico aparece en las listas y se le puede reservar. La ficha sigue a la cuenta
        // del médico: solo está activa si la cuenta está ACTIVA (BUG-05). Por eso la cambia Usuario.
        public void activar() {
                this.activo = true;
        }

        public void desactivar() {
                this.activo = false;
        }
}
