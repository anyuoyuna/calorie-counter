package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.dto.ParsedExpense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import com.github.anyuoyuna.lifeassistant.infrastructure.google.GmailService;
import com.github.anyuoyuna.lifeassistant.repository.UserRepository;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrabMailPoller {

    private final GmailService gmailService;
    private final GeneralAiAssistant aiAssistant;
    private final FinanceService financeService;
    private final UserRepository userRepo;

    private static final Long ADMIN_USER_ID = 1L;

    @Scheduled(fixedRate = 600000)
    public void pollEmails() {
        try {
            log.info("Checking Gmail for new Grab receipts...");
            List<Message> messages = gmailService.fetchNewGrabReceipts();
            if (messages.isEmpty()) {
                log.info("No new receipts found.");
                return;
            }
            User user = userRepo.findById(ADMIN_USER_ID)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            for (Message msg : messages) {
                processReceipt(user, msg.getId());
            }
        } catch (Exception e) {
            log.error("Error during mail polling: ", e);
        }
    }

    private void processReceipt(User user, String messageId) throws Exception {
        log.info("Processing receipt: {}", messageId);
        String emailBody = gmailService.getMessageBody(messageId);
        if (emailBody == null || emailBody.isBlank()) return;
        ParsedExpense parsed = aiAssistant.parseExpense("This is a Grab e-receipt text. Extract amount and description: " + emailBody);
        financeService.recordExpenseFromText(user, "Auto-Grab: " + parsed.description() + " " + parsed.amount());
        gmailService.getGmailClient().users().messages().trash("me", messageId).execute();
        log.info("✅ Receipt {} processed and deleted.", messageId);
    }
}