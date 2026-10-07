package com.ape.friction.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import com.ape.friction.api.dto.response.account.AccessLevelDTO;

import java.util.List;

@Getter
@Setter
public class AddAccountRequestDTO {

    @NotBlank
    @Size(min = 1, max = 32)
    private String name;

    @NotBlank
    @Size(min = 1, max = 32)
    private String surname;

    @NotBlank
    @Email
    @Size(max = 128)
    private String email;

    @NotBlank
    @Size(min = 3, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String login;

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,64}$"
    )
    @ToString.Exclude
    private String password;

    @NotEmpty
    @Valid
    private List<AccessLevelDTO> accessLevels;

    // Walidacja klasowa: login nie może być równy żadnemu z pól biznesowych.
    // Metoda MUSI być public — Hibernate Validator waliduje @AssertTrue tylko na getterach.
    @AssertTrue(message = "Login cannot be equal to name, surname, email or password")
    public boolean isLoginUniqueAmongFields() {
        if (login == null) {
            return true;
        }
        return !login.equalsIgnoreCase(name)
                && !login.equalsIgnoreCase(surname)
                && !login.equalsIgnoreCase(email)
                && !login.equalsIgnoreCase(password);
    }

}
