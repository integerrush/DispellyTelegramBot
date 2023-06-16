package com.example.dispellybot.components;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

public interface BotCommands {
  List<BotCommand> LIST_OF_COMMANDS = List.of(
      new BotCommand("/start", "start bot")
  );

}
