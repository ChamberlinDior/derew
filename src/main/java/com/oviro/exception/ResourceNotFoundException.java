package com.oviro.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends OviroException {
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " introuvable avec l'identifiant: " + id,
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
