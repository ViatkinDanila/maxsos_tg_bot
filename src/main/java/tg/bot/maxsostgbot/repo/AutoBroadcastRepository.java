package tg.bot.maxsostgbot.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tg.bot.maxsostgbot.repo.model.AutoBroadcastEntity;

@Repository
public interface AutoBroadcastRepository extends CrudRepository<AutoBroadcastEntity, Long> {
}
