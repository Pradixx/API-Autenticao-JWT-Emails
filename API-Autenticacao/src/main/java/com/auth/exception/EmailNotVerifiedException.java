package com.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("E-mail ainda não verificado. Verifique sua caixa de entrada e confirme seu cadastro.");
    }

}
