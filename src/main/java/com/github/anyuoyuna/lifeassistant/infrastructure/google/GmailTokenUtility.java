package com.github.anyuoyuna.lifeassistant.infrastructure.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class GmailTokenUtility {

    // Файл, который ты скачала из Google Cloud Console (OAuth Client ID)
    private static final String CREDENTIALS_FILE_PATH = "/client_secret.json";

    // Нам нужно право изменять почту (чтобы удалять/помечать прочитанным)
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);

    public static void main(String[] args) throws Exception {
        // 1. Загружаем секреты клиента
        InputStream in = GmailTokenUtility.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            System.err.println("Файл client_secret.json не найден в src/main/resources!");
            return;
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), new InputStreamReader(in));

        // 2. Настраиваем поток авторизации
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                SCOPES)
                // Создает папку "tokens" в корне проекта для хранения учетных данных
//                .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                .setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
                .setAccessType("offline") // ОБЯЗАТЕЛЬНО для получения Refresh Token
                .setApprovalPrompt("force") // Принудительно запрашиваем разрешение
                .build();

        // 3. Открываем браузер на порту 8888 для подтверждения
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