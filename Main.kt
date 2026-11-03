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
fun main(args: Array<String>) {
    try {
        if (args.contentEquals(arrayOf("--self-test"))) {
            val result = forecast(1200, listOf(Component("Corrente", 0, 1000), Component("Freio", 1100, 500)))
            check(result[0].remainingKm == -200L && result[1].remainingKm == 400L)
            check(runCatching { forecast(1, listOf(Component("x", 2, 1))) }.isFailure)
            check(runCatching { forecast(Long.MAX_VALUE, listOf(Component("x", Long.MAX_VALUE, 1))) }.isFailure)
            println("3 cenários OK"); return
        }
        require(args.size == 2) { "Uso: java -jar app.jar KM_ATUAL componentes.tsv" }
        val lines = File(args[1]).readLines().filter { it.isNotBlank() }
        require(lines.firstOrNull() == "componente\tultima_km\tintervalo_km") { "Cabeçalho TSV inválido" }
        val components = lines.drop(1).map { row ->
            val cells = row.split('\t'); require(cells.size == 3) { "Esperadas 3 colunas" }
            Component(cells[0], cells[1].toLong(), cells[2].toLong())
        }
        for (service in forecast(args[0].toLong(), components)) {
            println("${service.name}\tprevisto=${service.dueKm} km\tsaldo=${service.remainingKm} km")
        }
    } catch (e: Exception) { System.err.println("Erro: ${e.message}"); kotlin.system.exitProcess(2) }
}
