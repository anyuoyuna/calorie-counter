package com.github.anyuoyuna.lifeassistant.infrastructure.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class GmailService {

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;

    public GmailService(@Value("${google.gmail.client-id}") String clientId,
                        @Value("${google.gmail.client-secret}") String clientSecret,
                        @Value("${google.gmail.refresh-token}") String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    public Gmail getGmailClient() throws Exception {
        TokenResponse response = new GoogleRefreshTokenRequest(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                refreshToken, clientId, clientSecret).execute();

        Credential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setFromTokenResponse(response);

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("LifeAssistant")
                .build();
    }

    public List<Message> fetchNewGrabReceipts() throws Exception {
        Gmail service = getGmailClient();
        String query = "from:no-reply@grab.com subject:\"e-receipt\" is:unread";

        ListMessagesResponse response = service.users().messages().list("me")
                .setQ(query).execute();

        return response.getMessages() != null ? response.getMessages() : new ArrayList<>();
    }

    public String getMessageBody(String messageId) {
        try {
            Message message = getGmailClient().users().messages().get("me", messageId).execute();
            StringBuilder body = new StringBuilder();
            extractTextFromPart(message.getPayload(), body);

            String rawHtml = body.toString();

            String cleanText = rawHtml.replaceAll("(?is)<style.*?>.*?</style>", "");
            cleanText = cleanText.replaceAll("(?is)<script.*?>.*?</script>", "");

            cleanText = cleanText.replaceAll("<[^>]*>", " ");

            cleanText = cleanText.replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replaceAll("\\s+", " ")
                    .trim();

            log.info("Тело письма {} очищено. Было: {}, стало: {}",
                    messageId, rawHtml.length(), cleanText.length());
            return cleanText;
        } catch (Exception e) {
            log.error("Ошибка при получении тела письма {}", messageId, e);
            return null;
        }
    }

    private void extractTextFromPart(MessagePart part, StringBuilder out) {
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
            out.append(new String(decodedBytes));
        }

        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                extractTextFromPart(subPart, out);
            }
        }
    }
}