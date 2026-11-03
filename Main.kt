import java.io.File

data class Component(val name: String, val lastKm: Long, val intervalKm: Long)
data class Service(val name: String, val dueKm: Long, val remainingKm: Long)
fun forecast(current: Long, components: List<Component>): List<Service> {
    require(current >= 0) { "Quilometragem negativa" }
    require(components.map { it.name }.distinct().size == components.size) { "Componente repetido" }
    return components.map {
        require(it.name.isNotBlank() && it.lastKm in 0..current && it.intervalKm > 0) { "Componente ou intervalo inválido" }
        val due = Math.addExact(it.lastKm, it.intervalKm)
        Service(it.name, due, due - current)
    }.sortedWith(compareBy<Service> { it.remainingKm }.thenBy { it.name })
}
enum class MaintenanceStatus { ATRASADO, VENCE_AGORA, PROXIMO, EM_DIA }
data class MaintenanceSummary(val atrasados: Int, val vencemAgora: Int, val proximos: Int, val emDia: Int)

fun status(service: Service, alertKm: Long): MaintenanceStatus {
    require(alertKm >= 0) { "Alerta em km não pode ser negativo" }
    return when {
        service.remainingKm < 0L -> MaintenanceStatus.ATRASADO
        service.remainingKm == 0L -> MaintenanceStatus.VENCE_AGORA
        service.remainingKm <= alertKm -> MaintenanceStatus.PROXIMO
        else -> MaintenanceStatus.EM_DIA
    }
}

fun summarize(services: List<Service>, alertKm: Long): MaintenanceSummary {
    require(alertKm >= 0) { "Alerta em km não pode ser negativo" }
    val counts = services.groupingBy { status(it, alertKm) }.eachCount()
    return MaintenanceSummary(
        counts.getOrDefault(MaintenanceStatus.ATRASADO, 0),
        counts.getOrDefault(MaintenanceStatus.VENCE_AGORA, 0),
        counts.getOrDefault(MaintenanceStatus.PROXIMO, 0),
        counts.getOrDefault(MaintenanceStatus.EM_DIA, 0)
    )
}

fun main(args: Array<String>) {
    try {
        if (args.contentEquals(arrayOf("--self-test"))) {
            val result = forecast(1200, listOf(Component("Corrente", 0, 1000), Component("Freio", 1100, 500)))
            check(result[0].remainingKm == -200L && result[1].remainingKm == 400L)
            check(runCatching { forecast(1, listOf(Component("x", 2, 1))) }.isFailure)
            check(runCatching { forecast(Long.MAX_VALUE, listOf(Component("x", Long.MAX_VALUE, 1))) }.isFailure)
            val services = forecast(1200, listOf(
                Component("atrasado", 0, 1000),
                Component("agora", 200, 1000),
                Component("proximo", 500, 800),
                Component("em dia", 1100, 500)
            ))
            check(summarize(services, 100L) == MaintenanceSummary(1, 1, 1, 1))
            check(status(Service("exato", 1300, 100), 100L) == MaintenanceStatus.PROXIMO)
            check(status(Service("futuro", 1301, 101), 100L) == MaintenanceStatus.EM_DIA)
            check(runCatching { summarize(services, -1) }.isFailure)
            println("7 cenários OK"); return
        }
        require(args.size == 2 || (args.size == 4 && args[2] == "--alerta-km")) {
            "Uso: java -jar app.jar KM_ATUAL componentes.tsv [--alerta-km LIMITE]"
        }
        val alertKm = if (args.size == 4) args[3].toLong() else null
        if (alertKm != null) require(alertKm >= 0) { "Alerta em km não pode ser negativo" }
        val lines = File(args[1]).readLines().filter { it.isNotBlank() }
        require(lines.firstOrNull() == "componente\tultima_km\tintervalo_km") { "Cabeçalho TSV inválido" }
        val components = lines.drop(1).map { row ->
            val cells = row.split('\t'); require(cells.size == 3) { "Esperadas 3 colunas" }
            Component(cells[0], cells[1].toLong(), cells[2].toLong())
        }
        val services = forecast(args[0].toLong(), components)
        for (service in services) {
            val situation = alertKm?.let { "\tstatus=${status(service, it)}" } ?: ""
            println("${service.name}\tprevisto=${service.dueKm} km\tsaldo=${service.remainingKm} km$situation")
        }
        if (alertKm != null) {
            val totals = summarize(services, alertKm)
            println("resumo\tatrasados=${totals.atrasados}\tvencem_agora=${totals.vencemAgora}\tproximos=${totals.proximos}\tem_dia=${totals.emDia}")
        }
    } catch (e: Exception) { System.err.println("Erro: ${e.message}"); kotlin.system.exitProcess(2) }
}
