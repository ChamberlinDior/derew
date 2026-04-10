package com.oviro.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends OviroException {
    public InsufficientBalanceException() {
        super("Solde wallet insuffisant pour effectuer cette opération",
                HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE");
    }
}
