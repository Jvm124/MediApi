package com.voll.api.domain.usuario;

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

    private Boolean activo;

    public Usuario(String correo, String contrasenia, Rol rol) {
        this.correo = correo;
        this.contrasenia = contrasenia;
        this.rol = rol;
        this.activo = true;
    }

    public void asignarMedico(Medico medico) {
        this.medico = medico;
    }

    public void asignarPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public void desactivar() {
        this.activo = false;
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

        if (Boolean.FALSE.equals(this.activo)) {
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
