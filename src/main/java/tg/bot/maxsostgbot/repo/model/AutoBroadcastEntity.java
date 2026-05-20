package tg.bot.maxsostgbot.repo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.experimental.Accessors;

@Entity(name = "autoBroadcastEntity")
@Data
@Accessors(chain = true)
public class AutoBroadcastEntity {
    @Id
    private Long messageId;

    private String message;

    private Long intervalInMillis;

    private Boolean active;
}
