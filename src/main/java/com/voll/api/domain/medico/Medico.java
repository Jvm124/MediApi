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

        public void actualizarInformaciones(@Valid DatosActualizarMedico datos) {
                if(datos.nombre() != null){
                        this.nombre = datos.nombre();
                }
                if(datos.telefono() != null){
                        this.telefono = datos.telefono();
                }
                if(datos.direccion() != null){
                        this.direccion.actualizarDireccion(datos.direccion());
                }


        }

        public void eliminacionLogica() {
                this.activo = false;
        }
}
