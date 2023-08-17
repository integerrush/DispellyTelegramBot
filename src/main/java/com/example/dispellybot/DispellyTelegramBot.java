package com.example.dispellybot;


import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.BotMessage;
import com.example.dispellybot.database.User;
import com.example.dispellybot.database.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DispellyTelegramBot extends TelegramLongPollingBot {

    private static final List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start", "start bot")
    );

    private final BotConfig config;

    private final UserRepository userRepository;

    public DispellyTelegramBot(BotConfig config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        try {
            this.execute(new SetMyCommands(LIST_OF_COMMANDS, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId;
        String userName;
        String receivedMessage;

        // Eсли получено сообщение текстом
        if (update.hasMessage()) {
            chatId = update.getMessage().getChat().getId();
            userName = update.getMessage().getChat().getTitle();

            if (update.getMessage().hasText()) {
                receivedMessage = update.getMessage().getText();
                botAnswerUtils(receivedMessage, chatId, userName);
            }

            // Eсли нажата одна из кнопок бота
        } else if (update.hasCallbackQuery()) {
            chatId = update.getMessage().getChat().getId();
            userName = update.getCallbackQuery().getMessage().getChat().getTitle();
            receivedMessage = update.getCallbackQuery().getData();

            botAnswerUtils(receivedMessage, chatId, userName);
        }
    }

    private void botAnswerUtils(String receivedMessage, long chatId, String userName) {
        if (receivedMessage.equals("/start" + "@" + config.getBotName())) {
            updateDB(chatId, userName);
            startBot(chatId, userName);
        }
    }

    private void startBot(long chatId, String userName) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет, " + userName + "!\nЧтобы получать сообщения, необходимо подставить в письмо строку\n"
                + config.getField() + ": " + chatId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void updateDB(long chatId, String userName) {
        Optional<User> optionalUser = userRepository.findById(chatId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!user.getName().equals(userName)) {
                user.setName(userName);
                userRepository.save(user);
                log.info("The user's name is updated in DB: " + userName);
            } else {
                log.info("User " + userName + " is not new and has the same name");
            }
        } else {
            User user = new User();
            user.setId(chatId);
            user.setName(userName);

            userRepository.save(user);

            log.info("The user is added to DB: " + userName);
        }
    }

    public void sendTelegramMessage(BotMessage message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getGroupId());
        sendMessage.setText(message.getSubject() + "\n\n" + message.getText() + "\n\n" + message.getDate());
        try {
            if (message.getGroupId() != 0L) {
                execute(sendMessage);
            }
            message.setSentToTelegram(true);
        } catch (TelegramApiException e) {
            log.error("Can't send message", e);
            if (e.getMessage().contains("chat not found")) {
                message.setSentToTelegram(true);
            }
        }
    }
}


