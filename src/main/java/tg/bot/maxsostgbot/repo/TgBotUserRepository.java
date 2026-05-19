package tg.bot.maxsostgbot.repo;

import org.springframework.data.repository.CrudRepository;
import tg.bot.maxsostgbot.repo.model.TgBotUser;

import java.util.Optional;

//Spring Boot автоматически создает все компоненты необходимые для работы с БД. Для этого нужно добавить spring-boot starter'ы в Pom.xml и реализовать Interface
//который будет наследоваться От класс springframework'а CrudRepository + описать сущность с который мы будем работать (TgBotUser.class)
public interface TgBotUserRepository extends CrudRepository<TgBotUser, Long> {
    Optional<TgBotUser> findByUserNameIgnoreCase(String username);
}
