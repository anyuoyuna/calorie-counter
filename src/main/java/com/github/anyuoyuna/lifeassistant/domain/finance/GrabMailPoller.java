package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.infrastructure.google.GmailService;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrabMailPoller {

    private final GmailService gmailService;
    private final KafkaTemplate<String, GrabReceiptEvent> kafkaTemplate;

    @Scheduled(fixedRate = 600000)
    public void pollEmails() {
        try {
            List<Message> messages = gmailService.fetchNewGrabReceipts();
            if (messages.isEmpty()) {
                return;
            }
            Long adminUserId = 1L;
            for (Message msg : messages) {
                GrabReceiptEvent event = new GrabReceiptEvent(adminUserId, msg.getId(), "Grab receipt found");
                kafkaTemplate.send("raw-receipts", event);
                log.info("Receipt {} sent to Kafka for processing", msg.getId());
                deleteMessage(msg.getId());
            }
        } catch (Exception e) {
            log.error("Error polling email: ", e);
        }
    }

    private void deleteMessage(String messageId) throws Exception {
        gmailService.getGmailClient().users().messages().trash("me", messageId).execute();
        log.info("Email {} moved to trash", messageId);
    }
}