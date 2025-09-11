/**
 * Базовый пакет сервиса VisitManager.
 * Содержит точки входа приложения, REST-контроллеры, бизнес-логику и модели
 * домена (визиты, очереди, точки обслуживания и конфигурацию отделений).
 *
 * <p>Архитектурная схема (упрощённо):
 * <pre>
 *  [API] Controllers ──► Services ──► Events ──► Kafka/DataBus
 *            │              │             \
 *            │              │              └─► External consumers
 *            │              └─► Clients (Keycloak, Printers)
 *            └─► Models (Visits, Queues, Branch config)
 * </pre>
 *
 * <p>Идеи для примеров кода:
 * <ul>
 *   <li>Пример вызова сервиса создания визита</li>
 *   <li>Пример обработки события и обновления состояния визита</li>
 * </ul>
 */
package ru.aritmos;
