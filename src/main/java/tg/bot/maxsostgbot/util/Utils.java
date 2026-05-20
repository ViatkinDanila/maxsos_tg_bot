package tg.bot.maxsostgbot.util;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//класс для работы с текстовой(текст от пользователя tg ботом) редакцией
public class Utils {


    private static final Pattern USERNAME_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{5,32})");


    public static final String ADMIN_REGISTRATION_TEXT = "%s, вы являетесь админом бота! Доступные комманды и описанием перечислены в Menu.";
    public static final String USER_REGISTRATION_TEXT = "%s, вы подписались на новостную рассылку! Доступные комманды и описанием перечислены в Menu.";
    public static final String ADMIN_FORGET_TYPE_BROADCAST_TEXT = "Вы не ввели сообщение! После команды %s через пробел напишите сообщение которое хотите отправить пользователям чат-бота.";
    public static final String ADMIN_FORGET_TYPE_USERNAME = "Вы не ввели имя пользователя! После команды %s через пробел напишите @username.";
    public static final String ADMIN_TYPE_WRONG_INTERVAL_NUMBER = "Введено неверное значаение для нового интервала! После /changeAutobroadcast через пробел введите число, которое будет отображать новый интервал автосообщений в минутах.";
    public static final String BROADCAST_SUCCESSFUL = "Вы разослали сообщение выше %d пользователям.";
    public static final String AUTOBROADCAST_MESSAGE_CHANGED = "Сообщение автоматической рассылки было успешно обновлено!";
    public static final String AUTOBROADCAST_INTERVAL_CHANGED = "Интервал автоматической рассылки был успешно обновлен!";
    public static final String AUTOBROADCAST_IS_STOPPED = "Авторассылка приостановлена!";
    public static final String UNKNOWN_COMMAND = "%s комманда не поддерживается!";
    public static final String START_COMMAND_MISSING = "Для начала работы бота воспользуйтесь командой /start.";
    public static final String USER_NOT_FOUND = "Пользователь с ником @%s не пользовался ботом.";
    public static final String USER_HAS_BEEN_BLOCKED = "Пользователь с ником @%s был заблакирован.";
    public static final String USER_HAS_BEEN_UNBLOCKED = "Пользователь с ником @%s был разблакирован.";
    public static final String COMMAND_NOT_ALLOWED = "Данная доступна только администраторам чат-бота!";
    public static final String CANNOT_BLOCK_ROOT_ADMIN = "Пользователь с ником @%s не может быть заблакирован! Ты кого пытаешься оторвать от ботвы ?).";
    public static final String UNSUBSCRIBE_MESSAGE = "Оповещения отключены на ";
    public static final String USER_ALREADY_EXISTS = "Вы уже подписаны!";

    public static Optional<String> extractUsername(String text) {
        if (Objects.isNull(text) || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = USERNAME_PATTERN.matcher(text);

        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }

        String[] words = text.trim().split("\\s+");
        if (words.length > 1) {

            return Optional.of(words[1]);
        }
        return Optional.empty();
    }

    public static String extractCommand(String message) {
        int firstSpaceIndex = message.indexOf(" ");
        return (firstSpaceIndex != -1) ? message.substring(0, firstSpaceIndex) : message;
    }

    public static String getBroadcastMessage(String command, String message) {
        return message.replaceFirst("^" + command + "\\s*", "");
    }

    public static Long getMillisFromUserMessage(String command, String message) {
        String intervalString = getBroadcastMessage(command, message);
        return Long.parseLong(intervalString) * 60 * 1000;
    }
}
