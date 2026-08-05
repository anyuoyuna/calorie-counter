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
        log.info("KAFKA CONSUMER: Начало обработки письма: {}", event.messageId());

        try {
            log.info("Шаг 1: Запрашиваю текст письма из Gmail...");
            String emailBody = gmailService.getMessageBody(event.messageId());

            if (emailBody == null) {
                log.error("Шаг 1 ПРОВАЛ: Тело письма не получено");
                return;
            }
            log.info("Шаг 1 УСПЕХ: Текст письма получен (длина: {})", emailBody.length());

            log.info("Шаг 2: Отправляю в Gemini...");
            ParsedExpense parsed = aiAssistant.parseExpense("Это текст чека Grab. Извлеки сумму и описание: " + emailBody);

            log.info("Шаг 2 УСПЕХ: Gemini вернула: {} бат, {}", parsed.amount(), parsed.description());

            log.info("Шаг 3: Сохраняю в БД и Таблицу...");
            User user = userRepo.findById(event.userId())
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + event.userId() + " не найден в БД! Проверь DBeaver."));
            financeService.recordExpenseFromText(user,
                    "Grab: " + parsed.description() + " " + parsed.amount());

            log.info("✅ ШАГ 3 УСПЕХ: Письмо {} полностью обработано!", event.messageId());

        } catch (Exception e) {
            log.error("❌ КРИТИЧЕСКАЯ ОШИБКА в Consumer: ", e);
        }
    }
}