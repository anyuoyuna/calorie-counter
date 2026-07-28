package com.github.anyuoyuna.caloriecounter.infrastructure.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GoogleSheetsService {

    private final String spreadsheetId;
    private final Sheets sheetsService;

    public GoogleSheetsService(@Value("${google.sheets.id}") String spreadsheetId,
                               @Value("${google.credentials.path}") String credentialsPath) throws Exception {
        this.spreadsheetId = spreadsheetId;

        InputStream in = getClass().getResourceAsStream("/google-credentials.json");
        if (in == null) {
            throw new RuntimeException("Файл google-credentials.json не найден в resources!");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        this.sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CalorieCounterBot")
                .build();
    }

    /**
     * Метод для добавления строки в таблицу
     * @param sheetName Название листа (например "Операции")
     * @param row Данные строки в виде списка объектов
     */
    public void appendRow(String sheetName, List<Object> row) {
        try {
            ValueRange body = new ValueRange().setValues(Collections.singletonList(row));

            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, sheetName + "!A1", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            log.info("Строка успешно добавлена в Google Sheets: {}", row);
        } catch (IOException e) {
            log.error("Ошибка при записи в Google Sheets", e);
            throw new RuntimeException("Не удалось записать данные в таблицу", e);
        }
    }

    public List<List<Object>> readRows(String sheetName, String range) {
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, sheetName + "!" + range)
                    .execute();
            return response.getValues();
        } catch (IOException e) {
            log.error("Ошибка при чтении из Google Sheets", e);
            return Collections.emptyList();
        }
    }
}