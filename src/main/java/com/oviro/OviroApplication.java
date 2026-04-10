package com.oviro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée principal de l'application OVIRO Backend.
 * Système de mobilité et logistique – Paiement QR Code.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OviroApplication {

    public static void main(String[] args) {
        SpringApplication.run(OviroApplication.class, args);
    }
}
