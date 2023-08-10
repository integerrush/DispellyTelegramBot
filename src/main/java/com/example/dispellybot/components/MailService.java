package com.example.dispellybot.components;

import com.example.dispellybot.DispellyTelegramBot;
import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.BotMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.Address;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MailService {

    final BotConfig config;
    private final DispellyTelegramBot dispellyTelegramBot;
    private final MessageService messageService;

    private final ScheduledExecutorService mailFetchExecutor;

    public MailService(BotConfig config, DispellyTelegramBot dispellyTelegramBot, MessageService messageService) {
        this.config = config;
        this.dispellyTelegramBot = dispellyTelegramBot;
        this.messageService = messageService;
        this.mailFetchExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    public void init() {
        long delay = Long.parseLong(config.getDelay());
        long period = Long.parseLong(config.getTime());

        mailFetchExecutor.scheduleAtFixedRate(this::parseAndSendMessages, delay, period, TimeUnit.MILLISECONDS);
    }

    public void parseAndSendMessages() {
        List<BotMessage> messages = messageService.getMessagesForSent(message -> {
            String fromAddresses = "";
            try {
                fromAddresses = parseAddresses(message.getFrom());
            } catch (Exception ex) {
                log.error("Can't get FROM address", ex);
            }
            return fromAddresses.contains(config.getSender());
        });

        log.info("Found {} messages", messages.size());

        messages.forEach(dispellyTelegramBot::sendTelegramMessage);
        messageService.saveAll(messages);
    }

    private String parseAddresses(Address[] address) {
        StringBuilder listAddress = new StringBuilder();

        if (address != null) {
            for (int i = 0; i < address.length; i++) {
                if (i > 0 && i != address.length - 1) {
                    listAddress.append(", ");
                }
                listAddress.append(address[i].toString());
            }
        }

        return listAddress.toString();
    }
}
