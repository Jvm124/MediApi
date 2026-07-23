package com.voll.api.domain.medico;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHashTest {

    @Test
    void generarHash() {
        String hash = new BCryptPasswordEncoder().encode("admin123");
        System.out.println("Hash generado: " + hash);
    }
}