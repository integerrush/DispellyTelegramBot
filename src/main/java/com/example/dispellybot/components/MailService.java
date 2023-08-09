package com.example.dispellybot.components;

import com.example.dispellybot.DispellyTelegramBot;
import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.User;
import com.example.dispellybot.database.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class MailService {

    private static Timer timer;
    final BotConfig config;
    private final UserRepository userRepository;
    private final MailMessageBuilder mailMessageBuilder;
    private final DispellyTelegramBot dispellyTelegramBot;

    public MailService(BotConfig config, UserRepository userRepository,
                       MailMessageBuilder mailMessageBuilder,
                       DispellyTelegramBot dispellyTelegramBot) {
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

    public static Timer getTimer() {
        if (timer == null) {
            timer = new Timer();
        }
        return timer;
    }

    /**
     * Starts automatic check for new messages in inbox.
     */
    public void mailTimerTask(Store store) {
        long delay = Long.parseLong(config.getDelay());
        long period = Long.parseLong(config.getTime());

        getTimer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                List<User> users = userRepository.findAll();
                for (User user : users) {
                    try {
                        if (store.isConnected()) {
                            // Проверка наличия новых сообщений в почте и их обработка
                            autoFetchAndSendEmails(user, store);
                        } else {
                            reconnectAndFetch(user);
                        }
                    } catch (Exception ex) {
                        reconnectAndFetch(user);
                    }
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
            log.error("No provider for protocol: {}", config.getEmailProtocol(), ex);
        } catch (MessagingException ex) {
            log.error("Could not connect to the message store", ex);
        }
        return null;
    }

    /**
     * Reconnects to the message store and calls autoFetchAndSendEmails method.
     */
    private void reconnectAndFetch(User user) {
        Store storeCopy = connectToMessageStore();
        log.info("Reconnected to the message store.");
        autoFetchAndSendEmails(user, storeCopy);
    }

    public void autoFetchAndSendEmails(User user, Store store) {
        // opens the inbox folder
        Folder inbox = null;
        try {
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
        } catch (MessagingException ex) {
            log.error("Could not open the folder", ex);
        }

        String text;
        // fetches new messages from server
        Message[] messages;
        try {
            //Фильтруем по отправителю
            messages = inbox != null ? inbox.getMessages() : new Message[0];

            if (messages.length == 0) {
                log.info("No new messages found in inbox.");
            } else {
                log.info("Found {} messages", messages.length);
                int sendMessageCounter = 0;
                boolean messageSent = false; // флаг, отвечающий за отправку сообщения
                for (Message message : messages) {
                    MimeMessage mimeMessage = (MimeMessage) message;
                    String fromAddresses = mailMessageBuilder.parseAddresses(mimeMessage.getFrom());
                    Boolean containsAddress = Optional.ofNullable(fromAddresses)
                        .map(s -> s.contains(config.getSender()))
                        .orElse(Boolean.FALSE);
                    if (Boolean.FALSE.equals(containsAddress)) {
                        log.debug("From: {}; Doesn't contains required address: {}", fromAddresses,
                            config.getSender());
                        continue;
                    }
                    String subject = mimeMessage.getSubject();
                    String sentDate = message.getSentDate().toString();

                    // Считываем текст письма
                    String messageContent = MailMessageBuilder.getString(message);
                    // Парсим сообщение
                    messageContent = mailMessageBuilder.parseMessageContent(messageContent);
                    // Составляем отправляемый текст
                    text = mailMessageBuilder.buildMessage(messageContent, subject, sentDate);
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
                                // Отправка измененного сообщения в Telegram
                                dispellyTelegramBot.sendTelegramMessage(
                                    mailMessageBuilder.modifyMessageContent(text), user);
                                messageSent = true; // сообщение было отправлено
                                sendMessageCounter++;
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
                log.info("Messages sent: {}", sendMessageCounter);
            }
            inbox.close(false);
        } catch (NoSuchProviderException ex) {
            log.error("No provider for protocol: {}", config.getEmailProtocol(), ex);
        } catch (MessagingException ex) {
            log.error("Could not connect to the message store", ex);
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
