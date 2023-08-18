package com.example.dispellybot.database;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.*;

@Setter
@Getter
@Builder
@Entity(name = "bot_group")
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class BotGroup {

  @Id
  private long id;
  private String name;
  private long missedMessages;
  private boolean isRunning;
}
