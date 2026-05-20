package tg.bot.maxsostgbot.service;

import tg.bot.maxsostgbot.repo.model.AutoBroadcastEntity;

import java.util.List;
import java.util.Optional;

public interface BroadcastMessageService {
    AutoBroadcastEntity findMessage();
    Optional<AutoBroadcastEntity> updateMessage(String message);
    Optional<AutoBroadcastEntity> updateTime(Long timeInMillis);
    void deactivateAutoBroadcast();
}
