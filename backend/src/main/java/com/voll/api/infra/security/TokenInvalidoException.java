package com.voll.api.infra.security;

/**
 * El token JWT está vencido, alterado o mal formado.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
