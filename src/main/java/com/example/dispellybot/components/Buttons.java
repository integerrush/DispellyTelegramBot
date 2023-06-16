package com.example.dispellybot.components;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class Buttons {
  private static final InlineKeyboardButton START_BUTTON = new InlineKeyboardButton("Start");

  public static InlineKeyboardMarkup inlineMarkup() {
    START_BUTTON.setCallbackData("/start");

    List<InlineKeyboardButton> rowInline = List.of(START_BUTTON);
    List<List<InlineKeyboardButton>> rowsInLine = List.of(rowInline);

    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    markupInline.setKeyboard(rowsInLine);

    return markupInline;
  }
}
