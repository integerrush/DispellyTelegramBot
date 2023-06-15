package com.example.dispellybot.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@Getter
@PropertySource("config.properties")
public class BotConfig {
  @Value("${bot.name}") String botName;
  @Value("${bot.token}") String token;
  @Value("${bot.emailHost}") String emailHost;
  @Value("${bot.emailLogin}") String emailLogin;
  @Value("${bot.emailPassword}") String emailPassword;
  @Value("${bot.emailProtocol}") String emailProtocol;
  @Value("${bot.emailPort}") String emailPort;
  @Value("${bot.field}") String field;
  @Value("${bot.timer}") String time;
  @Value("${bot.delay}") String delay;
  @Value("${bot.sender}") String sender;
}
