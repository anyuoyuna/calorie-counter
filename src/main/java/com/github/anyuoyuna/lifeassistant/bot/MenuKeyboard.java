package com.github.anyuoyuna.lifeassistant.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Component
public class MenuKeyboard {

    public static final String BTN_FOOD = "🍽 Log food";
    public static final String BTN_ACTIVITY = "🏃 Activity";
    public static final String BTN_TODAY = "📋 Today";
    public static final String BTN_PROFILE = "👤 Profile";
    public static final String BTN_WEEK = "📊 Week";
    public static final String BTN_QUESTION = "❓ Question";

    public ReplyKeyboardMarkup build() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        KeyboardRow row1 = new KeyboardRow();
        row1.add(BTN_FOOD);
        row1.add(BTN_ACTIVITY);
        KeyboardRow row2 = new KeyboardRow();
        row2.add(BTN_TODAY);
        row2.add(BTN_PROFILE);
        KeyboardRow row3 = new KeyboardRow();
        row3.add(BTN_WEEK);
        row3.add(BTN_QUESTION);
        markup.setKeyboard(List.of(row1, row2, row3));
        return markup;
    }
}
