# Подтверждение работы

> Вставьте сюда РЕАЛЬНЫЙ вывод команд после запуска у себя (или приложите скриншоты в docs/screenshots/).

## 1. docker compose up --build
```
(вывод)
```

## 2. docker compose ps
```
(вывод — должны быть db, rooms-service, users-service, bookings-service)
```

## 3. bash docs/api-examples.sh
```
(вывод — создание комнаты/пользователя, бронь 201, повторная бронь 409, 404 для комнаты 999)
```

## 4. Сохранение данных после перезапуска
```
docker compose down
docker compose up -d
curl localhost:8083/bookings
(вывод — бронь на месте)
```
