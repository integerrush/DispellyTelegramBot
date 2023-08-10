package com.example.dispellybot.database;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
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
    @Column(length = 4000)
    private String subject;
    @Column(length = 10000)
    private String text;
    private String date;
    private boolean sentToTelegram = false;
}
