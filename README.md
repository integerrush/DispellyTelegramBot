# DispellyBot

  Данный telegram бот вычитывает новые сообщения в подключенной по IMAP, SSL почте, парсит сообщения по указанному в конфигурационном файле полю письма и отправляет их в одноименный telegram чат (в выбранном ручном или автоматическом режиме). 

  Чтобы воспользоваться данным ботом, добавьте свой telegram бот, подключенный к данному Java приложению, в качестве участника группы и выдайте необходимые разрешения.

## Technology Stack

Java 11, Maven, Spring 2, H2, Javax mail, Telegram Bots

## Инструкция по заполнению конфигурационного файла

  1. Зарегистрируйте telegram бот со своего аккаунта с помощью BotFather. Сохраните выданный токен в конфиденциальном месте. Заполните поля секции "bot".
  2. Заполните поля секции "mail", указав логин и пароль почты, из которой планируется чтение сообщений, оставив указание протокола по умолчанию.
  3. В поле bot.field укажите название поля, по которому будет парситься сообщение. Например, если в письме содержится строка "Group: Рейнджеры", чтобы отправить в чат "Рейнджеры", заполните поле так: bot.field=Group. Внимание! Кириллические символы должны быть записаны в формате Юникод. Например, для кириллических "АС", bot.field=\u0410\u0421
  4. Заполните секцию "timer value in ms", указав в миллисекундах частоту проверки  новых сообщений и отправки их в чат (bot.timer) и оставив дефолтное значение поля bot.delay, если задержка после запуска таймера не понадобится.
  5. Если вы хотите использовать фуннкцию запланированных действий, заполните секцию "scheduling", изменив enable.scheduling на значение true, указав крон выражения для первой автоматической вычитки сообщений (например, startcron.expression=0 30 8 * * *) для вычитки писем каждый день в 8.30) и аналогично для остановки работы таймера, в случае если он не будет остановлен вручную.
  6. Заполните секцию "db", указав данные для подключения к базе данных, работающей с сервером, на котором будет запущено данное Java приложение.

### Бот готов к запуску! 