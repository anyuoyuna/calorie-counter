package com.github.anyuoyuna.lifeassistant.handler;

import com.github.anyuoyuna.lifeassistant.bot.BotResponse;
import com.github.anyuoyuna.lifeassistant.domain.finance.FinanceService;
import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import dev.langchain4j.data.image.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FinanceMessageHandler {

    private final FinanceService financeService;

    public FinanceMessageHandler(FinanceService financeService) {
        this.financeService = financeService;
    }

    public BotResponse handle(User user, String text) {
        try {
            Expense expense = financeService.recordExpenseFromText(user, text);
            return formatResponse(expense);
        } catch (Exception e) {
            return BotResponse.plain("Could not detect any expenditure in the text.");
        }
    }

    public BotResponse handlePhoto(User user, Image image) {
        try {
            Expense expense = financeService.recordExpenseFromImage(user, image);
            return formatResponse(expense);
        } catch (Exception e) {
            return BotResponse.plain("Failed to read the receipt from the photo.");
        }
    }

    public BotResponse handleQuery(User user, String text) {
        try {
            String answer = financeService.answerFinanceQuestion(text);
            return BotResponse.plain(answer);
        } catch (Exception e) {
            log.error("Error when answering a financial question", e);
            return BotResponse.plain("Failed to retrieve financial analytics..");
        }
    }

    private BotResponse formatResponse(Expense e) {
        return BotResponse.plain(String.format("✅ Logged: %s — %.2f thb (%s)",
                e.getCategory(), e.getAmount(), e.getDescription()));
    }
}
