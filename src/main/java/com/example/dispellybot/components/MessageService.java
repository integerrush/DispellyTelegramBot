package com.example.dispellybot.components;

import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.BotMessage;
import com.example.dispellybot.database.BotMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private final BotConfig config;
  private final MailMessageParser mailMessageParser;

  private final BotMessageRepository botMessageRepository;

  private Store store;
  private final ConcurrentHashMap<String, Folder> folderMap = new ConcurrentHashMap<>();

  /**
   * @param predicate для поиска в store
   * @return сообщения, которые необходимо отправить
   */
  public List<BotMessage> getMessagesForSent(Predicate<Message> predicate) {
    store = actualStore();
    return findNotSentMessages(predicate);
  }

  public void saveAll(List<BotMessage> messages) {
    botMessageRepository.saveAll(messages);
  }

  private Store actualStore() {
    if (Objects.nonNull(store) && store.isConnected()) {
      return store;
    }

    return connectToMessageStore(config);
  }

  private List<BotMessage> findNotSentMessages(Predicate<Message> predicate) {
    try {
      Folder inbox = openFolder("INBOX");
      List<BotMessage> ms = Arrays.stream(inbox.getMessages()).filter(predicate)
              .map(mailMessageParser::parse)
              .collect(Collectors.toList());
      ms = new ArrayList<>(ms);
      ms.addAll(botMessageRepository.findAllBySentToTelegramIsFalse());
      return ms;
    } catch (Exception ex) {
      log.error("Can't read messages from folder.", ex);
    }

    return Collections.emptyList();
  }

  private Folder openFolder(String inbox) {
    return folderMap.compute(inbox, (s, folder) -> {
      try {
        if (Objects.nonNull(folder) && folder.isOpen()) {
          folder.close(true);
        }
        Folder f = store.getFolder("INBOX");
        f.open(Folder.READ_WRITE);
        return f;
      } catch (Exception ex) {
        log.error("Can't open folder {}.", inbox, ex);
      }
      return null;
    });
  }

  /**
   * Connects to the message store.
   */
  private static Store connectToMessageStore(BotConfig config) {
    Properties properties = getServerProperties(config.getEmailHost(), config.getEmailPort());
    Session session = javax.mail.Session.getInstance(properties);

    try {
      Store s = session.getStore("imap");
      s.connect(config.getEmailHost(), config.getEmailLogin(), config.getEmailPassword());

      log.info("Successfully connected to email server");

      return s;
    } catch (NoSuchProviderException ex) {
      log.error("No provider for protocol: {}", config.getEmailProtocol(), ex);
    } catch (MessagingException ex) {
      log.error("Could not connect to the message store", ex);
    }

    return null;
  }

  /**
   * Returns a Properties object which is configured for a IMAP server
   */
  private static Properties getServerProperties(String host, String port) {
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
