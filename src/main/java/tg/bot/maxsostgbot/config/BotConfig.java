package tg.bot.maxsostgbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bot")
public record BotConfig (
        String name,

        String token,

        List<String> adminUsernames
) {}