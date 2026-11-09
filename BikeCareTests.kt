private fun assertEquals(expected: Any?, actual: Any?, label: String = "") {
    check(expected == actual) { "$label expected=$expected actual=$actual" }
}

private fun fails(block: () -> Unit) {
    check(runCatching(block).isFailure) { "Expected failure" }
}

private fun sample(): BikeState =
    BikeCare.addComponent(
        BikeCare.addComponent(
            BikeCare.create("mtb-29", "MTB Aro 29", 1200),
            "Corrente", 1000, 0, 150
        ),
        "Freios", 500, 800, 100
    )

fun main() {
    val tests = linkedMapOf<String, () -> Unit>(
        "create profile" to {
            val bike = BikeCare.create("a", "Bicicleta", 0)
            assertEquals(0L, bike.odometerKm)
            assertEquals(0, bike.components.size)
        },
        "reject negative odometer" to {
            fails { BikeCare.create("a", "Bicicleta", -1) }
        },
        "reject control characters" to {
            fails { BikeCare.create("a\tb", "Bicicleta", 0) }
            fails { BikeCare.create("a", "Bici\nquebrada", 0) }
        },
        "add components" to {
            assertEquals(2, sample().components.size)
            assertEquals("Corrente", sample().components[0].name)
        },
        "reject duplicate components" to {
            fails { BikeCare.addComponent(sample(), "corrente", 200) }
        },
        "reject invalid component intervals" to {
            fails { BikeCare.addComponent(sample(), "Pneu", 0) }
            fails { BikeCare.addComponent(sample(), "Pneu", 100, 1300) }
            fails { BikeCare.addComponent(sample(), "Pneu", 100, 0, -1) }
        },
        "sort overdue before upcoming" to {
            val items = BikeCare.forecast(sample())
            assertEquals("Corrente", items[0].name)
            assertEquals(-200L, items[0].remainingKm)
            assertEquals(DueStatus.OVERDUE, items[0].status)
            assertEquals(DueStatus.DUE_SOON, items[1].status)
        },
        "record ride advances odometer" to {
            val bike = BikeCare.recordRide(sample(), "2026-11-08", 80)
            assertEquals(1280L, bike.odometerKm)
            assertEquals(80L, bike.rides.single().distanceKm)
        },
        "record ride does not mutate old state" to {
            val original = sample()
            val updated = BikeCare.recordRide(original, "2026-11-08", 80)
            assertEquals(1200L, original.odometerKm)
            assertEquals(1280L, updated.odometerKm)
        },
        "reject invalid ride distances" to {
            fails { BikeCare.recordRide(sample(), "2026-11-08", 0) }
            fails { BikeCare.recordRide(sample(), "2026-11-08", 1001) }
        },
        "reject invalid dates" to {
            fails { BikeCare.recordRide(sample(), "2026-02-29", 20) }
            fails { BikeCare.recordRide(sample(), "08/11/2026", 20) }
            BikeCare.recordRide(sample(), "2028-02-29", 20)
        },
        "reject backwards ride dates" to {
            val bike = BikeCare.recordRide(sample(), "2026-11-08", 30)
            fails { BikeCare.recordRide(bike, "2026-11-07", 30) }
        },
        "record service resets forecast" to {
            val bike = BikeCare.recordService(sample(), "2026-11-08", "corrente", "Lubrificada")
            val chain = BikeCare.forecast(bike).first { it.name == "Corrente" }
            assertEquals(1000L, chain.remainingKm)
            assertEquals(DueStatus.OK, chain.status)
            assertEquals("Lubrificada", bike.services.single().note)
        },
        "reject unknown service" to {
            fails { BikeCare.recordService(sample(), "2026-11-08", "Motor") }
        },
        "remove component without history" to {
            val bike = BikeCare.removeComponent(sample(), "freios")
            assertEquals(1, bike.components.size)
        },
        "preserve maintenance history on remove" to {
            val bike = BikeCare.recordService(sample(), "2026-11-08", "Corrente")
            fails { BikeCare.removeComponent(bike, "Corrente") }
        },
        "preview newly overdue and due soon" to {
            val bike = BikeCare.addComponent(
                BikeCare.create("x", "Bike", 100),
                "Pneu", 200, 0, 50
            )
            val preview = BikeCare.previewRide(bike, 120)
            assertEquals(220L, preview.projectedKm)
            assertEquals(listOf("Pneu"), preview.newlyOverdue)
        },
        "preview does not change state" to {
            val bike = sample()
            BikeCare.previewRide(bike, 80)
            assertEquals(1200L, bike.odometerKm)
        },
        "statistics count rides and services" to {
            var bike = BikeCare.recordRide(sample(), "2026-11-08", 50)
            bike = BikeCare.recordRide(bike, "2026-11-09", 100)
            bike = BikeCare.recordService(bike, "2026-11-09", "Corrente")
            val stats = BikeCare.stats(bike)
            assertEquals(2, stats.rideCount)
            assertEquals(150L, stats.riddenKm)
            assertEquals(100L, stats.longestRideKm)
            assertEquals(75.0, stats.averageRideKm)
            assertEquals(1, stats.serviceCount)
        },
        "empty statistics" to {
            val stats = BikeCare.stats(BikeCare.create("x", "Bike", 0))
            assertEquals(0.0, stats.averageRideKm)
            assertEquals(0, stats.rideCount)
        },
        "TSV roundtrip" to {
            var bike = BikeCare.recordRide(sample(), "2026-11-08", 40)
            bike = BikeCare.recordService(bike, "2026-11-08", "Corrente")
            assertEquals(bike, BikeCare.fromTsv(BikeCare.toTsv(bike)))
        },
        "TSV rejects unknown version" to {
            fails { BikeCare.fromTsv("BIKECARE\t99\n") }
        },
        "TSV rejects unknown record" to {
            fails { BikeCare.fromTsv("BIKECARE\t1\nBIKE\tx\tBike\t0\nHACK\t1\n") }
        },
        "TSV rejects invalid numeric fields" to {
            fails { BikeCare.fromTsv("BIKECARE\t1\nBIKE\tx\tBike\tnan\n") }
        },
        "TSV rejects malformed ride history" to {
            fails {
                BikeCare.fromTsv(
                    "BIKECARE\t1\nBIKE\tx\tBike\t10\nRIDE\t2026-11-08\t50\t10\n"
                )
            }
        },
        "monthly mileage groups rides across calendar months" to {
            var bike = BikeCare.create("x", "Bike", 0)
            bike = BikeCare.recordRide(bike, "2026-10-31", 40)
            bike = BikeCare.recordRide(bike, "2026-11-01", 50)
            bike = BikeCare.recordRide(bike, "2026-11-05", 10)
            val monthly = BikeCare.monthlyMileage(bike)
            assertEquals(listOf("2026-10", "2026-11"), monthly.keys.toList())
            assertEquals(40L, monthly["2026-10"])
            assertEquals(60L, monthly["2026-11"])
            check(BikeCare.summary(bike).contains("Pedais em 2026-11: 60 km"))
            assertEquals(100L, bike.odometerKm)
        },
        "imported ride history rejects dates out of order" to {
            var bike = BikeCare.create("x", "Bike", 0)
            bike = BikeCare.recordRide(bike, "2026-10-31", 40)
            bike = BikeCare.recordRide(bike, "2026-11-01", 50)
            val corrupted = BikeCare.toTsv(bike).replace(
                "RIDE\t2026-11-01\t50\t90", "RIDE\t2026-09-01\t50\t90"
            )
            fails { BikeCare.fromTsv(corrupted) }
            assertEquals("2026-11-01", bike.rides.last().date)
        },
        "summary is readable" to {
            val text = BikeCare.summary(sample())
            check(text.contains("MTB Aro 29"))
            check(text.contains("Corrente"))
            check(text.contains("vencidos"))
        }
    )
    var failures = 0
    for ((name, test) in tests) {
        try {
            test()
            println("[OK] $name")
        } catch (error: Throwable) {
            failures++
            System.err.println("[FAIL] $name: ${error.message}")
        }
    }
    println("${tests.size} testes; $failures falhas")
    check(failures == 0) { "$failures testes falharam" }
}
