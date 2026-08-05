package com.github.anyuoyuna.lifeassistant.infrastructure.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class GmailTokenUtility {

    private static final String CREDENTIALS_FILE_PATH = "/client_secret.json";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);

    public static void main(String[] args) throws Exception {
        InputStream in = GmailTokenUtility.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            System.out.println("Файл client_secret.json не найден в resources!");
            return;
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = flow.loadCredential("user");

        if (credential == null) {
            credential = new AuthorizationCodeInstalledApp(flow, receiver)
                    .authorize("user");
        }

        System.out.println("--- УСПЕХ ---");
        System.out.println("Access Token: " + credential.getAccessToken());
        System.out.println("Refresh Token: " + credential.getRefreshToken());
        System.out.println("Скопируй Refresh Token в свой application.properties");
        System.out.println("-------------");

        System.exit(0);
    }
}