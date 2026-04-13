# Диаграмма пакетов PCMEF

Архитектурный паттерн **PCMEF** (Presentation-Control-Mediator-Entity-Foundation) адаптирован для клиент-серверной архитектуры мобильного приложения.

- **Presentation** — мобильное клиентское приложение (Android/Kotlin).
- **Control** — REST-контроллеры на сервере.
- **Mediator** — сервисы бизнес-логики.
- **Entity** — JPA-сущности.
- **Foundation** — репозитории доступа к данным.