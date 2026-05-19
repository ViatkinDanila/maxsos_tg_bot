package tg.bot.maxsostgbot.service;

import tg.bot.maxsostgbot.repo.model.TgBotUser;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface TgBotUserService {

    TgBotUser save(Long chatId, String firstName, String lastName, String userName, TgBotUser.Status status,
                   OffsetDateTime resumptionNotificationTime, OffsetDateTime registrationTime, List<String> adminUsernames);

    Optional<TgBotUser> findById(Long chatId);

    TgBotUser update(TgBotUser tgBotUser);

    Stream<TgBotUser> findAll();

    Optional<TgBotUser> findByUsername(String username);

    String blockUser(String username);

    String unblockUser(String username);
}
