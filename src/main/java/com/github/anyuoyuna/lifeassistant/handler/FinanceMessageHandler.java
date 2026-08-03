package com.github.anyuoyuna.lifeassistant.handler;

import com.github.anyuoyuna.lifeassistant.bot.BotResponse;
import com.github.anyuoyuna.lifeassistant.domain.finance.FinanceService;
import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.entity.User;
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
            Expense expense = financeService.recordExpense(user, text);
            String message = String.format("✅ Записала в таблицу: %s %.2f (%s)",
                    expense.getCategory(), expense.getAmount(), expense.getDescription());
            return BotResponse.plain(message);
        } catch (Exception e) {
            log.error("Ошибка при записи финансов", e);
            return BotResponse.plain("Не смогла распознать трату. Попробуй написать понятнее, например: 'Кофе 120 бат'");
        }
    }
}
