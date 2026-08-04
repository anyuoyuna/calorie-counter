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
            return BotResponse.plain("Не смогла распознать трату в тексте.");
        }
    }

    public BotResponse handlePhoto(User user, Image image) {
        try {
            Expense expense = financeService.recordExpenseFromImage(user, image);
            return formatResponse(expense);
        } catch (Exception e) {
            return BotResponse.plain("Не удалось прочитать чек на фото.");
        }
    }

    private BotResponse formatResponse(Expense e) {
        return BotResponse.plain(String.format("✅ Записано: %s — %.2f бат (%s)",
                e.getCategory(), e.getAmount(), e.getDescription()));
    }
}
