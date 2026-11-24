#!/usr/bin/env bash
# Минимальный сценарий: создать комнату и пользователя, забронировать, проверить конфликт.
set -e
echo "== health =="
curl -s localhost:8081/health; echo
curl -s localhost:8082/health; echo
curl -s localhost:8083/health; echo

echo "== создаём комнату =="
curl -s -X POST localhost:8081/rooms -H "Content-Type: application/json" \
  -d '{"name":"Переговорная Альфа","capacity":8}'; echo

echo "== создаём пользователя =="
curl -s -X POST localhost:8082/users -H "Content-Type: application/json" \
  -d '{"name":"Иван Петров","email":"ivan@example.com"}'; echo

echo "== бронь (bookings-service сам ходит в rooms и users) =="
curl -s -X POST localhost:8083/bookings -H "Content-Type: application/json" \
  -d '{"roomId":1,"userId":1,"startTime":"2026-11-20T10:00:00Z","endTime":"2026-11-20T11:00:00Z"}'; echo

echo "== повторная бронь на то же время -> 409 =="
curl -s -i -X POST localhost:8083/bookings -H "Content-Type: application/json" \
  -d '{"roomId":1,"userId":1,"startTime":"2026-11-20T10:30:00Z","endTime":"2026-11-20T11:30:00Z"}' | head -n 1

echo "== несуществующая комната -> 404 =="
curl -s -X POST localhost:8083/bookings -H "Content-Type: application/json" \
  -d '{"roomId":999,"userId":1,"startTime":"2026-11-21T10:00:00Z","endTime":"2026-11-21T11:00:00Z"}'; echo

echo "== список броней =="
curl -s localhost:8083/bookings; echo
