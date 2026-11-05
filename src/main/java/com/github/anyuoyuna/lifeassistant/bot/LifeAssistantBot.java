package com.github.anyuoyuna.lifeassistant.bot;

import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.handler.FinanceMessageHandler;
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

@Slf4j
@Component
public class LifeAssistantBot extends TelegramLongPollingBot {
    private final UserRepository userRepo;
    private final TelegramMessageRouter router;
    private final MenuKeyboard menuKeyboard;
    private final FinanceMessageHandler financeMessageHandler;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public LifeAssistantBot(@Value("${telegram.bot.token}") String botToken,
                            UserRepository userRepo,
                            MenuKeyboard menuKeyboard,
                            TelegramMessageRouter router,
                            FinanceMessageHandler financeMessageHandler) {
        super(botToken);
        this.userRepo = userRepo;
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
            log.debug("Ignoring unsupported callback {}", update.getCallbackQuery().getData());
            return;
        }
        if (!update.hasMessage()) return;
        if (update.getMessage().hasPhoto()) {
            handlePhotoMessage(update);
            return;
        }
        if (!update.getMessage().hasText()) return;
        Long telegramId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();
        Optional<User> existing = userRepo.findByTelegramId(telegramId);
        if (existing.isEmpty()) {
            startNewUserFlow(telegramId, update.getMessage().getFrom().getFirstName(), chatId);
            return;
        }
        BotResponse response = router.route(existing.get(), update.getMessage().getText().trim());
        send(chatId, response);
    }

    private void startNewUserFlow(Long telegramId, String displayName, Long chatId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setDisplayName(displayName);
        userRepo.save(user);
        log.info("Registered user {}", telegramId);
        send(chatId, BotResponse.plainWithMenu("Welcome! Send an expense, income, receipt, or finance question."));
    }

    private void handlePhotoMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        Long telegramId = update.getMessage().getFrom().getId();
        User user = userRepo.findByTelegramId(telegramId).orElseGet(() -> {
            User created = new User();
            created.setTelegramId(telegramId);
            created.setDisplayName(update.getMessage().getFrom().getFirstName());
            return userRepo.save(created);
        });
        List<PhotoSize> photos = update.getMessage().getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();
        send(chatId, BotResponse.plain("Got your receipt, give me a sec, analyzing it..."));
        CompletableFuture.runAsync(() -> processReceipt(chatId, user, fileId));
    }

    private void processReceipt(Long chatId, User user, String fileId) {
        try {
            byte[] photoBytes = downloadPhoto(fileId);
            if (photoBytes == null) throw new IllegalStateException("Photo download failed");
            Image image = Image.builder().base64Data(Base64.getEncoder().encodeToString(photoBytes))
                    .mimeType("image/jpeg").build();
            send(chatId, financeMessageHandler.handlePhoto(user, image));
        } catch (Exception e) {
            log.error("Error processing photo", e);
            send(chatId, BotResponse.plain("Couldn't read the receipt. Try taking a clearer photo."));
        }
    }

    private byte[] downloadPhoto(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            File file = execute(getFile);
            java.io.File downloaded = downloadFile(file);
            byte[] bytes = java.nio.file.Files.readAllBytes(downloaded.toPath());
            downloaded.delete();
            return bytes;
        } catch (Exception e) {
            log.error("Error downloading photo from Telegram", e);
            return null;
        }
    }

    private void send(Long chatId, BotResponse response) {
        if (response == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(response.text());
        if (response.html()) message.setParseMode("HTML");
        if (response.showMenu()) message.setReplyMarkup(menuKeyboard.build());
        try { execute(message); } catch (TelegramApiException e) { log.error("Error sending message to Telegram", e); }
    }
}
