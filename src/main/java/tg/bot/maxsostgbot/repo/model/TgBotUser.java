package tg.bot.maxsostgbot.repo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

//Аннотация для Spring Boot'a для формирования таблицы в БД
@Entity(name = "tgBotUser")
//Аннотация из lomboka для автосоздание конструктора, getters , setters
@Data
@Accessors(chain = true)
public class TgBotUser {

    //Аннотация для Spring Boot'a задает первичный ключ сущьности из БД
    @Id
    private Long chatId;

    private String firstName;
    private String lastName;
    private String userName;
    private Status status;
    private Role role;
    private OffsetDateTime resumptionNotificationTime;
    private OffsetDateTime registrationTime;

    public enum Status {
        SUB, BLOCKED
    };

    public enum Role {
        SUBSCRIBER, ADMIN
    }
}
