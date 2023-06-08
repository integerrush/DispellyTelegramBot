package com.example.dispellybot.components;

import com.example.dispellybot.config.BotConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
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
  public static String getString(Message message)
      throws MessagingException {
    String messageContent = "";
    // проверяем тип содержимого сообщения
    if (message.getContentType().toLowerCase().startsWith("multipart/")) {
      // читаем содержимое сообщения типа MimeMultipart
      try {
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        for (int i = 0; i < multipart.getCount(); i++) {
          BodyPart part = multipart.getBodyPart(i);
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
      } catch (Exception ex) {
        log.error("[Error downloading content]");
        messageContent = "[Error downloading content]";
        ex.printStackTrace();
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
   * Builds a message to send
   */
  public String buildMessage(String messageContent, String subject, String toList, String ccList,
                             String sentDate) {
    parseMessageContent(messageContent);

    if (contentFormat == ContentFormat.TEXT_HTML) {
      messageContent = messageContent.replaceAll("<.*?>", "\n");
    }

    return "Subject: " + subject + "\n" + "From: " + toList + "\n" + ccList + "\n" +
        messageContent + "\n\n" + sentDate;
  }

  /**
   * Parses the message content
   */
  public String parseMessageContent(String messageContent) {
    String result = null;
    String regexp = null;
    if (contentFormat == ContentFormat.TEXT_HTML) {
      regexp = config.getField() + ": (.+?)</div>";
    }
    if (contentFormat == ContentFormat.TEXT_PLAIN) {
      regexp = config.getField() + ": (.+)";
    }
    if (contentFormat == ContentFormat.TEXT_XML) {
      regexp = config.getField() + ">(.+?)<";
    }

    Pattern pattern = Pattern.compile(regexp);
    Matcher matcher = pattern.matcher(messageContent);
    if (matcher.find()) {
      result = matcher.group(1);
    }

    if ((result == null) && (contentFormat == ContentFormat.TEXT_HTML)) {
      regexp = config.getField() + ": (.+?)\\s(.+?)<\\/div>";

      pattern = Pattern.compile(regexp);
      matcher = pattern.matcher(messageContent);
      if (matcher.find()) {
        result = matcher.group(1);
      }
    }

    if ((result == null) && (contentFormat == ContentFormat.TEXT_PLAIN)) {
      regexp = config.getField() + ": (.+?)\\s(.+?)";

      pattern = Pattern.compile(regexp);
      matcher = pattern.matcher(messageContent);
      if (matcher.find()) {
        result = matcher.group(1);
      }
    }
    return result;
  }
}
