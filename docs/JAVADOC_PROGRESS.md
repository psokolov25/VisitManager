Ведение прогресса по строгому Javadoc (doclint=all, failOnWarnings=true)

Дата/время начала: зафиксировано скриптами и логами в корне (javadoc_now.log).

Сделано:

- Application.java — добавлен Javadoc: класс, main, Configurer и метод configure.
- BasedService.java — добавлены описания полей и конструкторов, Javadoc для clone().
- Event.java — заменён блок /* */ на корректный Javadoc-класс.
- EventHandler.java — Javadoc интерфейса и метода Handle(...).
- DelayedEvents.java — Javadoc класса, конструктора и обоих перегрузок delayedEventService(...).
- Entity.java — Javadoc класса и полей id/name.
- BranchEntity.java — Javadoc полей id/name/branchId.

Следующие крупные очаги предупреждений (по последнему прогону):

- model/Branch.java — добавить @throws/@param и краткие описания для публичных методов (см. строки около 207, 283 и далее).
- service/BranchService.java — много публичных методов без Javadoc: краткие описания + @param/@return.
- model/* (BranchEntityWithVisits, BasedService — проверка остаточных предупреждений после правок).
- service/rules/* — проверка оставшихся интерфейсов/классов.

Как продолжить:

1) Запустить строгую проверку: ./scripts/javadoc-strict.ps1 (или ./scripts/javadoc-strict.sh)
2) Открыть javadoc_strict.log и пройтись по ТОП-файлам (есть краткая сводка в консоли и в scripts/javadoc-summary.*)
3) Пополнять этот файл (docs/JAVADOC_PROGRESS.md) списком «Сделано/Осталось», чтобы не потерять контекст.

