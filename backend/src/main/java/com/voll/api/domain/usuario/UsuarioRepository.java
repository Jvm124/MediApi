package com.voll.api.domain.usuario;

import com.voll.api.domain.paciente.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    UserDetails findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByPacienteId(Long idPaciente);

    Optional<Usuario> findByMedicoId(Long idMedico);
}
