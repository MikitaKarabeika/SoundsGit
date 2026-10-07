package com.ape.friction.api.mok.services;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.entities.users.Client;
import com.ape.friction.api.entities.users.GoogleAccount;
import com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccessLevelRepository;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.GoogleAccountRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@MethodCallLogged
@TransactionLogged
@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager")
public class GoogleService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.oauth.token-url}")
    private String tokenUrl;

    @Value("${google.oauth.user-info-url}")
    private String userInfoUrl;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;


    // Po co autowired?
    @Autowired
    private final GoogleAccountRepository googleAccountRepository;

    @Autowired
    private final AccessLevelRepository accessLevelRepository;

    @Autowired
    private final AccountRepository accountRepository;

    @Autowired
    private final JwtService jwtService;

    @Transactional(readOnly = false, transactionManager = "mokTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public List<String> loginWithGoogleCode(String code) throws IOException {

        TokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                new GsonFactory(),
                tokenUrl,
                clientId,
                clientSecret,
                code,
                redirectUri)
                .execute();

        // dostaemy OAuth 2.0 Access Token
        String accessToken = response.getAccessToken();

        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        GenericUrl url = new GenericUrl(userInfoUrl);
        HttpRequest request = requestFactory.buildGetRequest(url);

        //  Access Token in header of  Authorization
        request.getHeaders().setAuthorization("Bearer " + accessToken);

        HttpResponse httpResponse = request.execute();
        String jsonPayload = httpResponse.parseAsString();

        Gson gson = new Gson();
        JsonObject userInfo = gson.fromJson(jsonPayload, JsonObject.class);

        if (userInfo.has("error")) {
            String errorMsg = userInfo.has("error_description")
                    ? userInfo.get("error_description").getAsString()
                    : userInfo.get("error").getAsString();
            System.err.println("Google UserInfo request failed: " + errorMsg);
            throw new TokenNotFoundException();
        }

        // get users data
        String email = userInfo.get("email").getAsString();
        String googleSubId = userInfo.get("sub").getAsString();
        String givenName = userInfo.has("given_name") ? userInfo.get("given_name").getAsString() : "";
        String familyName = userInfo.has("family_name") ? userInfo.get("family_name").getAsString() : "";
        
        Account targetAccount = googleAccountRepository.findByGoogleId(googleSubId)
                .map(GoogleAccount::getAccount)
                .orElseGet(() -> {
                    Account account = accountRepository.findByEmailIgnoreCase(email)
                            .orElseGet(() -> {
                                Account newAcc = new Account();
                                newAcc.setEmail(email);

                                String prefix = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");

                                int maxPrefixLength = Math.min(prefix.length(), 52);
                                String baseLogin = prefix.substring(0, maxPrefixLength) + "google";

                                String uniqueLogin = baseLogin;
                                int counter = 1;

                                while (accountRepository.existsByLogin(uniqueLogin)) {
                                    uniqueLogin = baseLogin + counter;
                                    counter++;
                                }



                                newAcc.setLogin(uniqueLogin);
                                newAcc.setName(givenName);
                                newAcc.setSurname(familyName);
                                newAcc.setPassword("impossible_password123!");
                                newAcc.setVerified(true);

                                Client client = Client.builder()
                                        .account(newAcc)
                                        .levelName("CLIENT")
                                        .active(true)
                                        .build();

                                newAcc.getAccessLevels().add(client);
                                Account saved = accountRepository.saveAndFlush(newAcc);
                                accessLevelRepository.save(client);

                                return saved;
                            });

                    GoogleAccount googleLink = new GoogleAccount();
                    googleLink.setAccount(account);
                    googleLink.setGoogleId(googleSubId);
                    googleAccountRepository.save(googleLink);

                    return account;
                });
        accountRepository.saveAndFlush(targetAccount);

        return jwtService.authenticate(targetAccount);
    }

}
