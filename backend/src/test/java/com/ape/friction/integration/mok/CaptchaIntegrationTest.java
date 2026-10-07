//package com.ape.friction.integration.mok;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.client.RestTestClient;
//import   com.ape.friction.exceptions.CaptchaVerificationException;
//import   com.ape.friction.integration.config.MOKIntegrationTestBase;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.isNull;
//
//public class CaptchaIntegrationTest extends MOKIntegrationTestBase {
//
//    /*private static final HttpServer captchaServer;
//    private static final AtomicReference<String> captchaResponse = new AtomicReference<>("{\"success\":true}");
//
//    static {
//        try {
//            captchaServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
//            captchaServer.createContext("/siteverify", exchange -> {
//                exchange.getRequestBody().readAllBytes();
//                byte[] resp = captchaResponse.get().getBytes(StandardCharsets.UTF_8);
//                exchange.getResponseHeaders().add("Content-Type", "application/json");
//                exchange.sendResponseHeaders(200, resp.length);
//                exchange.getResponseBody().write(resp);
//                exchange.close();
//            });
//            captchaServer.start();
//        } catch (Exception e) {
//            throw new IllegalStateException("Cannot start fake captcha server", e);
//        }
//    }
//
//    @AfterAll
//    static void stopCaptchaServer() {
//        captchaServer.stop(0);
//    }
//
//    @DynamicPropertySource
//    static void captchaProperties(DynamicPropertyRegistry registry) {
//        registry.add("app.captcha.enabled", () -> "true");
//        registry.add("app.captcha.verify-url",
//                () -> "http://127.0.0.1:" + captchaServer.getAddress().getPort() + "/siteverify");
//        registry.add("app.captcha.secret", () -> "integration-test-secret");
//    }*/
//
//    @Autowired
//    private RestTestClient restTestClient;
///*
//    @BeforeEach
//    void resetCaptchaProvider() {
//        captchaResponse.set("{\"success\":true}");
//    }*/
//
//    private Map<String, Object> registerBody(String email, String login, String captchaToken) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("name", "Jan");
//        body.put("surname", "Kowalski");
//        body.put("email", email);
//        body.put("login", login);
//        body.put("password", "Password1!");
//        if (captchaToken != null) {
//            body.put("captchaToken", captchaToken);
//        }
//        return body;
//    }
//
//    private Map<String, Object> loginBody(String login, String password, String captchaToken) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("login", login);
//        body.put("password", password);
//        if (captchaToken != null) {
//            body.put("captchaToken", captchaToken);
//        }
//        return body;
//    }
//
//    @Test
//    void registerShouldSucceedWhenCaptchaValid() {
//        doNothing().when(captchaService).verify(anyString());
//
//        restTestClient.post()
//                .uri("/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(registerBody("captcha-ok@example.com", "captchaokuser", "good-token"))
//                .exchange()
//                .expectStatus().isCreated();
//    }
//
//    @Test
//    void registerShouldFailWhenCaptchaRejected() {
//        doThrow(new CaptchaVerificationException()).when(captchaService).verify(anyString());
//
//        restTestClient.post()
//                .uri("/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(registerBody("captcha-bad@example.com", "captchabaduser", "bad-token"))
//                .exchange()
//                .expectStatus().isBadRequest();
//    }
//
//    @Test
//    void registerShouldFailWhenCaptchaTokenMissing() {
//        doThrow(new CaptchaVerificationException()).when(captchaService).verify(isNull());
//        restTestClient.post()
//                .uri("/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(registerBody("captcha-missing@example.com", "captchamissinguser", null))
//                .exchange()
//                .expectStatus().isBadRequest();
//    }
//
//    @Test
//    void loginShouldSucceedWhenCaptchaValid() {
//        doNothing().when(captchaService).verify(anyString());
//
//        restTestClient.post()
//                .uri("/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(loginBody("admin1", "password", "good-token"))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody()
//                .jsonPath("$.accessToken").exists()
//                .jsonPath("$.refreshToken").exists();
//    }
//
//    @Test
//    void loginShouldFailWhenCaptchaRejected() {
//        doThrow(new CaptchaVerificationException()).when(captchaService).verify(anyString());
//
//        restTestClient.post()
//                .uri("/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(loginBody("admin1", "password", "bad-token"))
//                .exchange()
//                .expectStatus().isBadRequest();
//    }
//
//    @Test
//    void loginShouldFailWhenCaptchaTokenMissing() {
//        doThrow(new CaptchaVerificationException()).when(captchaService).verify(isNull());
//        restTestClient.post()
//                .uri("/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(loginBody("admin1", "password", null))
//                .exchange()
//                .expectStatus().isBadRequest();
//    }
//}
