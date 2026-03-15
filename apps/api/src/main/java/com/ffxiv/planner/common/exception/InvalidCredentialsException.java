package com.ffxiv.planner.common.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }
}
