package com.cleotaginador.login.exception;

public class GlobalLoginException extends RuntimeException {
    private final int codigo;

    public GlobalLoginException(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
