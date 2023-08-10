package com.example.dispellybot.components;

import com.example.dispellybot.config.BotConfig;
import com.example.dispellybot.database.BotMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MailMessageParser {

    private final String groupField;

    public MailMessageParser(BotConfig config) {
        this.groupField = config.getField();
    }

    public BotMessage parse(Message message) {
        StringBuilder messageBuilder = new StringBuilder();
        BotMessage botMessage = new BotMessage();

        try {
            botMessage.setSubject(message.getSubject());
            botMessage.setDate(message.getSentDate().toString());

            bodyPartParser(message, messageBuilder);
            String messageBuilderString = messageBuilder.toString();

            botMessage.setGroupId(groupId(messageBuilderString));
            botMessage.setText(replaceFields(messageBuilderString));

            message.setFlag(Flags.Flag.SEEN, true);
            message.setFlag(Flags.Flag.DELETED, true);
        } catch (Exception ex) {
            log.error("Can't parse message", ex);
        }

        return botMessage;
    }

    private long groupId(String message) {
        String botField = findBotField(message);
        try {
            return Long.parseLong(botField);
        } catch (Exception ex) {
            log.error("Can't parse group id {}", botField);
        }

        return 0L;
    }

    private void bodyPartParser(Part part, StringBuilder messageBuilder) throws MessagingException, IOException {
        if (part.getDataHandler().getContent() instanceof MimeMultipart) {
            MimeMultipart multipart1 = (MimeMultipart) part.getDataHandler().getContent();
            for (int j = 0; j < multipart1.getCount(); j++) {
                BodyPart part1 = multipart1.getBodyPart(j);
                bodyPartParser(part1, messageBuilder);
            }
        } else {
            if (part.getContentType().toLowerCase().startsWith("text/html")) {
                messageBuilder.append(removeCC(part.getContent().toString()));
            } else if (part.getContentType().toLowerCase().startsWith("text/plain")
                    || part.getContentType().toLowerCase().startsWith("text/xml")) {

                messageBuilder.append(part.getContent());
            }
        }
    }

    private String removeCC(String messageContent) {
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

        return messageContent;
    }

    private String findBotField(String messageContent) {
        String result = null;
        Pattern pattern = Pattern.compile(groupField + ": .-?\\d+");
        Matcher matcher = pattern.matcher(messageContent);
        if (matcher.find()) {
            result = matcher.group().replace(groupField + ": ", "");
        }
        return result;
    }

    private String replaceFields(String messageContent) {
        String preparedMessage = messageContent;
        preparedMessage = preparedMessage.replace(groupField + ": ", "")
                .replace("Текст: ", "\n");

        String botField = findBotField(messageContent);
        if (StringUtils.isNotEmpty(botField)) {
            preparedMessage = preparedMessage.replace(botField, "");
        }

        return preparedMessage;
    }
}
