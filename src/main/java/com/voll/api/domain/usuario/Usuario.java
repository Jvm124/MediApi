package com.voll.api.domain.usuario;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.paciente.Paciente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity(name = "Usuario")
@Table(name = "usuarios")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String correo;
    private String contrasenia;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @OneToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @OneToOne
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;

    public Usuario(String correo, String contrasenia, Rol rol) {
        this.correo = correo;
        this.contrasenia = contrasenia;
        this.rol = rol;
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void asignarMedico(Medico medico) {
        this.medico = medico;
    }

    public void asignarPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public void suspender() {
        if (this.estado != EstadoUsuario.ACTIVO) {
            throw new ValidacionException("Solo se puede suspender a un usuario activo");
        }
        this.estado = EstadoUsuario.SUSPENDIDO;
    }
    public void reactivar() {
        if (this.estado != EstadoUsuario.SUSPENDIDO && this.estado != EstadoUsuario.BAJA) {
            throw new ValidacionException("Solo se puede reactivar a un usuario suspendido o dado de baja");
        }
        this.estado = EstadoUsuario.ACTIVO;
    }
    public void darDeBaja() {
        if (this.estado != EstadoUsuario.ACTIVO && this.estado != EstadoUsuario.SUSPENDIDO) {
            throw new ValidacionException("Solo se puede dar de baja a un usuario activo o suspendido");
        }
        this.estado = EstadoUsuario.BAJA;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.rol.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return contrasenia;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {

        if (this.estado != EstadoUsuario.ACTIVO) {
            return false;
        }

        if (medico != null) {
            return Boolean.TRUE.equals(medico.getActivo());
        }
        if (paciente != null) {
            return Boolean.TRUE.equals(paciente.getActivo());
        }
        return true;
    }
}
