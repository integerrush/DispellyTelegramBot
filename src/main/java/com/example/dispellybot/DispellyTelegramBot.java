package com.example.dispellybot;

import static com.example.dispellybot.components.BotCommands.HELP_TEXT;
import static com.example.dispellybot.components.BotCommands.LIST_OF_COMMANDS;
import static java.util.Objects.nonNull;

import com.example.dispellybot.components.MailMessageBuilder;
import com.example.dispellybot.components.StoreAndFolder;
import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.User;
import com.example.dispellybot.database.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

  public static Timer timer;

  public static StoreAndFolder storeAndFolder;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MailMessageBuilder mailMessageBuilder;


  public DispellyTelegramBot(BotConfig config) {
    this.config = config;
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
    long chatId = 0;
    long userId = 0;
    String userName = null;
    String receivedMessage;

    // Eсли получено сообщение текстом
    if (update.hasMessage()) {
      chatId = update.getMessage().getChat().getId();
      //userId = update.getMessage().getFrom().getId();
      //userName = update.getMessage().getFrom().getFirstName();
      userName = update.getMessage().getChat().getTitle();

      if (update.getMessage().hasText()) {
        receivedMessage = update.getMessage().getText();
        botAnswerUtils(receivedMessage, chatId, userName);
      }

      // Eсли нажата одна из кнопок бота
    } else if (update.hasCallbackQuery()) {
      chatId = update.getMessage().getChat().getId();
      //userId = update.getCallbackQuery().getFrom().getId();
      //userName = update.getCallbackQuery().getFrom().getFirstName();
      userName = update.getCallbackQuery().getMessage().getChat().getTitle();
      receivedMessage = update.getCallbackQuery().getData();

      botAnswerUtils(receivedMessage, chatId, userName);
    }
  }

  private void botAnswerUtils(String receivedMessage, long chatId, String userName) {

    if (receivedMessage.equals("/start" + "@" + config.getBotName())) {
      updateDB(chatId, userName);
      startBot(chatId, userName);
    } else if (receivedMessage.equals("/help" + "@" + config.getBotName())) {
      sendHelpText(chatId, userName, HELP_TEXT);
    } else if (receivedMessage.equals("/connect" + "@" + config.getBotName())) {
      storeAndFolder = connectToMessageStore(chatId, userName);
    } else if (receivedMessage.equals("/fetchmail" + "@" + config.getBotName())) {
      fetchAndSendEmails(chatId, userName, storeAndFolder);
    } else if (receivedMessage.equals("/autofetch" + "@" + config.getBotName())) {
      mailTimerTask(chatId, userName, storeAndFolder);
    } else if (receivedMessage.equals("/cancel" + "@" + config.getBotName())) {
      stopTimerTask(userName, chatId, storeAndFolder);
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

  private void sendHelpText(long chatId, String userName, String textToSend) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(textToSend);

    try {
      execute(message);
      log.info("Reply sent to " + userName);
    } catch (TelegramApiException e) {
      log.error(e.getMessage());
    }
  }

  @Override
  public void onUpdatesReceived(List<Update> updates) {
    super.onUpdatesReceived(updates);
  }

  // Метод для отправки сообщений в Telegram
  private void sendTelegramMessage(String text, String userName, long chatId) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
    try {
      execute(message);
      log.info("Reply sent to " + userName);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  /**
   * Connects to the message store.
   */
  public StoreAndFolder connectToMessageStore(long chatId, String userName) {
    Properties properties = getServerProperties(config.getEmailHost(), config.getEmailPort());
    Session session = javax.mail.Session.getInstance(properties);

    try {
      // connects to the message store
      Store store = session.getStore("imap");
      store.connect(config.getEmailHost(), config.getEmailLogin(), config.getEmailPassword());

      // opens the inbox folder
      Folder inbox = store.getFolder("INBOX");
      inbox.open(Folder.READ_WRITE);
      log.info(userName + " successfully connected to email server");

      return new StoreAndFolder(store, inbox);

    } catch (NoSuchProviderException ex) {
      log.error("No provider for protocol: " + config.getEmailProtocol());
      ex.printStackTrace();
    } catch (MessagingException ex) {
      log.error("Could not connect to the message store");
      ex.printStackTrace();
    }
    return null;
  }

  /**
   * Downloads new messages and fetches details for each message.
   */
  private void fetchAndSendEmails(long chatId, String userName, StoreAndFolder storeAndFolder) {

    if (nonNull(storeAndFolder) && storeAndFolder.getFolder().isOpen()) {

      // fetches new messages from server
      Message[] messages = new Message[0];
      try {
        messages = storeAndFolder.getFolder()
            .search(new javax.mail.search.FlagTerm(new Flags(Flags.Flag.SEEN), false));

        if (messages.length == 0) {
          log.info("No new messages found in inbox.");
          sendTelegramMessage("No new messages found in inbox.", userName, chatId);
        } else {
          boolean messageSent = false; // флаг, отвечающий за отправку сообщения
          for (Message message : messages) {
            MimeMessage mimeMessage = (MimeMessage) message;
            String sender =
                mailMessageBuilder.parseSender((Arrays.toString(mimeMessage.getFrom())));

            //Фильтруем по отправителю
            if (StringUtils.equals(sender, config.getSender())) {

              String subject = mimeMessage.getSubject();
              String toList = mailMessageBuilder.parseAddresses(
                  message.getRecipients(MimeMessage.RecipientType.TO));
              String ccList = mailMessageBuilder.parseAddresses(
                  message.getRecipients(MimeMessage.RecipientType.CC));
              String sentDate = message.getSentDate().toString();

              // Считываем текст письма
              String messageContent = MailMessageBuilder.getString(message);
              // Парсим сообщение
              messageContent = mailMessageBuilder.parseMessageContent(messageContent);
              // Составляем отправляемый текст
              String text = mailMessageBuilder.buildMessage(messageContent, subject, toList, ccList, sentDate);
              // Ищем упоминание адресата в тексте письма
              String result = mailMessageBuilder.findBotField(messageContent);

              // Маршрутизация
              long groupId = 0;
              if (result != null) {
                groupId = Long.valueOf(result);
              }
              if (groupId != 0) {
                Optional<User> optionalUser = userRepository.findById(groupId);
                if (optionalUser.isPresent()) {
                  if (chatId == groupId) {
                    // Отправка сообщения в Telegram
                    sendTelegramMessage(text, userName, chatId);
                    messageSent = true; // сообщение было отправлено
                    // Пометка сообщения как прочитанного
                    message.setFlag(Flags.Flag.SEEN, true);
                  }
                } else {
                  // Пометка сообщения как непрочитанного
                  message.setFlag(Flags.Flag.SEEN, false);
                }
              }
              if (!messageSent) {
                // Если не было отправленных сообщений
                log.info("No messages found in inbox for " + userName);
                message.setFlag(Flags.Flag.SEEN, false);
              }
            } else {
              log.info("The message is not from the sender we demanded.");
            }
          }
        }
        storeAndFolder.getFolder().close(false);
        storeAndFolder.getStore().close();
        log.info("Folder closed. Next time reconnect to the message store");
      } catch (MessagingException ex) {
        log.error("Could not connect to the message store");
        ex.printStackTrace();
      }
    } else {
      log.info("Fetchmail selected without connection to the message store for " + userName);
    }
  }

  /**
   * Starts automatic check for new messages in inbox.
   */
  public void mailTimerTask(long chatId, String userName, StoreAndFolder storeAndFolder) {
    long delay = Long.parseLong(config.getDelay());
    long period = Long.parseLong(config.getTime());

    if (nonNull(storeAndFolder) && storeAndFolder.getFolder().isOpen()) {
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          // Проверка наличия новых сообщений в почте и их обработка
          autoFetchAndSendEmails(chatId, userName, storeAndFolder);
        }
      }, delay, period);
      log.info("Autofetch started for " + userName);
    } else {
      log.error("Autofetch without connection to the message store for " + userName);
    }
  }


  /**
   * Stops automatic check for new messages in inbox.
   */
  public void stopTimerTask(String userName, long chatId, StoreAndFolder storeAndFolder) {
    timer.cancel();
    try {
      storeAndFolder.getFolder().close(false);
      storeAndFolder.getStore().close();
    } catch (MessagingException e) {
      log.error("Could not close the connection for" + userName + ": " + e.getMessage());
      throw new RuntimeException();
    }
    log.info("Autofetch cancelled for " + userName);
  }

  private void autoFetchAndSendEmails(long chatId, String userName, StoreAndFolder storeAndFolder) {

    // fetches new messages from server
    Message[] messages = new Message[0];
    try {
      messages = storeAndFolder.getFolder()
          .search(new javax.mail.search.FlagTerm(new Flags(Flags.Flag.SEEN), false));

      if (messages.length == 0) {
        log.info("No new messages found in inbox.");
      } else {
        boolean messageSent = false; // флаг, отвечающий за отправку сообщения
        for (Message message : messages) {
          MimeMessage mimeMessage = (MimeMessage) message;
          String sender =
              mailMessageBuilder.parseSender((Arrays.toString(mimeMessage.getFrom())));

          //Фильтруем по отправителю
          if (StringUtils.equals(sender, config.getSender())) {

            String subject = mimeMessage.getSubject();
            String toList = mailMessageBuilder.parseAddresses(
                message.getRecipients(MimeMessage.RecipientType.TO));
            String ccList = mailMessageBuilder.parseAddresses(
                message.getRecipients(MimeMessage.RecipientType.CC));
            String sentDate = message.getSentDate().toString();

            // Считываем текст письма
            String messageContent = MailMessageBuilder.getString(message);
            // Парсим сообщение
            messageContent = mailMessageBuilder.parseMessageContent(messageContent);
            // Составляем отправляемый текст
            String text = mailMessageBuilder.buildMessage(messageContent, subject, toList, ccList, sentDate);
            // Ищем упоминание адресата в тексте письма
            String result = mailMessageBuilder.findBotField(messageContent);

            // Маршрутизация
            long groupId = 0;
            if (result != null) {
              groupId = Long.valueOf(result);
            }
            if (groupId != 0) {
              Optional<User> optionalUser = userRepository.findById(groupId);
              if (optionalUser.isPresent()) {
                if (chatId == groupId) {
                  // Отправка сообщения в Telegram
                  sendTelegramMessage(text, userName, chatId);
                  messageSent = true; // сообщение было отправлено
                  // Пометка сообщения как прочитанного
                  message.setFlag(Flags.Flag.SEEN, true);
                }
              } else {
                // Пометка сообщения как непрочитанного
                message.setFlag(Flags.Flag.SEEN, false);
              }
            }
            if (!messageSent) {
              // Если не было отправленных сообщений
              log.info("No messages found in inbox for " + userName);
              message.setFlag(Flags.Flag.SEEN, false);
            }
          } else {
            //Не правильный отправитель
          }
        }
      }
    } catch (NoSuchProviderException ex) {
      log.error("No provider for protocol: " + config.getEmailProtocol());
      ex.printStackTrace();
    } catch (MessagingException ex) {
      log.error("Could not connect to the message store");
      ex.printStackTrace();
    }
  }

  /**
   * Returns a Properties object which is configured for a IMAP server
   */
  private Properties getServerProperties(String host, String port) {
    Properties properties = new Properties();
    // server setting
    properties.put("mail.imap.host", host);
    properties.put("mail.imap.port", port);
    // SSL setting
    properties.setProperty("mail.imap.ssl.enable", "true");
    properties.put("mail.imap.ssl.protocols", "TLSv1.2");
    properties.put("mail.imap.ssl.trust", host);
    properties.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    properties.setProperty("mail.imap.socketFactory.fallback", "false");
    properties.setProperty("mail.imap.socketFactory.port", port);

    return properties;
  }
}


