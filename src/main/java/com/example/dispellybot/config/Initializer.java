package com.example.dispellybot.config;

import com.example.dispellybot.components.DispellyTelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class Initializer {

  private final DispellyTelegramBot bot;

  @EventListener({ContextRefreshedEvent.class})
  public void init() {
    try {
      TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
      telegramBotsApi.registerBot(bot);
      log.info("Bot registered successfully");
    } catch (TelegramApiException e) {
      log.error("Bot registration failed: " + e.getMessage());
    }
  }
}