package tg.bot.maxsostgbot.service;

import java.util.List;

public interface BroadcastService {
    void runAsynchronousBroadcast(String message, List<Long> chatIds);
}
