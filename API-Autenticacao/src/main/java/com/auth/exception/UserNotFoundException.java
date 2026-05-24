package com.auth.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String email) {
        super("Usuário não encontrado com o e-mail: " + email);
    }

}
