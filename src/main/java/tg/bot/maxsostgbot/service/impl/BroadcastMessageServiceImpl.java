package tg.bot.maxsostgbot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tg.bot.maxsostgbot.controller.TelegramBotController;
import tg.bot.maxsostgbot.repo.AutoBroadcastRepository;
import tg.bot.maxsostgbot.repo.model.AutoBroadcastEntity;
import tg.bot.maxsostgbot.service.BroadcastMessageService;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class BroadcastMessageServiceImpl implements BroadcastMessageService {

    @Autowired
    private AutoBroadcastRepository autoBroadcastRepository;

    @Override
    public AutoBroadcastEntity findMessage() {
        List<AutoBroadcastEntity> abes = StreamSupport.stream(autoBroadcastRepository.findAll().spliterator(), false).toList();
        if (abes.isEmpty()) {
            return new AutoBroadcastEntity().setMessageId(1L).setActive(true).setIntervalInMillis(60000L);
        }
        return abes.getFirst();
    }

    @Override
    public Optional<AutoBroadcastEntity> updateMessage(String message) {
        AutoBroadcastEntity abe = findMessage();
        return Optional.of(autoBroadcastRepository.save(abe.setMessage(message)));
    }

    @Override
    public Optional<AutoBroadcastEntity> updateTime(Long timeInMillis) {
        AutoBroadcastEntity abe = findMessage();
        return Optional.of(autoBroadcastRepository.save(abe.setIntervalInMillis(timeInMillis)));
    }

    @Override
    public void deactivateAutoBroadcast() {
        AutoBroadcastEntity abe = findMessage();
        autoBroadcastRepository.save(abe.setActive(false));
    }
}
