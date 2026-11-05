package com.github.anyuoyuna.lifeassistant.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Component
public class MenuKeyboard {

    public static final String BTN_QUESTION = "Finance question";

    public ReplyKeyboardMarkup build() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        KeyboardRow row = new KeyboardRow();
        row.add(BTN_QUESTION);
        markup.setKeyboard(List.of(row));
        return markup;
    }
}
