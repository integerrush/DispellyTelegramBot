package com.example.dispellybot;

import static com.example.dispellybot.components.BotCommands.LIST_OF_COMMANDS;

import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.User;
import com.example.dispellybot.database.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class DispellyTelegramBot extends TelegramLongPollingBot {

  final BotConfig config;

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
    } else {
      // handle default case, if necessary
    }
  }

  private void startBot(long chatId, String userName) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("Hello, " + userName + "! I'm a " + config.getBotName() +
        ". I will send emails specially for you using this unique key: " + chatId);
    try {
      execute(message);
      log.info("Reply sent to " + userName);
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

  @Override
  public void onUpdatesReceived(List<Update> updates) {
    super.onUpdatesReceived(updates);
  }

  // Метод для отправки сообщений в Telegram
  public void sendTelegramMessage(String text, User user) {
    SendMessage message = new SendMessage();
    message.setChatId(user.getId());
    message.setText(text);
    try {
      execute(message);
      log.info("Reply sent to " + user.getName());
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }
}


