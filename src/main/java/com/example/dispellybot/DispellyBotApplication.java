package com.example.dispellybot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DispellyBotApplication {

  public static void main(String[] args) {
    SpringApplication.run(DispellyBotApplication.class, args);
  }

}
