package tg.bot.maxsostgbot.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import tg.bot.maxsostgbot.config.BotConfig;
import tg.bot.maxsostgbot.repo.model.TgBotUser;
import tg.bot.maxsostgbot.service.BroadcastMessageService;
import tg.bot.maxsostgbot.service.TgBotUserService;
import tg.bot.maxsostgbot.service.impl.TgBotUserServiceImpl;
import tg.bot.maxsostgbot.util.Utils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static tg.bot.maxsostgbot.util.Utils.*;

//слой реализации бинзнес логики(75% ВСЕЙ ЛОГИКИ ПРИЛОЖЕНИЯ) с компонентами внешней библиотеки telegrambots + точка в хода в приложение
@Component
public class TelegramBotController extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final TgBotUserService userService;
    private final BroadcastMessageService bms;
    //Хранятся названия кнопок для комманды /unsub
    private Map<String, Map<Long, String>> inlineKeyBoardButtonOffsets;


    public TelegramBotController(@Autowired BotConfig botConfig,
                                 @Autowired TgBotUserServiceImpl userService,
                                 @Autowired  BroadcastMessageService bms) {
        this.botConfig = botConfig;
        this.userService = userService;
        this.bms = bms;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);

        inlineKeyBoardButtonOffsets = new HashMap<>();
        inlineKeyBoardButtonOffsets.put("unsub_1_min", Map.of(1L, "1 минуту"));
        inlineKeyBoardButtonOffsets.put("unsub_3_min", Map.of(3L, "3 минуты"));
        inlineKeyBoardButtonOffsets.put("unsub_5_min", Map.of(5L, "5 минут"));
        inlineKeyBoardButtonOffsets.put("unsub_total", Map.of(Long.MAX_VALUE, "∞"));

        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/unsub", "Остановить уведомления"));
        commands.add(new BotCommand("/sub", "Подписаться на уведомления"));
        commands.add(new BotCommand("/info", "Получить личную информацию"));
        this.execute(new SetMyCommands(commands, new BotCommandScopeAllChatAdministrators(), null));
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            Long chatId;
            SendMessage message = new SendMessage();

            //проверка на наличик данных от телеграм сервера необходимых для работы бота (любой текст)
            if (update.hasMessage() && update.getMessage().hasText()) {
                chatId = update.getMessage().getChatId();
                message.setChatId(chatId.toString());

                //первое обращение к БД, ищем нашего пользователя по chatId
                Optional<TgBotUser> tgBotUserOptional = userService.findById(chatId);

                //извлекаем из запроса команду /info, /sub, /unsub, /block и тд. (написанный метод)
                String command = Utils.extractCommand(update.getMessage().getText());

                //проверка на блок клиента
                if (tgBotUserOptional.isPresent() && TgBotUser.Status.BLOCKED.equals(tgBotUserOptional.get().getStatus())) {
                    message.setText(String.format(USER_HAS_BEEN_BLOCKED, tgBotUserOptional.get().getUserName()));
                    execute(message);
                    return;
                }

                switch (command) {
                    case "/start": {
                        if (tgBotUserOptional.isEmpty()) {
                            TgBotUser tgBotUser = userService.save(chatId,
                                    update.getMessage().getFrom().getFirstName(),
                                    update.getMessage().getFrom().getLastName(),
                                    update.getMessage().getFrom().getUserName(),
                                    TgBotUser.Status.SUB,
                                    OffsetDateTime.now(),
                                    OffsetDateTime.now(),
                                    botConfig.adminUsernames());

                            List<BotCommand> commands;
                            BotCommandScopeChat botCommandScopeChat = new BotCommandScopeChat();
                            botCommandScopeChat.setChatId(chatId);

                            if (TgBotUser.Role.ADMIN.equals(tgBotUser.getRole())) {
                                message.setText(String.format(ADMIN_REGISTRATION_TEXT, tgBotUser.getFirstName()));
                                commands = Arrays.asList(
                                        new BotCommand("info", "Информация о пользователе (/info @username)"),
                                        new BotCommand("block", "Заблокировать пользователя (/block @username)"),
                                        new BotCommand("unblock", "Разблокировать пользователя (/unblock @username)"),
                                        new BotCommand("message", "Изменить сообщение авторассылки"),
                                        new BotCommand("interval", "Изменить интервал авторассылки"),
                                        new BotCommand("stop", "Остановить авторассылку"),
                                        new BotCommand("broadcast", "Разослать оповещение(/broadcast 'сообщение')")
                                );

                            } else {
                                message.setText(String.format(USER_REGISTRATION_TEXT, tgBotUser.getFirstName()));
                                commands = Arrays.asList(
                                        new BotCommand("info", "Информация"),
                                        new BotCommand("sub", "Возобновить уведомления"),
                                        new BotCommand("unsub", "Прекратить уведомления")
                                );
                            }
                            execute(new SetMyCommands(commands, botCommandScopeChat, null));

                        } else {
                            message.setText(USER_ALREADY_EXISTS);
                        }

                        execute(message);
                        break;
                    }
                    case "/unsub": {
                        if (tgBotUserOptional.isPresent()) {
                            message.setChatId(chatId.toString());
                            message.setText("Выберите, на какой срок вы хотите остановить оповещения:");

                            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                            String[] buttonNames = {"1 минута", "3 минуты", "5 минут", "Отключить оповещения"};
                            String[] callbackData = inlineKeyBoardButtonOffsets.keySet().stream().sorted().toArray(String[]::new);

                            for (int i = 0; i < inlineKeyBoardButtonOffsets.size(); i++) {
                                List<InlineKeyboardButton> row = new ArrayList<>();
                                InlineKeyboardButton button = new InlineKeyboardButton();
                                button.setText(buttonNames[i]);
                                button.setCallbackData(callbackData[i]);
                                row.add(button);
                                rows.add(row);
                            }

                            markup.setKeyboard(rows);
                            message.setReplyMarkup(markup);
                        } else {
                            message.setText("Для начала работы бота воспользуйтесь командой /start");
                        }
                        execute(message);
                        break;
                    }
                    case "/sub": {
                        if (tgBotUserOptional.isPresent()) {
                            TgBotUser tgBotUser = tgBotUserOptional.get();
                            tgBotUser.setResumptionNotificationTime(OffsetDateTime.now());
                            userService.update(tgBotUser);
                            message.setText("Поздравляем! Вы возобновили подписку.");
                        } else {
                            message.setText("Для начала работы бота воспользуйтесь командой /start");
                        }
                        execute(message);
                        break;
                    }
                    case "/info": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser -> {
                                    if (TgBotUser.Role.ADMIN.equals(tgBotUser.getRole())) {
                                        Optional<String> userName = Utils.extractUsername(update.getMessage().getText());
                                        if (userName.isPresent()) {
                                            userService.findByUsername(userName.get()).ifPresentOrElse(botUser ->
                                                            message.setText(botUser.toString()),
                                                    () -> message.setText(String.format(USER_NOT_FOUND, userName.get())));
                                        } else {
                                            message.setText(String.format(ADMIN_FORGET_TYPE_USERNAME, command));
                                        }
                                    } else {
                                        message.setText(tgBotUser.toString());
                                    }
                                },
                                () -> message.setText(START_COMMAND_MISSING));

                        execute(message);
                        break;
                    }
                    case "/broadcast": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser1 -> {
                            if(TgBotUser.Role.ADMIN.equals(tgBotUser1.getRole())) {
                                String broadcastMessage = Utils.getBroadcastMessage(command, update.getMessage().getText());
                                if (broadcastMessage.isEmpty()) {
                                    message.setText(String.format(ADMIN_FORGET_TYPE_BROADCAST_TEXT, command));
                                } else {
                                    Stream<Long> subscriberChatIds = userService.findAll()
                                            .filter(tgBotUser -> TgBotUser.Role.SUBSCRIBER.equals(tgBotUser.getRole())
                                                    && OffsetDateTime.now().isAfter(tgBotUser.getResumptionNotificationTime())
                                                    && !TgBotUser.Status.BLOCKED.equals(tgBotUser.getStatus()))
                                            .map(tgBotUser -> {
                                                try {
                                                    sendMessage(tgBotUser.getChatId(), broadcastMessage);
                                                } catch (TelegramApiException e) {}
                                                return tgBotUser.getChatId();
                                            });
                                    message.setText(String.format(BROADCAST_SUCCESSFUL, subscriberChatIds.count()));
                                }
                            } else {
                                message.setText(COMMAND_NOT_ALLOWED);
                            }
                                },
                                () -> message.setText(START_COMMAND_MISSING));

                        execute(message);
                        break;
                    }
                    case "/message": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser1 -> {
                                    if(TgBotUser.Role.ADMIN.equals(tgBotUser1.getRole())) {
                                        String newAutoBroadcastMessage = Utils.getBroadcastMessage(command, update.getMessage().getText());
                                        if (newAutoBroadcastMessage.isEmpty()) {
                                            message.setText(String.format(ADMIN_FORGET_TYPE_BROADCAST_TEXT, command));
                                        } else {
                                            bms.updateMessage(newAutoBroadcastMessage);
                                            message.setText(AUTOBROADCAST_MESSAGE_CHANGED);
                                        }
                                    } else {
                                        message.setText(COMMAND_NOT_ALLOWED);
                                    }
                                },
                                () -> message.setText(START_COMMAND_MISSING));
                        execute(message);
                        break;
                    }
                    case "/time": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser1 -> {
                                    if(TgBotUser.Role.ADMIN.equals(tgBotUser1.getRole())) {
                                        try {
                                            Long newIntervalInMillis = Utils.getMillisFromUserMessage(command, update.getMessage().getText());
                                            bms.updateTime(newIntervalInMillis);
                                            message.setText(AUTOBROADCAST_INTERVAL_CHANGED);
                                        } catch (NumberFormatException e) {
                                            message.setText(ADMIN_TYPE_WRONG_INTERVAL_NUMBER);
                                        }
                                    } else {
                                        message.setText(COMMAND_NOT_ALLOWED);
                                    }
                                },
                                () -> message.setText(START_COMMAND_MISSING));
                        execute(message);
                        break;
                    }
                    case "/stop": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser1 -> {
                                    if(TgBotUser.Role.ADMIN.equals(tgBotUser1.getRole())) {
                                        bms.deactivateAutoBroadcast();
                                        message.setText(AUTOBROADCAST_IS_STOPPED);
                                    } else {
                                        message.setText(COMMAND_NOT_ALLOWED);
                                    }
                                },
                                () -> message.setText(START_COMMAND_MISSING));
                        execute(message);
                        break;
                    }
                    case "/block": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser1 -> {
                                    if(TgBotUser.Role.ADMIN.equals(tgBotUser1.getRole())) {
                                        tgBotUserOptional.ifPresentOrElse(tgBotUser -> {
                                                    if (TgBotUser.Role.ADMIN.equals(tgBotUser.getRole())) {
                                                        Optional<String> userName = Utils.extractUsername(update.getMessage().getText());
                                                        if (userName.isPresent()) {
                                                            String responseMessage = userService.blockUser(userName.get());
                                                            message.setText(String.format(responseMessage, userName.get()));
                                                        } else {
                                                            message.setText(String.format(ADMIN_FORGET_TYPE_USERNAME, command));
                                                        }
                                                    } else {
                                                        message.setText(COMMAND_NOT_ALLOWED);
                                                    }
                                                },
                                                () -> message.setText(START_COMMAND_MISSING));
                                    } else {
                                        message.setText(COMMAND_NOT_ALLOWED);
                                    }
                                },
                                () -> message.setText(START_COMMAND_MISSING));

                        execute(message);
                        break;
                    }
                    case "/unblock": {
                        tgBotUserOptional.ifPresentOrElse(tgBotUser1 -> {
                                    if(TgBotUser.Role.ADMIN.equals(tgBotUser1.getRole())) {
                                        tgBotUserOptional.ifPresentOrElse(tgBotUser -> {
                                                    if (TgBotUser.Role.ADMIN.equals(tgBotUser.getRole())) {
                                                        Optional<String> userName = Utils.extractUsername(update.getMessage().getText());
                                                        if (userName.isPresent()) {
                                                            String responseMessage = userService.unblockUser(userName.get());
                                                            message.setText(String.format(responseMessage, userName.get()));
                                                        } else {
                                                            message.setText(String.format(ADMIN_FORGET_TYPE_USERNAME, command));
                                                        }
                                                    } else {
                                                        message.setText(tgBotUser.toString());
                                                    }
                                                },
                                                () -> message.setText(START_COMMAND_MISSING));
                                    } else {
                                        message.setText(COMMAND_NOT_ALLOWED);
                                    }
                                },
                                () -> message.setText(START_COMMAND_MISSING));
                        execute(message);
                        break;
                    }
                    default: {
                        message.setText(String.format(UNKNOWN_COMMAND, command));
                        execute(message);
                    }
                }
            }
            //проверка на наличик данных от телеграм сервера необходимых для работы бота (кнопки после /unsub)
            if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
                message.setChatId(chatId);
                String queryCallbackData = update.getCallbackQuery().getData();
                Optional<TgBotUser> tgBotUserOpt = userService.findById(chatId);
                Map<Long, String> inlineKeyBoardButtonInfo = inlineKeyBoardButtonOffsets.get(queryCallbackData);

                if (tgBotUserOpt.isPresent() && !inlineKeyBoardButtonInfo.isEmpty()) {
                    TgBotUser tgBotUser = tgBotUserOpt.get();
                    inlineKeyBoardButtonInfo.forEach((resumptionOffset, messageText) -> {
                        tgBotUser.setResumptionNotificationTime(OffsetDateTime.now().plusMinutes(resumptionOffset));
                        userService.update(tgBotUser);
                        message.setText(String.format(UNSUBSCRIBE_MESSAGE, messageText));
                    });
                }

                execute(message);
            }

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(long chatId, String textToSend) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        execute(message);
    }

    @Override
    public String getBotUsername() {
        return botConfig.name();
    }

    @Override
    public String getBotToken() {
        return botConfig.token();
    }
}
