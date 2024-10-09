import org.slf4j.LoggerFactory
import ru.aritmos.model.visit.Visit


//Инициализация логирования
def log = LoggerFactory.getLogger(this.getClass())
ArrayList<Visit> resultVisit = new ArrayList<>()


log.info "{}", visits

//Проверка, если передан параметр идентификатора очереди - осуществлять фильтрацию визитов по его значению
params.entrySet().forEach(fe -> {
    if (fe.key.equals("queueId")) {
        resultVisit.addAll(visits.stream().filter(f -> f.queueId.equals(params.get("queueId"))).toList())
    }
//Проверка, если передан параметр идентификатора юзерпулов - осуществлять фильтрацию визитов по его значению
    if (fe.key.equals("userPoolId")) {
        resultVisit.addAll(visits.stream().filter(f -> f.poolUserId.equals(params.get("userPoolId"))).toList())
    }
//Проверка, если передан параметр идентификатора пула точек обслуживания - осуществлять фильтрацию визитов по его значению
    if (fe.key.equals("servicePointId")) {
        resultVisit.addAll(visits.stream().filter(f -> f.servicePointId.equals(params.get("servicePointId"))).toList())
    }
})
//Получение оптимального визита с помощью stream.sorted, в данный момент сортировка по времени ожидания
//от большего к меньшему
//после чего возвращается первый визит
result = resultVisit.stream().sorted(Comparator.comparingLong(Visit::getWaitingTime).reversed()).findFirst()
result
