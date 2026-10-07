package com.ape.friction.api.mok.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ape.friction.api.common.JwsService;
import com.ape.friction.api.dto.data.ForgotPasswordDataDTO;
import com.ape.friction.api.dto.request.*;
import com.ape.friction.api.dto.response.RegisterResponseDTO;
import com.ape.friction.api.dto.response.login.*;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.LogFailedLogin;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.RequireSignature;
import com.ape.friction.api.mappers.AccountMapper;
import com.ape.friction.api.mok.services.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;


@RequiredArgsConstructor
@RestController
@MethodCallLogged
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AccountService accountService;

    private final AccountMapper accountMapper;

    private final PasswordResetService passwordResetService;

    private final GoogleService googleService;
    private final LoginCodeService loginCodeService;

    private final AuthService authService;

    private final JwsService jwsService;

    private final CaptchaService captchaService;



    //MOK.2
    @PostMapping("/login")
    @LogFailedLogin
    @Operation(
            summary = "Zaloguj się",
            description = "Uwierzytelnia użytkownika za pomocą loginu i hasła. " +
                    "Może zwrócić różne typy odpowiedzi w zależności od stanu konta: " +
                    "zwykłe tokeny, wymaganie zmiany hasła, wymaganie kodu 2FA."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logowanie powiodło się. W zależności od sytuacji zwracane są tokeny JWT, " +
                            "informacja o wymuszonej zmianie hasła lub konieczność podania kodu 2FA.",
                    content = @Content(schema = @Schema(oneOf = {
                            LoginSuccessResponseDTO.class,
                            LoginRequiresPasswordChange.class,
                            Login2FARequiredResponseDTO.class
                    }))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (brak loginu/hasła)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Nieprawidłowy login lub hasło"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Konto jest zablokowane lub nieaktywne"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO login) {

        captchaService.verify(login.captchaToken());

        LoginResponseDTO response = authService.login(login.login(), login.password());

        return switch (response) {
            case LoginSuccessResponseDTO success -> ResponseEntity.ok(success);
            case LoginRequiresPasswordChange requiresNew -> {
                String signature = jwsService.generate(requiresNew);
                yield  ResponseEntity.ok()
                        .header("X-Resource-Signature", signature)
                        .body(requiresNew);
            }
            case Login2FARequiredResponseDTO twoFa -> ResponseEntity.ok(twoFa);

            default -> throw new IllegalStateException("Unexpected response type: " + response.getClass());
        };

    }

    //MOK.2
    @PostMapping("/refresh")
    @Operation(
            summary = "Odśwież token JWT",
            description = "Wymienia ważny refresh token na nową parę tokenów (access + refresh)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokeny zostały odświeżone",
                    content = @Content(schema = @Schema(implementation = LoginSuccessResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (brak refresh tokenu)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token jest nieważny lub wygasł"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshRequest token) {
        List<String> response = authService.refresh(token.refreshToken());
        return ResponseEntity.ok(LoginSuccessResponseDTO.builder()
                .accessToken(response.get(0))
                .refreshToken(response.get(1))
                .build());
    }


    @PostMapping("/twofa")
    @LogFailedLogin
    @Operation(
            summary = "Zweryfikuj kod 2FA",
            description = "Kończy proces logowania chronionego dwuskładnikowo. " +
                    "Wymaga podania identyfikatora sesji (authSessionId) oraz 8‑cyfrowego kodu " +
                    "otrzymanego przez użytkownika (np. e‑mailem)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kod 2FA poprawny. Zwracane są tokeny lub żądanie zmiany hasła.",
                    content = @Content(schema = @Schema(oneOf = {
                            LoginSuccessResponseDTO.class,
                            LoginRequiresPasswordChange.class
                    }))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (brak sesji/kodu)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Nieprawidłowy kod 2FA lub sesja wygasła"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginResponseDTO> twofa(@Valid @RequestBody TwoFARequestDTO request) {

        LoginResponseDTO response = authService.check2fA(request.authSessionId(), request.token());

        return switch (response) {
            case LoginSuccessResponseDTO success -> ResponseEntity.ok(success);
            case LoginRequiresPasswordChange requiresNew -> {
                String signature = jwsService.generate(requiresNew);
                yield  ResponseEntity.ok()
                        .header("X-Resource-Signature", signature)
                        .body(requiresNew);
            }
            default -> throw new IllegalStateException("Unexpected response type: " + response.getClass());
        };
    }


    //MOK.1
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Zarejestruj nowe konto",
            description = "Tworzy nowe niezweryfikowane konto użytkownika. Na podany adres e-mail wysyłany jest link aktywacyjny."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Rejestracja zakończona sukcesem. Sprawdź skrzynkę e-mail.",
                    content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (np. słabe hasło, zajęty login/email)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Login lub adres e-mail jest już zajęty"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera (np. nieudana wysyłka maila)"
            )
    })
    public RegisterResponseDTO register(@Valid @RequestBody RegisterRequestDTO request,
                                        Locale locale) {
        captchaService.verify(request.captchaToken());
        Account account = accountMapper.toAccount(request);
        authService.register(account, locale);
        return new RegisterResponseDTO(
                "Rejestracja zakończona! Sprawdź skrzynkę e-mail, aby aktywować konto."
        );
    }

    // MOK.1
    @GetMapping("/verify")
    @Operation(
            summary = "Zweryfikuj konto (przez e-mail)",
            description = "Aktywuje konto użytkownika na podstawie tokenu z linku aktywacyjnego."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Konto zostało pomyślnie zweryfikowane"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Zły format JSON"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> verifyAccount(@RequestParam("token") String token) {
        authService.verifyAccount(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Wyślij link resetowania hasła",
            description = "Wysyła na podany adres e-mail link umożliwiający zresetowanie hasła."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Link resetujący został wysłany (jeśli konto istnieje)",
                    content = @Content(schema = @Schema(implementation = ForgotPasswordDataDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowy format adresu e-mail"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera "
            )
    })
    public ResponseEntity<ForgotPasswordDataDTO> forgotPassword(@Valid @RequestBody ForgotPasswordDataDTO request) {
        String response = passwordResetService.sendResetLink(request.email());
        return ResponseEntity.ok(ForgotPasswordDataDTO.builder()
                .email(response).build());
    }

    //MOK.3
    @PostMapping("/reset-password")
    @Operation(
            summary = "Zresetuj hasło",
            description = "Ustawia nowe hasło na podstawie ważnego tokenu resetującego."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hasło zostało zmienione"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane (słabe hasło) lub token nieważny/wygasł"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok().build();
    }

    //MOK.15 + MOK.16
    @PostMapping("/change-email")
    @Operation(
            summary = "Potwierdź zmianę adresu e-mail (nowy adres)",
            description = "Kończy proces zmiany adresu e-mail na podstawie tokenu weryfikacyjnego wysłanego na nowy adres. Wysyła maila z linkiem do zmiany maila spowrotem na stary email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Adres e-mail został zmieniony"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token jest nieprawidłowy lub wygasł"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> changeEmail(@Valid @RequestBody EditEmailRequestDTO request) {
        authService.finishingChangingEmail(request.token());
        return ResponseEntity.ok().build();

    }

    //MOK.15 + MOK.16
    @PostMapping("/change-email-to-old")
    @Operation(
            summary = "Przywróć stary adres e-mail",
            description = "Kończy proces przywracania poprzedniego adresu e-mail na podstawie tokenu weryfikacyjnego."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Przywrócono poprzedni adres e-mail"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token jest nieprawidłowy lub wygasł"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> changeEmailToOldOne(@Valid @RequestBody EditEmailRequestDTO request) {
        authService.finishingChangingEmailToOldOne(request.token());
        return ResponseEntity.ok().build();

    }


    @GetMapping("/google/callback")
    @Operation(
            summary = "Logowanie przez Google OAuth2",
            description = "Wymienia kod autoryzacyjny Google na tokeny JWT aplikacji."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logowanie przez Google powiodło się",
                    content = @Content(schema = @Schema(implementation = LoginSuccessResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Brak kodu autoryzacyjnego lub kod jest nieprawidłowy"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Nie udało się uwierzytelnić przez Google"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginResponseDTO> googleCallback(@RequestParam("code") String code) throws GeneralSecurityException, IOException {
        List<String> tokens = googleService.loginWithGoogleCode(code);

        return ResponseEntity.ok(LoginSuccessResponseDTO.builder()
                .accessToken(tokens.get(0))
                .refreshToken(tokens.get(1))
                .build());
    }

    @PostMapping("/activate")
    @Operation(
            summary = "Aktywuj konto ",
            description = "Ręcznie aktywuje konto użytkownika na podstawie tokenu z linka wysłanego na mail."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Konto zostało aktywowane"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token nieprawidłowy lub wygasł"
            ),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany – brak tokena JWT"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Zabroniony – brak uprawnień administracyjnych"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> activateAccount (@RequestParam("token") String token) {
        accountService.activateAccountWithToken(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forced-new-password")
    @RequireSignature(
            signable = "#request"
    )
    @Operation(
            summary = "Wymuszona zmiana hasła",
            description = "Ustawia nowe hasło w przypadku wymuszonej zmiany – np. po pierwszym logowaniu, " +
                    "resecie hasła lub nakazie administratora. Wymaga nagłówka X-Resource-Signature " +
                    "z podpisem weryfikującym integralność żądania."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hasło zostało zmienione"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane (słabe hasło) lub brak podpisu"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Podpis niezgodny (ID tampering) lub wersja niezgodna"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<Void> forcedNewPassword(@Valid @RequestBody SetNewPasswordRequestDTO request)
     {
        authService.setForcedPasswordChange(request.password(),request.id(),request.version());
        return ResponseEntity.ok()
                .build();
    }

    // SSBD : CODELOGIN
    @PostMapping("/login-with-e-mail")
    @Operation(
            summary = "Wyślij link do logowania przez e-mail",
            description = "Wysyła na podany adres e-mail jednorazowy kod umożliwiający logowanie bez hasła."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kod logowania wysłany (jeśli konto istnieje)",
                    content = @Content(schema = @Schema(implementation = LoginCodeResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowy format adresu e-mail"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera (np. problem z wysyłką maila)"
            )
    })
    public ResponseEntity<LoginCodeResponseDTO> loginWithEmail(@Valid @RequestBody ForgotPasswordDataDTO request) {
        LoginCodeResponseDTO response = loginCodeService.sendCodeLink(request.email());
        return ResponseEntity.ok(response);
    }

    // SSBD : CODELOGIN
    @PostMapping("/login-with-e-mail/codelogin")
    @LogFailedLogin
    @Operation(
            summary = "Zaloguj się kodem z e-maila",
            description = "Loguje użytkownika za pomocą jednorazowego kodu otrzymanego w wiadomości e-mail."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logowanie powiodło się – zwraca tokeny JWT",
                    content = @Content(schema = @Schema(implementation = LoginSuccessResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nieprawidłowe dane wejściowe (brak kodu/sesji) lub kod wygasł"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Nieprawidłowy kod lub sesja wygasła"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Konto jest niezweryfikowane lub zablokowane"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Błąd serwera"
            )
    })
    public ResponseEntity<LoginResponseDTO> loginWithLoginCode(@Valid @RequestBody LoginCodeDTO request) {
        List<String> tokens = loginCodeService.loginWithCode(request.code(), request.authSessionId());
        return ResponseEntity.ok(LoginSuccessResponseDTO.builder()
                .accessToken(tokens.get(0))
                .refreshToken(tokens.get(1))
                .build());
    }
}
