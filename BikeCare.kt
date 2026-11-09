import java.time.LocalDate

/** Offline maintenance model. All commands return a new immutable state. */
data class BikeComponent(
    val name: String,
    val intervalKm: Long,
    val lastServiceKm: Long,
    val warningKm: Long = 100
)

data class RideEntry(val date: String, val distanceKm: Long, val odometerKm: Long)
data class ServiceEntry(val date: String, val component: String, val odometerKm: Long, val note: String)

data class BikeState(
    val id: String,
    val name: String,
    val odometerKm: Long,
    val components: List<BikeComponent> = emptyList(),
    val rides: List<RideEntry> = emptyList(),
    val services: List<ServiceEntry> = emptyList()
)

enum class DueStatus { OVERDUE, DUE_SOON, OK }
data class DueItem(val name: String, val dueKm: Long, val remainingKm: Long, val status: DueStatus)
data class RidePreview(val projectedKm: Long, val newlyOverdue: List<String>, val newlyDueSoon: List<String>)
data class BikeStats(
    val rideCount: Int,
    val riddenKm: Long,
    val longestRideKm: Long,
    val averageRideKm: Double,
    val serviceCount: Int,
    val overdueCount: Int
)

object BikeCare {
    fun create(id: String, name: String, odometerKm: Long): BikeState {
        checkText(id, "ID", 40)
        checkText(name, "Nome", 100)
        require(odometerKm >= 0) { "Odômetro negativo" }
        return BikeState(id.trim(), name.trim(), odometerKm)
    }

    fun validate(state: BikeState): BikeState {
        checkText(state.id, "ID", 40)
        checkText(state.name, "Nome", 100)
        require(state.odometerKm >= 0) { "Odômetro negativo" }
        val names = mutableSetOf<String>()
        state.components.forEach {
            checkText(it.name, "Componente", 100)
            require(names.add(it.name.lowercase())) { "Componente repetido" }
            require(it.intervalKm > 0 && it.warningKm >= 0) { "Intervalo inválido" }
            require(it.lastServiceKm in 0..state.odometerKm) { "Último serviço inválido" }
            Math.addExact(it.lastServiceKm, it.intervalKm)
        }
        var lastOdometer = 0L
        var lastRideDate: String? = null
        var totalRidden = 0L
        state.rides.forEach {
            checkDate(it.date)
            require(lastRideDate?.let { previous -> it.date >= previous } ?: true) {
                "Histórico de pedais com datas fora de ordem"
            }
            lastRideDate = it.date
            require(it.distanceKm in 1..1000) { "Distância inválida" }
            require(it.odometerKm in 0..state.odometerKm && it.odometerKm > lastOdometer) {
                "Histórico de pedais fora de ordem"
            }
            lastOdometer = it.odometerKm
            totalRidden = Math.addExact(totalRidden, it.distanceKm)
        }
        require(totalRidden <= state.odometerKm) { "Pedais excedem odômetro" }
        state.services.forEach {
            checkDate(it.date)
            checkText(it.component, "Componente", 100)
            checkOptionalText(it.note, "Observação", 200)
            require(it.odometerKm in 0..state.odometerKm) { "Serviço com odômetro inválido" }
            require(it.component.lowercase() in names) { "Serviço de componente desconhecido" }
        }
        return state
    }

    fun addComponent(
        state: BikeState,
        name: String,
        intervalKm: Long,
        lastServiceKm: Long = state.odometerKm,
        warningKm: Long = 100
    ): BikeState {
        validate(state)
        checkText(name, "Componente", 100)
        require(state.components.none { it.name.equals(name.trim(), ignoreCase = true) }) {
            "Componente repetido"
        }
        return validate(state.copy(components = state.components +
            BikeComponent(name.trim(), intervalKm, lastServiceKm, warningKm)))
    }

    fun removeComponent(state: BikeState, name: String): BikeState {
        validate(state)
        val item = find(state, name)
        require(state.services.none { it.component.equals(item.name, ignoreCase = true) }) {
            "Não pode remover componente com histórico"
        }
        return validate(state.copy(components = state.components.filterNot { it.name == item.name }))
    }

    fun recordRide(state: BikeState, date: String, distanceKm: Long): BikeState {
        validate(state)
        checkDate(date)
        require(distanceKm in 1..1000) { "Distância inválida" }
        require(state.rides.isEmpty() || date >= state.rides.last().date) {
            "Data de pedal anterior à última"
        }
        val next = Math.addExact(state.odometerKm, distanceKm)
        return validate(state.copy(
            odometerKm = next,
            rides = state.rides + RideEntry(date, distanceKm, next)
        ))
    }

    fun recordService(state: BikeState, date: String, name: String, note: String = ""): BikeState {
        validate(state)
        checkDate(date)
        checkOptionalText(note, "Observação", 200)
        val item = find(state, name)
        return validate(state.copy(
            components = state.components.map {
                if (it.name == item.name) it.copy(lastServiceKm = state.odometerKm) else it
            },
            services = state.services + ServiceEntry(date, item.name, state.odometerKm, note.trim())
        ))
    }

    fun forecast(state: BikeState): List<DueItem> {
        validate(state)
        return state.components.map {
            val due = Math.addExact(it.lastServiceKm, it.intervalKm)
            val remaining = due - state.odometerKm
            val status = when {
                remaining < 0 -> DueStatus.OVERDUE
                remaining <= it.warningKm -> DueStatus.DUE_SOON
                else -> DueStatus.OK
            }
            DueItem(it.name, due, remaining, status)
        }.sortedWith(compareBy<DueItem> { it.remainingKm }.thenBy { it.name })
    }

