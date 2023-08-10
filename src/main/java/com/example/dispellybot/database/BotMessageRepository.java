package com.example.dispellybot.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotMessageRepository extends JpaRepository<BotMessage, Long> {

    List<BotMessage> findAllBySentToTelegramIsFalse();
}
