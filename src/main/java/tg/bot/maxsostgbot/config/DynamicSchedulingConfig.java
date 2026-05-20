package tg.bot.maxsostgbot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import tg.bot.maxsostgbot.repo.AutoBroadcastRepository;
import tg.bot.maxsostgbot.repo.model.AutoBroadcastEntity;
import tg.bot.maxsostgbot.repo.model.TgBotUser;
import tg.bot.maxsostgbot.service.BroadcastService;
import tg.bot.maxsostgbot.service.TgBotUserService;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Configuration
public class DynamicSchedulingConfig implements SchedulingConfigurer {

    private final BroadcastService broadcastServiceService;
    private final TgBotUserService userService;
    private final AutoBroadcastRepository autoBroadcastRepository;

    public DynamicSchedulingConfig(@Autowired BroadcastService broadcastServiceService, @Autowired TgBotUserService userService,
                                   @Autowired AutoBroadcastRepository autoBroadcastRepository) {
        this.broadcastServiceService = broadcastServiceService;
        this.userService = userService;
        this.autoBroadcastRepository = autoBroadcastRepository;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                () -> {
                    Optional<AutoBroadcastEntity> abe = getAutoBroadEntity();
                    if (abe.isPresent() && abe.get().getActive()) {
                        List<Long> userChatIds = userService.findAll()
                                .filter(tgBotUser -> TgBotUser.Role.SUBSCRIBER.equals(tgBotUser.getRole())
                                        && OffsetDateTime.now().isAfter(tgBotUser.getResumptionNotificationTime())
                                        && !TgBotUser.Status.BLOCKED.equals(tgBotUser.getStatus()))
                                .map(TgBotUser::getChatId).toList();

                        // 2. Передаем задачу асинхронному сервису.
                        // Метод вернет управление СРАЗУ ЖЕ, не дожидаясь отправки сообщений!
                        if (!Objects.isNull(abe.get().getMessage()) && !userChatIds.isEmpty()) {
                            broadcastServiceService.runAsynchronousBroadcast(abe.get().getMessage(), userChatIds);
                        }
                    }
                },
                triggerContext -> {
                    Optional<AutoBroadcastEntity> abe = getAutoBroadEntity();
                    Instant lastExecution = triggerContext.lastActualExecution();
                    if (lastExecution == null) {
                        lastExecution = Instant.now();
                    }
                    if (abe.isPresent()) {
                        long delayInMilliseconds = abe.get().getIntervalInMillis();
                        return lastExecution.plus(Duration.ofMillis(delayInMilliseconds));
                    }
                    return lastExecution.plus(Duration.ofMillis(60000));
                }
        );
    }

    private Optional<AutoBroadcastEntity> getAutoBroadEntity() {
        List<AutoBroadcastEntity> abel = StreamSupport.stream(autoBroadcastRepository.findAll().spliterator(), false).toList();
        return abel.isEmpty() ? Optional.empty() : Optional.of(abel.getFirst());
    }
}
