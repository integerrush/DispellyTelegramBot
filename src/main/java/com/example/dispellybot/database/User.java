package com.example.dispellybot.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Entity(name = "telegram_user")
public class User {

  @Id
  private long id; //BigInt
  private String name; //Text
}
