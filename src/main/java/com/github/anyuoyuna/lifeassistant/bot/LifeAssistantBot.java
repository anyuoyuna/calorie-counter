package com.github.anyuoyuna.lifeassistant.bot;

import com.github.anyuoyuna.lifeassistant.domain.profile.ProfileEditHandler;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.handler.FinanceMessageHandler;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import com.github.anyuoyuna.lifeassistant.onboarding.OnboardingHandler;
import com.github.anyuoyuna.lifeassistant.repository.UserRepository;
import dev.langchain4j.data.image.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.github.anyuoyuna.lifeassistant.bot.MenuKeyboard.BTN_PROFILE;

@Slf4j
@Component
public class LifeAssistantBot extends TelegramLongPollingBot {

    private final UserRepository userRepo;
    private final OnboardingHandler onboardingHandler;
    private final ProfileEditHandler profileEditHandler;
    private final TelegramMessageRouter router;
    private final MenuKeyboard menuKeyboard;
    private final FinanceMessageHandler financeMessageHandler;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public LifeAssistantBot(@Value("${telegram.bot.token}") String botToken,
                            UserRepository userRepo,
                            OnboardingHandler onboardingHandler,
                            ProfileEditHandler profileEditHandler,
                            MenuKeyboard menuKeyboard,
                            TelegramMessageRouter router, FinanceMessageHandler financeMessageHandler, GeneralAiAssistant aiAssistant) {
        super(botToken);
        this.userRepo = userRepo;
        this.onboardingHandler = onboardingHandler;
        this.profileEditHandler = profileEditHandler;
        this.router = router;
        this.menuKeyboard = menuKeyboard;
        this.financeMessageHandler = financeMessageHandler;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (update.getMessage().hasPhoto()) {
            handlePhotoMessage(update);
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long telegramId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();

        Optional<User> userOpt = userRepo.findByTelegramId(telegramId);

        if (userOpt.isEmpty()) {
            startNewUserFlow(telegramId, chatId);
            return;
        }

        User user = userOpt.get();

        if (onboardingHandler.isInProgress(telegramId)) {
            send(chatId, onboardingHandler.handleAnswer(telegramId, chatId, text, null));
            if (!onboardingHandler.isInProgress(telegramId)) {
                User updatedUser = userRepo.findByTelegramId(telegramId).orElseThrow();
                send(chatId, BotResponse.plainWithMenu("Профиль готов! Теперь я буду узнавать тебя по имени " + updatedUser.getDisplayName()));
            }
            return;
        }

        if (profileEditHandler.isInProgress(telegramId)) {
            send(chatId, profileEditHandler.handle(telegramId, chatId, text, null));
            return;
        }

        BotResponse response = router.route(user, text);
        if (response != null) {
            send(chatId, response);
            if (text.equals(BTN_PROFILE) || text.equals("/profile")) {
                send(chatId, profileEditHandler.editMenu(chatId));
            }
        }
    }

    private void handleCallback(Update update) {
        Long telegramId = update.getCallbackQuery().getFrom().getId();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackData = update.getCallbackQuery().getData();

        if (onboardingHandler.isInProgress(telegramId)) {
            send(chatId, onboardingHandler.handleAnswer(telegramId, chatId, null, callbackData));
            if (!onboardingHandler.isInProgress(telegramId)) {
                send(chatId, BotResponse.plainWithMenu("Теперь можешь пользоваться ботом 👇"));
            }
            return;
        }
        if (profileEditHandler.isInProgress(telegramId) || callbackData.startsWith("EDIT_MENU_")
                || callbackData.startsWith("GOAL_") || callbackData.startsWith("ACT_")) {
            send(chatId, profileEditHandler.handle(telegramId, chatId, null, callbackData));
        }
    }

    private void send(Long chatId, BotResponse response) {
        if (response == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(response.text());
        if (response.html()) message.setParseMode("HTML");
        if (response.showMenu()) message.setReplyMarkup(menuKeyboard.build());
        executeMessage(message);
    }

    private void send(Long chatId, SendMessage message) {
        if (message == null) return;
        message.setChatId(chatId.toString());
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в Telegram", e);
        }
    }

    private void startNewUserFlow(Long telegramId, Long chatId) {
        User newUser = new User();
        newUser.setTelegramId(telegramId);
        userRepo.save(newUser);
        log.info("Зарегистрирован абсолютно новый пользователь: {}", telegramId);

        onboardingHandler.startOnboarding(telegramId);
        send(chatId, onboardingHandler.firstQuestion(chatId));
    }

    private byte[] downloadPhoto(String fileId) {
        try {
            GetFile getFile = new org.telegram.telegrambots.meta.api.methods.GetFile();
            getFile.setFileId(fileId);
            File file = execute(getFile);
            java.io.File downloaded = downloadFile(file);
            byte[] bytes = java.nio.file.Files.readAllBytes(downloaded.toPath());
            downloaded.delete();
            return bytes;
        } catch (Exception e) {
            log.error("Ошибка при скачивании фото из Telegram", e);
            return null;
        }
    }

    private void handlePhotoMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        Long telegramId = update.getMessage().getFrom().getId();

        List<PhotoSize> photos = update.getMessage().getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();

        User user = userRepo.findByTelegramId(telegramId).orElseThrow();

        send(chatId, BotResponse.plain("Вижу чек, секунду, анализирую..."));

        CompletableFuture.runAsync(() -> {
            try {
                byte[] photoBytes = downloadPhoto(fileId);
                String base64Data = Base64.getEncoder().encodeToString(photoBytes);
                log.info("Скачано фото чека, размер: {} байт", photoBytes.length);
                if (photoBytes.length < 100) {
                    log.error("Файл слишком маленький, возможно скачивание не удалось");
                }
                Image image = Image.builder()
                        .base64Data(base64Data)
                        .mimeType("image/jpeg")
                        .build();

                BotResponse response = financeMessageHandler.handlePhoto(user, image);
                send(chatId, response);

            } catch (Exception e) {
                log.error("Ошибка при обработке фото", e);
                send(chatId, BotResponse.plain("Не удалось прочитать чек. Попробуй сделать фото четче."));
            }
        });
    }
}