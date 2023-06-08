package com.example.dispellybot.components;

import static com.example.dispellybot.DispellyTelegramBot.storeAndFolder;

import com.example.dispellybot.DispellyTelegramBot;
import com.example.dispellybot.database.User;
import com.example.dispellybot.database.UserRepository;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConditionalOnProperty(name = "scheduling.enabled", matchIfMissing = true)
@PropertySource("config.properties")
public class CronTask {

  @Autowired
  private DispellyTelegramBot dispellyTelegramBot;

  @Autowired
  private UserRepository userRepository;

  @Scheduled(cron = "${startcron.expression}", zone = "Europe/Moscow")
  public void readMail() {

    List<User> users = userRepository.findAll();
    for (User user : users) {
      long chatId = user.getId();
      String userName = user.getName();
      storeAndFolder = dispellyTelegramBot.connectToMessageStore(chatId, userName);
      dispellyTelegramBot.mailTimerTask(chatId, userName, storeAndFolder);
      log.info("Task cron: mailTimerTask");
    }
  }

  @Scheduled(cron = "${endcron.expression}", zone = "Europe/Moscow")
  public void stop() {
    List<User> users = userRepository.findAll();
    for (User user : users) {
      long chatId = user.getId();
      String userName = user.getName();
      dispellyTelegramBot.stopTimerTask(userName, chatId, storeAndFolder);
      log.info("Task cron: stopTimerTask");
    }
  }
}
