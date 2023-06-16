package com.example.dispellybot.components;

import static java.util.Objects.isNull;

import com.example.dispellybot.DispellyTelegramBot;
import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.User;
import com.example.dispellybot.database.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PostConstruct;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FromStringTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MailService {

  final BotConfig config;
  private final UserRepository userRepository;
  private final MailMessageBuilder mailMessageBuilder;
  private final DispellyTelegramBot dispellyTelegramBot;

  public MailService(BotConfig config, UserRepository userRepository,
                     MailMessageBuilder mailMessageBuilder, DispellyTelegramBot dispellyTelegramBot) {
    this.config = config;
    this.userRepository = userRepository;
    this.mailMessageBuilder = mailMessageBuilder;
    this.dispellyTelegramBot = dispellyTelegramBot;
  }

  @PostConstruct
  public void init() {
    Store store = connectToMessageStore();
    mailTimerTask(store);
  }

  /**
   * Starts automatic check for new messages in inbox.
   */
  public void mailTimerTask(Store store) {
    long delay = Long.parseLong(config.getDelay());
    long period = Long.parseLong(config.getTime());

    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        Store storeCopy = store;
        if (isNull(storeCopy)) {
          storeCopy = connectToMessageStore();
          log.info("Reconnected to the message store.");
        }
        List<User> users = userRepository.findAll();
        for (User user : users) {
          // Проверка наличия новых сообщений в почте и их обработка
          autoFetchAndSendEmails(user, storeCopy);
        }
      }
    }, delay, period);
    log.info("Autofetch started.");
  }

  /**
   * Connects to the message store.
   */
  public Store connectToMessageStore() {
    Properties properties = getServerProperties(config.getEmailHost(), config.getEmailPort());
    Session session = javax.mail.Session.getInstance(properties);

    try {
      // connects to the message store
      Store store = session.getStore("imap");
      store.connect(config.getEmailHost(), config.getEmailLogin(), config.getEmailPassword());

      log.info("Successfully connected to email server");

      return store;

    } catch (NoSuchProviderException ex) {
      log.error("No provider for protocol: " + config.getEmailProtocol());
      ex.printStackTrace();
    } catch (MessagingException ex) {
      log.error("Could not connect to the message store");
      ex.printStackTrace();
    }
    return null;
  }

  public void autoFetchAndSendEmails(User user, Store store) {
    // opens the inbox folder
    Folder inbox = null;
    try {
      inbox = store.getFolder("INBOX");
      inbox.open(Folder.READ_WRITE);
    } catch (MessagingException e) {
      log.error("Could not open the folder");
      e.printStackTrace();
    }

    String text;
    // fetches new messages from server
    Message[] messages;
    try {
      //Фильтруем по отправителю
      messages =
          inbox != null ? inbox.search(new FromStringTerm(config.getSender())) : new Message[0];

      if (messages.length == 0) {
        //log.info("No new messages found in inbox.");
      } else {
        boolean messageSent = false; // флаг, отвечающий за отправку сообщения
        for (Message message : messages) {
          MimeMessage mimeMessage = (MimeMessage) message;
            String subject = mimeMessage.getSubject();
            String toList = mailMessageBuilder.parseAddresses(message.getRecipients(MimeMessage.RecipientType.TO));
            String ccList = mailMessageBuilder.parseAddresses(message.getRecipients(MimeMessage.RecipientType.CC));
            String sentDate = message.getSentDate().toString();

            // Считываем текст письма
            String messageContent = MailMessageBuilder.getString(message);
            // Парсим сообщение
            messageContent = mailMessageBuilder.parseMessageContent(messageContent);
            // Составляем отправляемый текст
            text = mailMessageBuilder.buildMessage(messageContent, subject, toList, ccList, sentDate);
            // Ищем упоминание адресата в тексте письма
            String result = mailMessageBuilder.findBotField(messageContent);

            // Маршрутизация
            long groupId = 0;
            if (result != null) {
              groupId = Long.parseLong(result);
            }
            if (groupId != 0) {
              Optional<User> optionalUser = userRepository.findById(groupId);
              if (optionalUser.isPresent()) {
                if (user.getId() == groupId) {
                  // Отправка сообщения в Telegram
                  dispellyTelegramBot.sendTelegramMessage(text, user);
                  messageSent = true; // сообщение было отправлено
                  // Пометка сообщения на удаление
                  message.setFlag(Flags.Flag.DELETED, true);
                } else {
                  // Пометка сообщения как непрочитанного
                  message.setFlag(Flags.Flag.SEEN, false);
                }
              } else {
                // Пометка сообщения как непрочитанного
                message.setFlag(Flags.Flag.SEEN, false);
              }
            }
            if (!messageSent) {
              // Если не было отправленных сообщений
              //log.info("No messages found in inbox for " + user.getName());
              message.setFlag(Flags.Flag.SEEN, false);
            }
        }
      }
      inbox.close(false);
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
