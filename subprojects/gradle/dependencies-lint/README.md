Lint для поиска излишних зависимостей.

## Как пользоваться?

`./gradlew lintDependencies`

**Результаты работы**

Отчет: `build/reports/dependencies-lint.xml`. 
Формат совместим с android lint xml отчетом, чтобы использовать один универсальный для автоматизации.

## Что умеет находить

### Неиспользуемая зависимость

id = UnusedDependency

Не нашли ни одного использования классов.

### Излишняя зависимость

id = RedundantDependency

Подключили зависимость только ради ее транзитивных зависимостей.
