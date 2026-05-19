package tg.bot.maxsostgbot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tg.bot.maxsostgbot.config.BotConfig;
import tg.bot.maxsostgbot.repo.TgBotUserRepository;
import tg.bot.maxsostgbot.repo.model.TgBotUser;
import tg.bot.maxsostgbot.service.TgBotUserService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static tg.bot.maxsostgbot.util.Utils.*;

//слой реализации бинзнес логики с внутренними компонентами
@Service
public class TgBotUserServiceImpl implements TgBotUserService {

    private final TgBotUserRepository userRepo;
    private final BotConfig botConfig;


    public TgBotUserServiceImpl(@Autowired TgBotUserRepository tgBotUserRepository,
                                @Autowired BotConfig botConfig) {
        this.userRepo = tgBotUserRepository;
        this.botConfig = botConfig;
    }

    @Override
    public TgBotUser save(Long chatId, String firstName, String lastName, String userName, TgBotUser.Status status,
                          OffsetDateTime resumptionNotificationTime, OffsetDateTime registrationTime, List<String> adminUsernames) {
        TgBotUser tgBotUser = new TgBotUser()
                .setChatId(chatId)
                .setUserName(userName)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setStatus(status)
                .setRole(adminUsernames.contains(userName) ? TgBotUser.Role.ADMIN : TgBotUser.Role.SUBSCRIBER)
                .setRegistrationTime(registrationTime)
                .setResumptionNotificationTime(resumptionNotificationTime);
        return userRepo.save(tgBotUser);
    }

    @Override
    public Optional<TgBotUser> findById(Long chatId) {
        return userRepo.findById(chatId);
    }

    @Override
    public TgBotUser update(TgBotUser tgBotUser) {
        return userRepo.save(tgBotUser);
    }

    @Override
    public Stream<TgBotUser> findAll() {
        return StreamSupport.stream(userRepo.findAll().spliterator(), false);
    }

    @Override
    public Optional<TgBotUser> findByUsername(String username) {
        return userRepo.findByUserNameIgnoreCase(username);
    }

    @Override
    public String blockUser(String username) {
        if (botConfig.adminUsernames().contains(username)) {
            return CANNOT_BLOCK_ROOT_ADMIN;
        }
        Optional<TgBotUser> tgBotUserOptional = findByUsername(username);

        if (tgBotUserOptional.isPresent()) {
            update(tgBotUserOptional.get().setStatus(TgBotUser.Status.BLOCKED));
            return USER_HAS_BEEN_BLOCKED;
        }
        return USER_NOT_FOUND;
    }

    @Override
    public String unblockUser(String username) {
        Optional<TgBotUser> tgBotUserOptional = findByUsername(username);

        if (tgBotUserOptional.isPresent()) {
            update(tgBotUserOptional.get().setStatus(TgBotUser.Status.SUB));
            return USER_HAS_BEEN_UNBLOCKED;
        }
        return USER_NOT_FOUND;
    }
}
