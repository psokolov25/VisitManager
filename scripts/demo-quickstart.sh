#!/usr/bin/env bash
set -euo pipefail

# Quickstart сценарий: открытие СП → создание визита → вызов (confirm) → итог → завершение → закрытие СП
# Требования: запущен VisitManager на http://localhost:8080 с профилем local-no-docker, установлен jq

BASE=${BASE:-http://localhost:8080}
BR=${BR:-37493d1c-8282-4417-a729-dceac1f3e2b4}
SP=${SP:-a66ff6f4-4f4a-4009-8602-0dc278024cf2}
WP=${WP:-d5a84e60-e605-4527-b065-f4bd7a385790}
ENTRY=${ENTRY:-2}
USER=${USER:-psokolov}

# Демо: услуги/итоги
SVC=${SVC:-c3916e7f-7bea-4490-b9d1-0d4064adbe8b}  # Хирург
OUTCOME=${OUTCOME:-462bac1a-568a-4f1f-9548-1c7b61792b4b}  # Запись на повторный приём

echo "[1/6] Открытие точки обслуживания для $USER"
curl -sf -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/workProfiles/$WP/users/$USER/open" >/dev/null

echo "[2/6] Создание визита через точку входа (entryPoint=$ENTRY)"
curl -sf -X POST \
  "$BASE/entrypoint/branches/$BR/entryPoints/$ENTRY/visit" \
  -H 'Content-Type: application/json' \
  -d "[\"$SVC\"]" >/dev/null

echo "[3/6] Вызов визита с подтверждением"
VISIT_ID=$(curl -sf -X POST \
  "$BASE/servicepoint/branches/$BR/servicePoints/$SP/confirmed/visits/call" | jq -r '.id')
if [[ -z "$VISIT_ID" || "$VISIT_ID" == "null" ]]; then
  echo "Не удалось вызвать визит — возможно очередь пуста" >&2
  exit 1
fi
echo "  VISIT_ID=$VISIT_ID"

echo "[4/6] Подтверждение прихода"
curl -sf -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/confirmed/confirm/$VISIT_ID" >/dev/null

echo "[5/6] Добавление итога услуги ($OUTCOME)"
curl -sf -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/outcome/$OUTCOME" >/dev/null

echo "[6/6] Завершение обслуживания"
curl -sf -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/visit/end" >/dev/null

echo "Готово. Сценарий успешно выполнен."

