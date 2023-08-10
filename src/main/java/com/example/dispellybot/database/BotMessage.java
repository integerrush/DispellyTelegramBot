package com.example.dispellybot.database;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Getter
@Setter
@Entity(name = "bot_message")
public class BotMessage {
    @Id
    @GeneratedValue
    private long id;
    private long groupId;
    private String subject;
    private String text;
    private String date;
    private boolean sentToTelegram = false;
}
