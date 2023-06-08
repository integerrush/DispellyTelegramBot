package com.example.dispellybot.components;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

public interface BotCommands {
  List<BotCommand> LIST_OF_COMMANDS = List.of(
      new BotCommand("/start", "start bot"),
      new BotCommand("/help", "bot info"),
      new BotCommand("/connect", "connect to the message store"),
      new BotCommand("/fetchmail", "fetch a new email"),
      new BotCommand("/autofetch", "activate autofetch"),
      new BotCommand("/cancel", "cancel autofetch")
  );

  String HELP_TEXT = "This bot will help you to receive the message from your inbox. \n\n" +
      "1. /connect to the message store. 2. Choose /fetchmail or /autofetch 3. /cancel autofetch \n\n" +
      "The following commands are available to you:\n\n" +
      "/start - start bot interaction\n" +
      "/help - help menu\n" +
      "/connect - connect to message store\n" +
      "/fetchmail - fetch a new email from inbox manually\n" +
      "/autofetch - activate automatic fetch\n" +
      "/cancel - deactivate automatic fetch";
}
