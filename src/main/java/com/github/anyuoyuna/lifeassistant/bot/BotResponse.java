package com.github.anyuoyuna.lifeassistant.bot;

public record BotResponse(String text, boolean html, boolean showMenu) {
    public static BotResponse plain(String text) {
        return new BotResponse(text, false, false);
    }
    public static BotResponse html(String text) {
        return new BotResponse(text, true, false);
    }
    public static BotResponse plainWithMenu(String text) {
        return new BotResponse(text, false, true);
    }
    public static BotResponse htmlWithMenu(String text) {
        return new BotResponse(text, true, true);
    }
}
