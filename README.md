# Лабораторная работа №6. Мониторинг доставки еды

Вариант 5. Проект продолжает ЛР №5 и использует тот же сервис оформления заказов, Maven, JUnit, Checkstyle и SLF4J/Logback.

## Запуск

Из папки `lab6` в PowerShell с JDK 17, Maven и запущенным Docker Desktop:

```powershell
mvn -B clean compile checkstyle:check test package
docker compose up -d
```

- [Метрики приложения](http://localhost:8081/metrics)
- [Prometheus Targets](http://localhost:9090/targets) — дождитесь статуса **UP** у задания `food-delivery`
- [Prometheus Query](http://localhost:9090/query) — выполните запросы из таблицы ниже
- [Grafana Food Delivery Monitoring](http://localhost:3001/d/food-delivery-lab6/food-delivery-monitoring) — открывается локально в режиме Viewer. Источник Prometheus и дашборд загружаются из `monitoring/grafana` при запуске.

Успешные и отклонённые заказы проходят через `OrderService.placeOrder`. Micrometer регистрирует счётчики, а зависимость `micrometer-registry-prometheus` подключает ядро Micrometer и экспортирует счётчики на `GET /metrics` в формате Prometheus. Prometheus опрашивает endpoint каждые 5 секунд, сохраняет значения как временные ряды и отвечает на PromQL-запросы Grafana.

| Запрос PromQL | Что показывает |
| --- | --- |
| `up{job="food-delivery"}` | Результат последнего scrape. `1` означает успешный опрос, `0` — ошибку опроса. |
| `application_operations_total{job="food-delivery"}` | Все завершившиеся попытки оформить заказ. Используется в карточке и на графике. |
| `application_operations_success_total{job="food-delivery"}` | Заказы, сохранённые сервисом. |
| `application_operations_error_total{job="food-delivery"}` | Ошибки оформления, включая сумму ниже минимума и сбой репозитория. |
| `food_delivery_orders_below_minimum_total{job="food-delivery"}` | Индивидуальный счётчик отказов из-за суммы ниже минимальной. |

## Эксперимент для защиты

Дождитесь первого scrape и посмотрите начальные значения в Prometheus и Grafana. С чистого запуска приложения счётчики начинаются с нуля. HTTP-сценарии используют фиксированные корзины и сохраняют заказы в памяти.

```powershell
1..3 | ForEach-Object { curl.exe -sS -X POST -w "HTTP %{http_code}\n" http://localhost:8081/orders }
# Дождитесь обновления графика и счётчиков (обычно 5–10 секунд).
curl.exe -sS -X POST -w "HTTP %{http_code}\n" http://localhost:8081/orders/below-minimum
docker compose logs delivery
```

| Шаг | Всего | Успешные | Ошибки | Ниже минимума |
| --- | ---: | ---: | ---: | ---: |
| До запросов | 0 | 0 | 0 | 0 |
| Три заказа по 1200, HTTP 201 | 3 | 3 | 0 | 0 |
| Заказ на 300 при минимуме 500, HTTP 400 | 4 | 3 | 1 | 1 |

Успешное сохранение увеличивает общий счётчик и счётчик успехов. Последний запрос увеличивает общий счётчик, счётчик ошибок и счётчик отказов по сумме. В журнале Logback есть конкретное событие `WARN` с номером заказа, суммой и порогом. Мониторинг показывает, сколько раз происходили ошибки и когда росло их число. Checkstyle проверяет исходный код во время сборки. JUnit также проверяет сценарий сбоя хранилища.

Для повторения с нуля выполните `docker compose restart delivery` и дождитесь следующего scrape. Счётчики сбросятся, а Prometheus сохранит предыдущие точки на графике. Для остановки стенда используйте `docker compose down`.

## Git и Jenkins

Ветка [`lab6`](https://github.com/grrraaaa/IPPOR/tree/lab6) создана от проекта ЛР №5. Локальная задача [`delivery-lab6`](http://localhost:8080/job/delivery-lab6/) использует Pipeline script from SCM, ветку `*/lab6`, путь `Jenkinsfile` и Poll SCM раз в минуту. Если Jenkins выключен, запустите `C:\tools\start-jenkins.cmd`.

В [сборке №2](http://localhost:8080/job/delivery-lab6/2/console) проверены общие счётчики из коммита `53556dc`. Следующий коммит добавляет индивидуальный счётчик и панель Orders Below Minimum. Результат виден в [последней успешной сборке](http://localhost:8080/job/delivery-lab6/lastSuccessfulBuild/).

После изменения проверьте `git status`, сохраните коммит и выполните `git push`. В Console Output проверьте ревизию, загрузку Jenkinsfile, Checkout → Build → Static Analysis → Test → Package и итог `Finished: SUCCESS`. Из Artifacts можно взять `target/delivery-1.0-SNAPSHOT.jar`. Остановите контейнер `docker compose stop delivery`, поместите этот JAR в локальный `target/`, затем выполните `docker compose up -d --force-recreate delivery` и повторите эксперимент. Так проверяется метрика именно из сборки Jenkins.

Документация — [Micrometer Prometheus Registry](https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html), [настройка Prometheus](https://prometheus.io/docs/prometheus/latest/configuration/configuration/), [provisioning Grafana](https://grafana.com/docs/grafana/latest/administration/provisioning/).
