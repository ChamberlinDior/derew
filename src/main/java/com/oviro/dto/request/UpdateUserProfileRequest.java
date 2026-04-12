package com.oviro.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserProfileRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100)
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Numéro de téléphone invalide")
    private String phoneNumber;

    @Past(message = "La date de naissance doit être dans le passé")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
}