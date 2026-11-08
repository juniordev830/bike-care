# Bike Care

Ferramenta local em Kotlin para acompanhar quilometragem e manutenção de bicicletas. A nova biblioteca mantém histórico imutável de pedais, componentes, serviços, alertas de revisão e snapshots TSV. **Intervalos são configurados pelo ciclista e não substituem o manual do fabricante.**

## Requisitos

JDK 17+ e compilador Kotlin 2.1.0 ou compatível. Não precisa de banco de dados, rede nem dependências externas.

## Interface original preservada

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar 1200 examples/componentes.tsv
java -jar app.jar --self-test
```

A função `forecast(current, components)` continua funcionando sem alterações.

## Nova biblioteca de domínio

O arquivo `BikeCare.kt` oferece os seguintes tipos: `BikeState`, `BikeComponent`, `RideEntry`, `ServiceEntry`, `DueItem`, `DueStatus`, `RidePreview` e `BikeStats`.

Exemplo de integração em código Kotlin:

```kotlin
var bike = BikeCare.create("mtb-29", "MTB Aro 29", 1200)
bike = BikeCare.addComponent(bike, "Corrente", intervalKm = 1000, lastServiceKm = 300)
bike = BikeCare.addComponent(bike, "Freios", intervalKm = 500, lastServiceKm = 1000)
bike = BikeCare.recordRide(bike, "2026-11-08", 80)

val alertas = BikeCare.forecast(bike)
val simulacao = BikeCare.previewRide(bike, 100)
val resumo = BikeCare.summary(bike)
val snapshot = BikeCare.toTsv(bike)
val recuperado = BikeCare.fromTsv(snapshot)
check(recuperado == bike)
```

As operações retornam um novo `BikeState`, sem modificar o estado anterior. `recordService` atualiza a quilometragem da última manutenção e registra a ocorrência. `removeComponent` impede excluir componentes com histórico de manutenção. `stats` apresenta distância registrada, média por pedal, maior pedal e quantidade de serviços.

Os status de previsão são `OVERDUE` (vencido), `DUE_SOON` (próximo) e `OK`. A simulação de um pedal não modifica o odômetro. Os snapshots TSV são versionados e validados na importação.

## Testes

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar --self-test
kotlinc BikeCare.kt BikeCareTests.kt -include-runtime -d bike-tests.jar
java -jar bike-tests.jar
```

O GitHub Actions compila e executa a interface original e os 26 cenários da biblioteca. Os testes incluem validações, overflow, imutabilidade, histórico, manutenção, simulação e persistência.

## Limitações

A biblioteca ainda não tem interface gráfica, sincronização em nuvem, banco de dados ou recomendações automáticas de peças. As distâncias e intervalos são fornecidos pelo usuário. O TSV é um formato local simples, sem criptografia; não armazene dados confidenciais nele.
