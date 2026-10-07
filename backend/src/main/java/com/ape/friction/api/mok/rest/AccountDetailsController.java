package com.ape.friction.api.mok.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ape.friction.api.dto.data.EditTimeZoneDataDTO;
import com.ape.friction.api.dto.response.LoginAuditDTO;
import com.ape.friction.api.dto.response.projection.LoginAuditProjection;
import com.ape.friction.api.dto.response.projection.TimezoneResponseProjection;
import com.ape.friction.api.filters.userPrincipal.UserPrincipal;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.mok.services.AccountDetailsService;

import java.security.Principal;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@MethodCallLogged
@RequestMapping(value = "/account-details", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Account Details", description = "Punkty końcowe związane z zarządzaniem detalami kont użytkowników")
@Validated
public class AccountDetailsController {

    private final AccountDetailsService accountDetailsService;

    @PatchMapping("/me/timezone")
    @PreAuthorize("hasAuthority('CHANGE_TZONE')")
    @Operation(
            summary = "Zaktualizuj własną strefę czasową",
            description = "Zapisuje nową strefę czasową dla zalogowanego użytkownika."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Strefa czasowa została zaktualizowana. Zwraca nową wartość.",
                    content = @Content(schema = @Schema(implementation = EditTimeZoneDataDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe – brak lub niepoprawna nazwa strefy czasowej"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia CHANGE_TZONE"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<EditTimeZoneDataDTO> updateMyTimeZone(Principal principal, @Valid @RequestBody EditTimeZoneDataDTO editTimeZoneDataDTO) {
        String newTimezone = accountDetailsService.updateMyTimeZone(principal.getName(), editTimeZoneDataDTO.timezone());


        return ResponseEntity.ok()
                .body(new EditTimeZoneDataDTO(newTimezone));
    }

    @GetMapping("/me/timezone")
    @PreAuthorize("hasAuthority('GET_TZONE')")
    @Operation(
            summary = "Pobierz własną strefę czasową",
            description = "Zwraca strefę czasową zapisaną dla zalogowanego użytkownika."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Strefa czasowa pobrana pomyślnie",
                    content = @Content(schema = @Schema(implementation = TimezoneResponseProjection.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia GET_TZONE"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<TimezoneResponseProjection> getMyTimeZone(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountDetailsService.getMyTimeZone(principal.getId()));
    }


    @GetMapping("/me/login-audit")
    @PreAuthorize("hasAuthority('GET_LOGIN_AUDIT')")
    @Operation(
            summary = "Pobierz historię logowań własnego konta",
            description = "Zwraca listę zdarzeń logowania (udanych i nieudanych) dla zalogowanego użytkownika."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Historia logowań pobrana pomyślnie",
                    content = @Content(schema = @Schema(implementation = LoginAuditDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia GET_LOGIN_AUDIT"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginAuditDTO> getMyLoginAudit(Principal principal) {
        String login = principal.getName();
        return ResponseEntity.ok(accountDetailsService.getMyLoginAudit(login));
    }


    @GetMapping("/{id}/login-audit")
    @PreAuthorize("hasAuthority('GET_ANY_LOGIN_AUDIT')")
    @Operation(
            summary = "Pobierz historię logowań dowolnego użytkownika",
            description = "Administrator pobiera listę zdarzeń logowania dla wskazanego konta."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Historia logowań pobrana pomyślnie",
                    content = @Content(schema = @Schema(implementation = LoginAuditDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe ID – niepoprawny format UUID"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia GET_ANY_LOGIN_AUDIT"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono użytkownika o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginAuditProjection> getUserLoginAudit(@Valid @PathVariable UUID id)
    {
        return ResponseEntity.ok(accountDetailsService.getAnyLoginAudit(id));
    }
}
