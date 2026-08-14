package com.github.anyuoyuna.lifeassistant.infrastructure.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
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
            System.err.println("Файл client_secret.json не найден в src/main/resources!");
            return;
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                SCOPES)
                .setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        System.out.println("Сейчас откроется браузер. Пожалуйста, авторизуйтесь...");
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");
        System.out.println("\n--- УСПЕХ! ---");
        System.out.println("Refresh Token: " + credential.getRefreshToken());
        System.out.println("Скопируй этот токен в application.properties");
        System.out.println("---------------\n");
        System.exit(0);
    }
}