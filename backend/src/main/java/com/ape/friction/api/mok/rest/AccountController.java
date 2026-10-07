package com.ape.friction.api.mok.rest;


import com.ape.friction.api.dto.data.PreferencesDataDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ape.friction.api.common.JwsService;
import com.ape.friction.api.dto.data.EditOwnAccountDataDTO;
import com.ape.friction.api.dto.data.EditUserAccountDataDataDTO;
import com.ape.friction.api.dto.request.*;
import com.ape.friction.api.dto.response.ChangeEmailResponseDTO;
import com.ape.friction.api.dto.response.EntityAuditDTO;
import com.ape.friction.api.dto.response.account.AccessLevelDTO;
import com.ape.friction.api.dto.response.account.AccountResponseDTO;
import com.ape.friction.api.dto.response.projection.AccountListProjection;
import com.ape.friction.api.entities.users.AccessLevel;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.filters.userPrincipal.UserPrincipal;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.RequireSignature;
import com.ape.friction.api.mappers.*;
import com.ape.friction.api.mok.services.AccountService;

import java.security.Principal;
import java.util.Collection;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@MethodCallLogged
@RequestMapping(value = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Accounts", description = "Punkty końcowe związane z zarządzaniem kontami użytkowników")
@Validated
public class AccountController {

    private final AccountService accountService;
    private final AccountListMapper accountListMapper;
    private final AccountMapper accountMapper;
    private final AccessLevelMapper accessLevelMapper;
    private final EditOwnAccountDataMapper editOwnAccountDataMapper;
    private final JwsService jwsService;
    private final EditUserAccountDataMapper editUserAccountDataMapper;


    //MOK.5
    @GetMapping
    @PreAuthorize("hasAuthority('MOK6_GET_ALL_ACCOUNTS')")
    @Operation(summary = "Wyświetl wszystkich użytkowników",
            description = "Zwraca listę wszystkich użytkowników")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie pobrano listę użytkowników",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountResponseDTO.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(responseCode = "403", description = "Zabroniony – brak wymaganych uprawnień"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<PagedModel<EntityModel<AccountListProjection>>> getAllAccounts(
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            PagedResourcesAssembler<AccountListProjection> assembler) {

        Page<AccountListProjection> accountsPage = accountService.getAllAccounts(pageable);

        PagedModel<EntityModel<AccountListProjection>> pagedModel = assembler.toModel(accountsPage);

        return ResponseEntity.ok(pagedModel);
    }

    //MOK.4
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('MOK4_GET_MY_ACCOUNT')")
    @Operation(summary = "Wyświetl własne konto",
            description = "Zwraca dane przypisane do własnego konta")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie pobrano dane własnego konta",
                    content = @Content(schema = @Schema(implementation = AccountResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(responseCode = "403", description = "Zabroniony – brak wymaganych uprawnień"),
            @ApiResponse(responseCode = "404", description = "Brak konta w bazie"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<AccountResponseDTO> getMyAccount(@AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getMyAccount(principal.getId());

        AccountResponseDTO accountDTO = accountListMapper.toDTO(account);

        String signature = jwsService.generate(accountDTO);
        return ResponseEntity.ok()
                .header("X-Resource-Signature", signature)
                .body(accountDTO);
    }

    @GetMapping("/me/audit")
    @PreAuthorize("hasAuthority('MOK4_GET_MY_ACCOUNT')")
    @Operation(summary = "Wyświetl audyt własnego konto",
            description = "Zwraca dane audytu przypisane do własnego konta")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie pobrano dane audytu własnego konta",
                    content = @Content(schema = @Schema(implementation = EntityAuditDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(responseCode = "403", description = "Zabroniony – brak wymaganych uprawnień"),
            @ApiResponse(responseCode = "404", description = "Brak konta w bazie"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<EntityAuditDTO> getMyAccountAudit(@AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getMyAccountWithAudit(principal.getId());

        EntityAuditDTO.EntityAuditDTOBuilder builder = EntityAuditDTO.builder()
                .createdAt(account.getCreatedAt())
                .modifiedAt(account.getModifiedAt())
                .lastOperationType(account.getLastOperationType());


        if (account.getCreatedBy() != null) {
            builder.createdBy(account.getCreatedBy().getLogin());
        }
        if (account.getLastModifiedBy() != null) {
            builder.lastModifiedBy(account.getLastModifiedBy().getLogin());
        }

        return ResponseEntity.ok().body(builder.build());
    }


    //MOK.5
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MOK5_GET_OTHER_ACCOUNT')")
    @Operation(summary = "Wyświetl dane konta",
            description = "Zwraca dane przypisane do konta")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie pobrano dane konta",
                    content = @Content(schema = @Schema(implementation = AccountResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(responseCode = "403", description = "Zabroniony – brak wymaganych uprawnień"),
            @ApiResponse(responseCode = "403", description = "Zabroniony – użytkownik próbuje wyświetlić swój profil jako profil innego użytkownika)"),
            @ApiResponse(responseCode = "404", description = "Brak konta w bazie"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<AccountResponseDTO> getOtherAccount(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getOtherAccount(id, principal.getId());
        AccountResponseDTO accountDTO = accountListMapper.toDTO(account);


        String signature = jwsService.generate(accountDTO);

        return ResponseEntity.ok()
                .header("X-Resource-Signature", signature)
                .body(accountDTO);
    }

    @GetMapping("/audit/{id}")
    @PreAuthorize("hasAuthority('MOK5_GET_OTHER_ACCOUNT')")
    @Operation(summary = "Wyświetl audyt konta",
            description = "Zwraca dane audytu przypisane do konta")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie pobrano dane audytu konta",
                    content = @Content(schema = @Schema(implementation = EntityAuditDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(responseCode = "403", description = "Zabroniony – brak wymaganych uprawnień"),
            @ApiResponse(responseCode = "404", description = "Brak konta w bazie"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<EntityAuditDTO> getOtherAccountAudit(@PathVariable UUID id) {
        Account account = accountService.getOtherAccountWithAudit(id);

        EntityAuditDTO.EntityAuditDTOBuilder builder = EntityAuditDTO.builder()
                .createdAt(account.getCreatedAt())
                .modifiedAt(account.getModifiedAt())
                .lastOperationType(account.getLastOperationType());

        if (account.getCreatedBy() != null) {
            builder.createdBy(account.getCreatedBy().getLogin());
        }
        if (account.getLastModifiedBy() != null) {
            builder.lastModifiedBy(account.getLastModifiedBy().getLogin());
        }

        return ResponseEntity.ok().body(builder.build());
    }


    //MOK.18
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('MOK18_UPDATE_OTHER_ACCOUNT')")
    @RequireSignature(
            id = "#id",
            signable = "#editUserAccountRequestDTO"
    )
    @Operation(summary = "Zaktualizuj personalne dane konta",
            description = "Aktualizuje dane przypisane do konta")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie zaktualizowano dane personalne konta",
                    content = @Content(schema = @Schema(implementation = EditUserAccountDataDataDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – możliwe przyczyny: brak wymaganych uprawnień (MOK18_UPDATE_OTHER_ACCOUNT) lub próba manipulacji ID (ID tampering)"),
            @ApiResponse(responseCode = "404", description = "Brak konta w bazie"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt wersji – konto zostało zmodyfikowane równocześnie przez innego użytkownika. Pobierz aktualną wersję i spróbuj ponownie."),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<EditUserAccountDataDataDTO> updateOtherAccountData(@PathVariable UUID id, @Valid @RequestBody EditUserAccountDataDataDTO editUserAccountRequestDTO) {
        EditUserAccountDataDataDTO updated = editUserAccountDataMapper.toDto(accountService.updateOtherAccount(id, editUserAccountDataMapper.toAccount(editUserAccountRequestDTO)));
        String signature = jwsService.generate(updated);

        return ResponseEntity.ok()
                .header("X-Resource-Signature", signature)
                .body(updated);
    }

    // MOK.15
    @PatchMapping("/me/change-email")
    @PreAuthorize("hasAuthority('MOK15_CHANGE_OWN_EMAIL')")
    @RequireSignature(
            signable = "#changeEmailDTO"
    )
    @Operation(summary = "Zaktualizuj e-mail przypisany do własnego konta",
            description = "Inicjuje proces zmiany adresu e-mail zalogowanego użytkownika. " +
                    "Wysyła e‑mail weryfikacyjny na nowy adres. Operacja wymaga zgodności wersji konta oraz " +
                    "prawidłowego podpisu (X-Resource-Signature)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Proces zmiany adresu e-mail został pomyślnie rozpoczęty. ",
                    content = @Content(schema = @Schema(implementation = ChangeEmailResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (np. niepoprawny format e‑maila, niezgodność bieżącego e‑maila)"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak wymaganych uprawnień (MOK15_CHANGE_OWN_EMAIL) lub próba zmiany adresu e‑mail innego użytkownika (ID tampering)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono konta o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt wersji – konto zostało zmodyfikowane równocześnie przez inny proces. " +
                            "Pobierz aktualną wersję i spróbuj ponownie."
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera (m.in. nieudana wysyłka e‑maila weryfikacyjnego)"
            )
    })
    public ChangeEmailResponseDTO requestOwnEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangeEmailRequestDTO changeEmailDTO) {
        if (!principal.getId().equals(changeEmailDTO.id())) {
            throw new AccessDeniedException("You are allowed to change only your own email");
        }
        return accountService.changeEmail(changeEmailDTO.id(), changeEmailDTO.currentEmail(), changeEmailDTO.newEmail());

    }

    //MOK.16
    @PatchMapping("{id}/change-email-other-acc")
    @PreAuthorize("hasAuthority('MOK16_CHANGE_OTHER_EMAIL')")
    @RequireSignature(
            id = "#id",
            signable = "#changeEmailDTO"
    )
    @Operation(summary = "Zaktualizuj e-mail przypisany do konta",
            description = "Inicjuje proces zmiany adresu e-mail użytkownika. " +
                    "Wysyła e‑mail weryfikacyjny na nowy adres. Operacja wymaga zgodności wersji konta oraz " +
                    "prawidłowego podpisu (X-Resource-Signature)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Proces zmiany adresu e-mail został pomyślnie rozpoczęty. ",
                    content = @Content(schema = @Schema(implementation = ChangeEmailResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (np. niepoprawny format e‑maila, niezgodność bieżącego e‑maila)"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak wymaganych uprawnień (MOK16_CHANGE_OTHER_EMAIL) lub próba zmiany adresu e‑mail innego użytkownika (ID tampering)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono konta o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt wersji – konto zostało zmodyfikowane równocześnie przez inny proces. " +
                            "Pobierz aktualną wersję i spróbuj ponownie."
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera (m.in. nieudana wysyłka e‑maila weryfikacyjnego)"
            )
    })
    public ChangeEmailResponseDTO editEmailOfOtherAccount(@PathVariable UUID id, @Valid @RequestBody ChangeEmailRequestDTO changeEmailDTO) {

        return accountService.changeEmail(id, changeEmailDTO.currentEmail(), changeEmailDTO.newEmail());

    }

    //MOK.17
    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('MOK17_UPDATE_OWN_ACCOUNT')")
    @RequireSignature(
            signable = "#editOwnAccountDataDTO"
    )
    @Operation(summary = "Zaktualizuj personalne dane własnego konta",
            description = "Aktualizuje dane przypisane do własnego konta")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pomyślnie zaktualizowano dane personalne własnego konta",
                    content = @Content(schema = @Schema(implementation = EditUserAccountDataDataDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – możliwe przyczyny: brak wymaganych uprawnień (MOK17_UPDATE_OWN_ACCOUNT) lub próba manipulacji ID (ID tampering)"),
            @ApiResponse(responseCode = "404", description = "Brak konta w bazie"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt wersji – konto zostało zmodyfikowane równocześnie przez innego użytkownika. Pobierz aktualną wersję i spróbuj ponownie."),
            @ApiResponse(responseCode = "500", description = "Błąd serwera ")
    })
    public ResponseEntity<EditOwnAccountDataDTO> updateMyAccountData(Principal principal, @Valid @RequestBody EditOwnAccountDataDTO editOwnAccountDataDTO) {


        Account updatedAccount = accountService.updateOwnAccount(principal.getName(), editOwnAccountDataMapper.toAccount(editOwnAccountDataDTO));

        EditOwnAccountDataDTO updated = editOwnAccountDataMapper.toDto(updatedAccount);
        String signature = jwsService.generate(updated);

        return ResponseEntity.ok()
                .header("X-Resource-Signature", signature)
                .body(updated);
    }

    //MOK.9
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('MOK9_DEACTIVATE_ACCOUNT')")
    @Operation(
            summary = "Dezaktywuj konto użytkownika",
            description = "Administrator dezaktywuje konto innego użytkownika na podstawie jego ID. " +
                    "Zalogowany administrator nie może dezaktywować własnego konta."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Konto zostało pomyślnie dezaktywowane"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak wymaganych uprawnień (MOK9_DEACTIVATE_ACCOUNT) lub próba dezaktywacji własnego konta"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono konta o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt – nie można dezaktywować użytkownika (np. ma aktywne zamówienia)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> deactivateAccount(@PathVariable UUID id, Principal principal) {
        accountService.deactivateAccountByAdmin(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    // MOK.11
    @PutMapping("/{login}/access-levels")
    @PreAuthorize("hasAuthority('MOK11_GRANT_ACCESS_LEVEL')")
    @Operation(
            summary = "Nadaj poziom dostępu użytkownikowi",
            description = "Administrator nadaje (lub przywraca już istniejący) poziom dostępu wskazanemu użytkownikowi. " +
                    "Wymagane jest podanie nazwy poziomu w ciele żądania."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Poziom dostępu został pomyślnie nadany (lub przywrócony). Zwraca aktualny stan poziomu dostępu.",
                    content = @Content(schema = @Schema(implementation = AccessLevelDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (np. brak loginu, niepoprawna nazwa poziomu)"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak wymaganych uprawnień (MOK11_GRANT_ACCESS_LEVEL)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono użytkownika o podanym loginie lub nie znaleziono poziomu dostępu"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<AccessLevelDTO> grantAccessLevel(
            @NotBlank @PathVariable("login") String login,
            @Valid @RequestBody AccessLevelDTO requestDTO) {

        String requestedLevelName = requestDTO.getLevelName().toUpperCase();


        AccessLevel accessLevelToGrant = accessLevelMapper.toAccessLevel(requestDTO);
        AccessLevel savedAccessLevel = accountService.addExistingOrGrantAccessLevel(login, accessLevelToGrant, requestedLevelName);

        AccessLevelDTO responseDTO = accessLevelMapper.toAccessLevelDTO(savedAccessLevel);
        return ResponseEntity.ok(responseDTO);

    }

    // MOK.12
    @DeleteMapping("/{login}/access-levels/{levelname}")
    @PreAuthorize("hasAuthority('MOK12_REVOKE_ACCESS_LEVEL')")
    @Operation(
            summary = "Odbierz poziom dostępu użytkownikowi",
            description = "Administrator obiera poziom dostępu wskazanemu użytkownikowi. " +
                    "Wymagane jest podanie nazwy poziomu w ciele żądania."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Poziom dostępu został pomyślnie odebrany."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (np. brak loginu, niepoprawna nazwa poziomu)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Administrator próbuje odebrać sobie samemu poziom dostępu Administrator"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak wymaganych uprawnień (MOK12_REVOKE_ACCESS_LEVEL)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono użytkownika o podanym loginie lub nie znaleziono poziomu dostępu"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> RevokeAccessLevel(
            @NotBlank @PathVariable("login") String login,
            @NotBlank @PathVariable("levelname") String levelName) {
        accountService.revokeAccessLevel(login, levelName);
        return ResponseEntity.noContent().build();
    }

    // Przełącz poziom dostępu
    @PostMapping("/change-view-access-level")
    @PreAuthorize("hasAuthority('CHANGE_VIEW_ACCESS_LEVEL')")
    @Operation(
            summary = "Zarejestruj zmianę aktywnego widoku",
            description = "Rejestruje fakt przełączenia przez zalogowanego użytkownika aktywnego widoku (perspektywy poziomu dostępu). " +
                    "Endpoint nie modyfikuje uprawnień – zapisuje wyłącznie zdarzenie do audytu."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zmiana widoku została pomyślnie zarejestrowana"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe – brak lub niepoprawna nazwa poziomu widoku"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – użytkownik nie posiada uprawnienia CHANGE_VIEW_ACCESS_LEVEL"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera – niepowodzenie mechanizmu logowania"
            )
    })
    public ResponseEntity<Void> changeViewAccessLevel(
            @Valid @RequestBody ViewChangeAccessLevelRequestDTO requestDTO) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Collection<? extends GrantedAuthority> authorities =
                authentication.getAuthorities();

        String login = authentication.getName();

        String requestedLevelName = requestDTO.chosenViewLevelName().toUpperCase();

        accountService.changeViewAccessLevel(authorities, requestedLevelName, login);
        return ResponseEntity.ok().build();

    }

    //MOK.10
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('MOK10_ACTIVATE_ACCOUNT')")
    @Operation(
            summary = "Aktywuj konto użytkownika",
            description = "Administrator aktywuje konto innego użytkownika na podstawie jego ID. " +
                    "Zalogowany administrator nie może aktywować własnego konta."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Konto zostało pomyślnie aktywowane"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak wymaganych uprawnień (MOK10_ACTIVATE_ACCOUNT)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono konta o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> activateAccount(@PathVariable UUID id) {
        accountService.activateAccountByAdmin(id);
        return ResponseEntity.ok().build();
    }

    //MOK.7
    @PostMapping("/add-user")
    @PreAuthorize("hasAuthority('MOK7_ADD_USER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Dodaj nowego użytkownika",
            description = "Administrator tworzy nowe konto użytkownika. " +
                    "Nowe konto otrzymuje status niezweryfikowanego – użytkownik musi potwierdzić adres e-mail."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Konto zostało pomyślnie utworzone. Zwraca dane nowego użytkownika.",
                    content = @Content(schema = @Schema(implementation = AccountResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (np. brak wymaganych pól, niepoprawny format e-maila, za słabe hasło)"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – użytkownik nie posiada uprawnienia MOK7_ADD_USER"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt – login lub adres e-mail już istnieje w systemie"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera (np. niepowodzenie wysyłki maila weryfikacyjnego)"
            )
    })
    public AccountResponseDTO addUser(@Valid @RequestBody AddAccountRequestDTO request) {
        Account account = accountMapper.toAccount(request);
        Account saved = accountService.addUser(account);
        return accountListMapper.toDTO(saved);
    }

    //MOK.8
    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('MOK8_DELETE_ACCOUNT')")
    @Operation(
            summary = "Usuń konto użytkownika",
            description = "Administrator usuwa konto wskazanego użytkownika. " +
                    "Nie można usunąć własnego konta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Konto zostało pomyślnie usunięte"),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia MOK8_DELETE_ACCOUNT lub próba usunięcia własnego konta"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono konta o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Konflikt – konto nie może być usunięte (np. posiada aktywne zamówienia)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id, Principal principal) {
        accountService.deleteAccount(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // MOK.14
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAuthority('MOK14_CHANGE_USER_PASSWORD')")
    @Operation(
            summary = "Zmień hasło użytkownika",
            description = "Administrator zmienia hasło wskazanego użytkownika bez znajomości starego hasła."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hasło zostało pomyślnie zmienione"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe – nowe hasło nie spełnia wymogów bezpieczeństwa"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia MOK14_CHANGE_USER_PASSWORD"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – użytkownik próbuje zmienić swoje hasło (tutaj może zmienić tylko hasło innego użytkownika)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Nie znaleziono konta o podanym ID"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> changeUserPassword(@PathVariable UUID id, @Valid @RequestBody ChangeUserPasswordRequestDTO dto, @AuthenticationPrincipal UserPrincipal principal) {
        accountService.changeUserPassword(id, principal.getId(), dto.newPassword());
        return ResponseEntity.ok().build();
    }

    // MOK.13
    @PatchMapping("/me/change-password")
    @PreAuthorize("hasAuthority('MOK13_CHANGE_OWN_PASSWORD')")
    @Operation(
            summary = "Zmień własne hasło",
            description = "Zalogowany użytkownik zmienia swoje hasło. Wymagane jest podanie starego hasła."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hasło zostało pomyślnie zmienione"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe – stare hasło niepoprawne lub nowe nie spełnia wymogów"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia MOK13_CHANGE_OWN_PASSWORD"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> changeOwnPassword(
            Principal principal,
            @Valid @RequestBody ChangeOwnPasswordRequestDTO changeOwnPasswordRequestDTO) {

        accountService.changeOwnPassword(
                principal.getName(),
                changeOwnPasswordRequestDTO.oldPassword(),
                changeOwnPasswordRequestDTO.newPassword()
        );

        return ResponseEntity.ok().build();
    }

    // S2_KODYJEDN — user włącza/wyłącza 2FA na własnym koncie
    @PatchMapping("/me/two-factor-auth")
    @PreAuthorize("hasAuthority('SET_OWN_2FA')")
    @Operation(
            summary = "Włącz lub wyłącz uwierzytelnianie dwuskładnikowe (2FA)",
            description = "Zalogowany użytkownik zmienia stan 2FA dla swojego konta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stan 2FA został pomyślnie zaktualizowany"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe – brak lub niepoprawna wartość pola 'enabled'"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia SET_OWN_2FA"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> setOwnTwoFa(
            Principal principal,
            @Valid @RequestBody TwoFaToggleRequestDTO body) {
        accountService.setOwnTwoFa(principal.getName(), body.enabled());
        return ResponseEntity.ok().build();
    }


    //preferencje na 67% do refactoringu (nowa tabela??)

    @PatchMapping("/me/preferences")
    @PreAuthorize("hasAuthority('MOK17_UPDATE_OWN_ACCOUNT')")
    @Operation(
            summary = "Zaktualizuj własne preferencje",
            description = "Zalogowany użytkownik zapisuje swoje preferencje (język, motyw)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferencje zostały zaktualizowane. Zwraca aktualny stan preferencji.",
                    content = @Content(schema = @Schema(implementation = PreferencesDataDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia MOK17_UPDATE_OWN_ACCOUNT"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<PreferencesDataDTO> changeOwnPreferences(Principal principal, @RequestBody PreferencesDataDTO dto) {
        Account ownAccount = accountMapper.toAccount(dto);
        ownAccount.setLogin(principal.getName());
        Account account = accountService.updatePreferencesOfOwnAccount(principal.getName(), ownAccount);


        PreferencesDataDTO response = PreferencesDataDTO.builder()
                .language(account.getLanguage())
                .theme(account.getTheme())
                .build();
        return ResponseEntity.ok()
                .body(response);
    }

    @GetMapping("/me/preferences")
    @PreAuthorize("hasAuthority('MOK4_GET_MY_ACCOUNT')")
    @Operation(
            summary = "Pobierz własne preferencje",
            description = "Zwraca preferencje (język, motyw) zalogowanego użytkownika."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferencje pobrane pomyślnie",
                    content = @Content(schema = @Schema(implementation = PreferencesDataDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnienia MOK4_GET_MY_ACCOUNT"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<PreferencesDataDTO> getOwnPreferences(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().body(accountService.getMyPreferences(principal.getId()));
    }

}
