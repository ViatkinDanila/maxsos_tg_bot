package tg.bot.maxsostgbot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tg.bot.maxsostgbot.controller.TelegramBotController;
import tg.bot.maxsostgbot.service.BroadcastService;

import java.util.List;

@Service
public class BroadcastServiceImpl implements BroadcastService{

    @Autowired
    private TelegramBotController telegramBotController;

    @Async
    @Override
    public void runAsynchronousBroadcast(String message, List<Long> chatIds) {
        chatIds.stream().forEach(chatId -> {
            try {
                telegramBotController.sendMessage(chatId, message);
            } catch (Exception e) {
            }
        });
    }
}
