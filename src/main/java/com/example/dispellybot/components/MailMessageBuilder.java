package com.example.dispellybot.components;

import com.example.dispellybot.config.BotConfig;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MailMessageBuilder {

  final BotConfig config;

  private static ContentFormat contentFormat;

  public MailMessageBuilder(BotConfig config) {
    this.config = config;
  }

  /**
   * Returns a messageContent from the email
   */
  public static String getString(Message message) throws MessagingException {
    String messageContent = "";
    // проверяем тип содержимого сообщения
    if (message.getContentType().toLowerCase().startsWith("multipart/")) {
      // читаем содержимое сообщения типа MimeMultipart
      try {
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        for (int i = 0; i < multipart.getCount(); i++) {
          BodyPart part = multipart.getBodyPart(i);
          messageContent = bodyPartParser(part);
        }
      } catch (Exception ex) {
        log.error("[Error downloading content]");
        messageContent = "[Error downloading content]";
        ex.printStackTrace();
      }
    }
    return messageContent;
  }

  private static String bodyPartParser(BodyPart part) throws MessagingException, IOException {
    String messageContent = "";
    if (part.getDataHandler().getContent() instanceof MimeMultipart) {
      MimeMultipart multipart1 = (MimeMultipart) part.getDataHandler().getContent();
      for (int j = 0; j < multipart1.getCount(); j++) {
        BodyPart part1 = multipart1.getBodyPart(j);
        messageContent += bodyPartParser(part1);
      }
    } else {
      if (part.getContentType().toLowerCase().startsWith("text/plain")) {
        messageContent = (String) part.getContent();
        contentFormat = ContentFormat.TEXT_PLAIN;
      } else if (part.getContentType().toLowerCase().startsWith("text/html")) {
        messageContent = (String) part.getContent();
        contentFormat = ContentFormat.TEXT_HTML;
      } else if (part.getContentType().toLowerCase().startsWith("text/xml")) {
        messageContent = (String) part.getContent();
        contentFormat = ContentFormat.TEXT_XML;
      } else {
        // обрабатываем другие типы содержимого
      }
    }
    return messageContent;
  }


  /**
   * Returns a list of addresses in String format separated by comma
   */
  public String parseAddresses(Address[] address) {
    String listAddress = "";

    if (address != null) {
      for (int i = 0; i < address.length; i++) {
        listAddress += address[i].toString() + ", ";
      }
    }
    if (listAddress.length() > 1) {
      listAddress = listAddress.substring(0, listAddress.length() - 2);
    }

    return listAddress;
  }

  /**
   * Returns parsed sender
   */
  public String parseSender(String sender) {
    Pattern pattern = Pattern.compile("\\b[\\w.%-]+@[\\w.-]+\\.[a-zA-Z]{2,4}\\b");
    Matcher matcher = pattern.matcher(sender);
    if (matcher.find()) {
      sender = matcher.group();
    }
    return sender;
  }

  /**
   * Builds a message to send
   */
  public String buildMessage(String messageContent, String subject, String sentDate) {
    parseMessageContent(messageContent);

    if (contentFormat == ContentFormat.TEXT_HTML) {
      messageContent = parseMessageContent(messageContent);
    }

    return "Subject: " + subject + "\n\n" + messageContent + "\n\n" + sentDate;
  }

  /**
   * Parses the message <html> </html> content
   */
  public String parseMessageContent(String messageContent) {
    if (contentFormat == ContentFormat.TEXT_HTML) {
      Document document = Jsoup.parse(messageContent);
      Optional<Element> optionalLastP =
          Optional.ofNullable(document.select("p:last-child").first());
      if (optionalLastP.isPresent()) {
        Node nextSibling = optionalLastP.get().nextSibling();
        if (nextSibling instanceof TextNode) {
          nextSibling.remove();
        }
      }
      messageContent = document.text();
    }

    return messageContent;

  }

  /**
   * Finds bot field value in the parsed message content
   */
  public String findBotField(String messageContent) {
    String result = null;
    Pattern pattern = Pattern.compile(config.getField() + ": .-?\\d+");
    Matcher matcher = pattern.matcher(messageContent);
    if (matcher.find()) {
      result = matcher.group().replace(config.getField() + ": ", "");
    }
    return result;
  }

  /**
   * Modifies message content before sending
   */
  public String modifyMessageContent(String messageContent) {
    return messageContent.replace(config.getField() + ": ",  "").replace(findBotField(messageContent), "")
        .replace("Текст:", "\n");
  }
}