    fun previewRide(state: BikeState, distanceKm: Long): RidePreview {
        validate(state)
        require(distanceKm in 1..1000) { "Distância de simulação inválida" }
        val projected = Math.addExact(state.odometerKm, distanceKm)
        val before = forecast(state).associateBy { it.name }
        val after = forecast(state.copy(odometerKm = projected))
        return RidePreview(
            projected,
            after.filter {
                it.status == DueStatus.OVERDUE && before.getValue(it.name).status != DueStatus.OVERDUE
            }.map { it.name },
            after.filter {
                it.status == DueStatus.DUE_SOON && before.getValue(it.name).status == DueStatus.OK
            }.map { it.name }
        )
    }

    /** Monthly distance chart data, including only recorded rides (not imported odometer). */
    fun monthlyMileage(state: BikeState): Map<String, Long> {
        validate(state)
        val totals = sortedMapOf<String, Long>()
        for (ride in state.rides) {
            val month = ride.date.substring(0, 7)
            totals[month] = Math.addExact(totals[month] ?: 0L, ride.distanceKm)
        }
        return totals
    }

    fun stats(state: BikeState): BikeStats {
        validate(state)
        val distance = state.rides.fold(0L) { total, ride -> Math.addExact(total, ride.distanceKm) }
        return BikeStats(
            state.rides.size, distance, state.rides.maxOfOrNull { it.distanceKm } ?: 0L,
            if (state.rides.isEmpty()) 0.0 else distance.toDouble() / state.rides.size,
            state.services.size, forecast(state).count { it.status == DueStatus.OVERDUE }
        )
    }

    /** Versioned TSV: tabs, newlines and control characters are rejected in user fields. */
    fun toTsv(state: BikeState): String {
        validate(state)
        val lines = mutableListOf("BIKECARE\t1", "BIKE\t${state.id}\t${state.name}\t${state.odometerKm}")
        state.components.forEach {
            lines += "COMPONENT\t${it.name}\t${it.intervalKm}\t${it.lastServiceKm}\t${it.warningKm}"
        }
        state.rides.forEach {
            lines += "RIDE\t${it.date}\t${it.distanceKm}\t${it.odometerKm}"
        }
        state.services.forEach {
            lines += "SERVICE\t${it.date}\t${it.component}\t${it.odometerKm}\t${it.note}"
        }
        return lines.joinToString("\n", postfix = "\n")
    }

    fun fromTsv(contents: String): BikeState {
        val lines = contents.lines().filter { it.isNotEmpty() }
        require(lines.firstOrNull() == "BIKECARE\t1") { "Versão TSV inválida" }
        require(lines.size >= 2) { "Bicicleta ausente" }
        val bike = parseRow(lines[1], "BIKE", 4)
        var state = create(bike[1], bike[2], bike[3].toLong())
        val components = mutableListOf<BikeComponent>()
        val rides = mutableListOf<RideEntry>()
        val services = mutableListOf<ServiceEntry>()
        lines.drop(2).forEach { line ->
            when (line.substringBefore('\t')) {
                "COMPONENT" -> {
                    val row = parseRow(line, "COMPONENT", 5)
                    components += BikeComponent(
                        row[1], row[2].toLong(), row[3].toLong(), row[4].toLong()
                    )
                }
                "RIDE" -> {
                    val row = parseRow(line, "RIDE", 4)
                    rides += RideEntry(row[1], row[2].toLong(), row[3].toLong())
                }
                "SERVICE" -> {
                    val row = parseRow(line, "SERVICE", 5)
                    services += ServiceEntry(row[1], row[2], row[3].toLong(), row[4])
                }
                else -> throw IllegalArgumentException("Tipo de registro desconhecido")
            }
        }
        state = state.copy(components = components, rides = rides, services = services)
        return validate(state)
    }

    fun summary(state: BikeState): String {
        val statistics = stats(state)
        val lines = mutableListOf(
            "Bicicleta: ${state.name} (${state.id})",
            "Odômetro: ${state.odometerKm} km",
            "Pedais: ${statistics.rideCount} (${statistics.riddenKm} km)",
            "Serviços: ${statistics.serviceCount}",
            "Componentes vencidos: ${statistics.overdueCount}"
        )
        forecast(state).forEach {
            lines += "${it.name}: ${it.status} | previsto=${it.dueKm} km | saldo=${it.remainingKm} km"
        }
        monthlyMileage(state).forEach { (month, distance) ->
            lines += "Pedais em $month: $distance km"
        }
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun parseRow(line: String, kind: String, count: Int): List<String> {
        val values = line.split('\t')
        require(values.size == count && values[0] == kind) { "Registro $kind inválido" }
        return values
    }

    private fun find(state: BikeState, name: String): BikeComponent =
        state.components.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?: throw IllegalArgumentException("Componente não encontrado: $name")

    private fun checkDate(date: String) {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) { "Data deve usar AAAA-MM-DD" }
        try {
            LocalDate.parse(date)
        } catch (error: java.time.format.DateTimeParseException) {
            throw IllegalArgumentException("Data inválida: $date")
        }
    }

    private fun checkText(value: String, label: String, max: Int) {
        require(value.isNotBlank() && value.trim().length <= max) { "$label inválido" }
        require(value.none { it.code < 32 }) { "$label contém caracteres de controle" }
    }

    private fun checkOptionalText(value: String, label: String, max: Int) {
        if (value.isNotEmpty()) checkText(value, label, max)
    }
}
