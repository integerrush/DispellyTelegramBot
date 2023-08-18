package com.example.dispellybot.components;


import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.BotGroup;
import com.example.dispellybot.database.BotMessage;
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
import java.util.Objects;

@Slf4j
@Component
public class DispellyTelegramBot extends TelegramLongPollingBot {

    private static final List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start", "start bot"),
            new BotCommand("/stop", "Stop bot")
    );

    private final BotConfig config;
    private final BotGroupService botGroupService;

    public DispellyTelegramBot(BotConfig config, BotGroupService botGroupService) {
        this.config = config;
        this.botGroupService = botGroupService;
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
            startBot(chatId, userName);
        } else if (receivedMessage.equals("/stop" + "@" + config.getBotName())) {
            stopBot(chatId, userName);
        }
    }

    private void startBot(long chatId, String groupName) {
        BotGroup group = BotGroup.builder()
                .id(chatId).name(groupName)
                .build();
        StringBuilder text = new StringBuilder(256);
        text.append("Привет, ").append(groupName).append("!\n");
        text.append("Чтобы получать сообщения, необходимо подставить в письмо строку\n");
        text.append(config.getField()).append(": ").append(chatId);

        long missedMessages = botGroupService.startGroup(group);

        if (missedMessages > 0) {
            text.append("\nС последнего запуска пропущено ").append(missedMessages).append(" сообщений!");
        }

        sendMessage(group.getId(), text.toString());
    }

    private void stopBot(long chatId, String groupName) {
        BotGroup group = BotGroup.builder()
                .id(chatId).name(groupName)
                .build();
        String text = "Чтобы снова получать сообщения, введите команду /start@" + config.getBotName();

        botGroupService.stopGroup(group);

        sendMessage(group.getId(), text);
    }

    private boolean sendMessage(BotGroup group, String text) {
        if (Objects.isNull(group) || group.getId() == 0) {
            return true;
        }

        if (!group.isRunning()) {
            botGroupService.addMissedMessage(group);
            return true;
        }

        sendMessage(group.getId(), text);

        return false;
    }

    private void sendMessage(long groupId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(groupId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (Exception ex) {
            log.error("Can't send message", ex);
        }
    }

    public void sendTelegramMessage(BotMessage message) {
        boolean sent = sendMessage(message.getGroup(), message.getText());
        message.setSentToTelegram(sent);
    }
}


