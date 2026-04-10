package com.oviro.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "OVIRO Backend API",
        version = "1.0.0",
        description = """
            API REST professionnelle du système OVIRO – Mobilité & Logistique.
            
            ## Modules disponibles
            - **Authentification** : JWT Access + Refresh Token, RBAC
            - **Courses** : Cycle de vie complet, assignation, notation
            - **Wallet & Paiement** : Recharge Mobile Money, transactions
            - **QR Code Paiement** : Génération sécurisée, validation anti-duplication
            - **Chauffeur** : Géolocalisation, statut, SOS
            - **Administration** : Supervision globale, monitoring
            
            ## Authentification
            Utilisez le endpoint `/auth/login` pour obtenir un token Bearer.
            """,
        contact = @Contact(name = "Équipe OVIRO", email = "tech@oviro.com")
    ),
    servers = {
        @Server(url = "/api/v1", description = "Serveur principal")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenu via /auth/login"
)
public class OpenApiConfig {}
