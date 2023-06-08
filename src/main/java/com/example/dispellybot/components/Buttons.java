package com.example.dispellybot.components;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class Buttons {
  private static final InlineKeyboardButton START_BUTTON = new InlineKeyboardButton("Start");
  private static final InlineKeyboardButton HELP_BUTTON = new InlineKeyboardButton("Help");
  private static final InlineKeyboardButton CONNECT_BUTTON = new InlineKeyboardButton("Connect to Email");
  private static final InlineKeyboardButton FETCHMAIL_BUTTON = new InlineKeyboardButton("FetchMail");
  private static final InlineKeyboardButton AUTOFETCH_BUTTON = new InlineKeyboardButton("AutoFetch");
  private static final InlineKeyboardButton CANCEL_BUTTON = new InlineKeyboardButton("Cancel AutoFetch");

  public static InlineKeyboardMarkup inlineMarkup() {
    START_BUTTON.setCallbackData("/start");
    HELP_BUTTON.setCallbackData("/help");
    CONNECT_BUTTON.setCallbackData("/connect");
    FETCHMAIL_BUTTON.setCallbackData("/fetchMail");
    AUTOFETCH_BUTTON.setCallbackData("/autoFetch");
    CANCEL_BUTTON.setCallbackData("/cancel");


    List<InlineKeyboardButton> rowInline = List.of(START_BUTTON, HELP_BUTTON, CONNECT_BUTTON, FETCHMAIL_BUTTON,
        AUTOFETCH_BUTTON, CANCEL_BUTTON);
    List<List<InlineKeyboardButton>> rowsInLine = List.of(rowInline);

    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    markupInline.setKeyboard(rowsInLine);

    return markupInline;
  }
}
