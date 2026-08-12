package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.dto.ParsedExpense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import com.github.anyuoyuna.lifeassistant.infrastructure.google.GmailService;
import com.github.anyuoyuna.lifeassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrabReceiptConsumer {

    private final GmailService gmailService;
    private final GeneralAiAssistant aiAssistant;
    private final FinanceService financeService;
    private final UserRepository userRepo;

    @KafkaListener(topics = "raw-receipts", groupId = "life-assistant-group-v3")
    public void consume(GrabReceiptEvent event) {
        log.info("KAFKA CONSUMER: Started processing email: {}", event.messageId());
        try {
            String emailBody = gmailService.getMessageBody(event.messageId());
            if (emailBody == null) {
                return;
            }
            ParsedExpense parsed = aiAssistant.parseExpense("This is a Grab receipt text. Extract the amount and description: " + emailBody);
            User user = userRepo.findById(event.userId())
                    .orElseThrow(() -> new RuntimeException("User with ID " + event.userId() + " not found in DB! Check DBeaver."));
            financeService.recordExpenseFromText(user,
                    "Grab: " + parsed.description() + " " + parsed.amount());
            log.info("✅ Email {} fully processed!", event.messageId());
        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR in Consumer: ", e);
        }
    }
}