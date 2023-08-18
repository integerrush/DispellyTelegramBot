package com.example.dispellybot.components;

import com.example.dispellybot.database.BotGroup;
import com.example.dispellybot.database.BotGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotGroupService {

    private final BotGroupRepository botGroupRepository;

    public BotGroup findById(long groupId) {
        return botGroupRepository.findById(groupId).orElse(null);
    }

    /**
     * @param botGroup группа, которую стартуем.
     * @return количество пропущенных сообщений с последнего старта.
     */
    public long startGroup(BotGroup botGroup) {
        if (Objects.isNull(botGroup)) {
            return 0;
        }

        botGroup = botGroupRepository.findById(botGroup.getId()).orElse(botGroup);
        botGroup.setRunning(true);
        long missedMessages = botGroup.getMissedMessages();
        botGroup.setMissedMessages(0);
        botGroupRepository.saveAndFlush(botGroup);

        return missedMessages;
    }

    public void stopGroup(BotGroup botGroup) {
        if (Objects.isNull(botGroup)) {
            return;
        }

        botGroup = botGroupRepository.findById(botGroup.getId()).orElse(botGroup);
        botGroup.setRunning(false);
        botGroup.setMissedMessages(0);
        botGroupRepository.saveAndFlush(botGroup);
    }

    /**
     * Добавляет пропущенные сообщения к группе, если группа есть в БД и не запущена.
     *
     * @param botGroup группа которая может пропутить сообщение
     * @return сколько пропущенных сообщений получилось.
     */
    public long addMissedMessage(BotGroup botGroup) {
        if (Objects.isNull(botGroup)) {
            return 0;
        }
        Optional<BotGroup> groupFromDB = botGroupRepository.findById(botGroup.getId());
        if (groupFromDB.isEmpty()) {
            return 0;
        }

        botGroup = groupFromDB.get();

        botGroup.setMissedMessages(botGroup.getMissedMessages() + 1);
        botGroupRepository.saveAndFlush(botGroup);
        return botGroup.getMissedMessages();
    }

}
