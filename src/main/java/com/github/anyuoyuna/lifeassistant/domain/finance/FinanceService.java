package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.lifeassistant.dto.ParsedExpense;
import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.enums.ExpenseCategory;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import com.github.anyuoyuna.lifeassistant.infrastructure.google.GoogleSheetsService;
import com.github.anyuoyuna.lifeassistant.repository.ExpenseRepository;
import com.github.anyuoyuna.lifeassistant.repository.UserRepository;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FinanceService {

    private final GeneralAiAssistant aiAssistant;
    private final GoogleSheetsService googleSheetsService;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ChatLanguageModel geminiModel;

    public FinanceService(GeneralAiAssistant aiAssistant,
                          GoogleSheetsService googleSheetsService,
                          ExpenseRepository expenseRepository,
                          Clock clock,
                          UserRepository userRepository,
                          ObjectMapper objectMapper,
                          @Qualifier("geminiModel") ChatLanguageModel geminiModel) {
        this.aiAssistant = aiAssistant;
        this.googleSheetsService = googleSheetsService;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.geminiModel = geminiModel;
    }

    @Transactional
    public Expense recordExpenseFromText(User user, String text) {
        ParsedExpense parsed = aiAssistant.parseExpense(text);
        return saveParsedExpense(user, parsed);
    }

    @Transactional
    public Expense recordExpenseFromImage(User user, Image image) {
        UserMessage message = UserMessage.from(
                TextContent.from(GeneralAiAssistant.PHOTO_BILL_PROMPT),
                ImageContent.from(image.base64Data(), image.mimeType())
        );

        Response<AiMessage> response = geminiModel.generate(message);
        String rawJson = response.content().text();

        try {
            ParsedExpense parsed = objectMapper.readValue(stripMarkdownFences(rawJson), ParsedExpense.class);
            return saveParsedExpense(user, parsed);
        } catch (Exception e) {
            log.error("Ошибка парсинга чека: {}", rawJson, e);
            throw new RuntimeException("Не удалось распознать чек");
        }
    }

    @Transactional
    public int importHistoryFromSheets() {
        List<List<Object>> rows = googleSheetsService.readRows("Операции", "A3:F");
        if (rows == null || rows.isEmpty()) return 0;

        int count = 0;

        List<User> allUsers = userRepository.findAll();

        for (List<Object> row : rows) {
            try {
                if (row.size() < 5 || row.get(0) == null || row.get(0).toString().isBlank()) continue;

                String dateStr = row.get(0).toString();
                String whoStr = row.get(1).toString();
                String categoryStr = row.get(2).toString();
                String description = row.get(3).toString();
                String amountStr = row.get(4).toString();
                String type = row.size() > 5 ? row.get(5).toString() : "-";

                User expenseOwner = allUsers.stream()
                        .filter(u -> whoStr.equalsIgnoreCase(u.getDisplayName()))
                        .findFirst()
                        .orElse(null);

                if (expenseOwner == null) {
                    log.warn("Пропуск строки: пользователь '{}' не найден в БД. Строка: {}", whoStr, row);
                    continue;
                }

                Expense e = new Expense();
                e.setUser(expenseOwner);
                e.setDate(parseDate(dateStr));
                e.setCategory(com.github.anyuoyuna.lifeassistant.entity.enums.ExpenseCategory.valueOf(categoryStr));
                e.setDescription(description);
                e.setAmount(new BigDecimal(amountStr.replace(",", ".").replaceAll("\\s", "")));
                e.setType(type);

                expenseRepository.save(e);
                count++;
            } catch (Exception ex) {
                log.warn("Ошибка импорта строки {}: {}", row, ex.getMessage());
            }
        }
        log.info("Успешно импортировано {} записей из Google Sheets", count);
        return count;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return LocalDate.parse(dateStr, dtf);
        }
    }

    private Expense saveParsedExpense(User user, ParsedExpense parsed) {
        if (parsed == null || parsed.amount() == null) {
            throw new RuntimeException("Не удалось распознать сумму");
        }

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setDate(LocalDate.now(clock));
        expense.setAmount(parsed.amount());
        expense.setCategory(safeParseCategory(parsed.category()));
        expense.setDescription(parsed.description());
        expense.setType((parsed.type() != null && parsed.type().contains("+")) ? "+" : "-");
        expense.setExternalId(UUID.randomUUID().toString());

        expenseRepository.save(expense);

        googleSheetsService.appendRow("Операции", List.of(
                expense.getDate().toString(),
                user.getDisplayName(),
                expense.getCategory().name(),
                expense.getDescription() != null ? expense.getDescription() : "",
                expense.getAmount(),
                expense.getType(),
                expense.getExternalId()
        ));

        return expense;
    }

    private ExpenseCategory safeParseCategory(String cat) {
        try {
            return ExpenseCategory.valueOf(cat);
        } catch (Exception e) {
            return ExpenseCategory.Others;
        }
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return null;
        return text.replace("```json", "").replace("```", "").trim();
    }
}
