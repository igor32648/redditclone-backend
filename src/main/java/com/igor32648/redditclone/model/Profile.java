package com.igor32648.redditclone.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;
    @NotBlank(message = "Profile name can´t be empty")
    private String profilename;
    @NotBlank(message = "Password can´t be empty")
    private String password;
    @Email
    @NotEmpty(message = "Email can´t be empty")
    private String email;
    private Instant createdInstant;
    private boolean enabled;
}
