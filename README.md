# Bike Care

Registro da manutenção de bicicletas por componente e quilometragem.

## Implementado na primeira versão

**Kotlin — núcleo executável de domínio.** Calcula serviços previstos por componente e ordena os vencidos antes dos futuros. Opcionalmente classifica cada serviço como `ATRASADO`, `VENCE_AGORA`, `PROXIMO` ou `EM_DIA`, com limiar configurável. Rejeita quilometragem incoerente e soma com overflow.

## Como executar

Instale JDK 17+ e o compilador Kotlin 2.1.0 (ou compatível). Os comandos abaixo partem da raiz do repositório.

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar 1200 examples/componentes.tsv
java -jar app.jar 1200 examples/componentes.tsv --alerta-km 200
```

A opção `--alerta-km 200` informa quais serviços vencem em até 200 km e imprime um resumo final com a quantidade de componentes atrasados, vencendo agora, próximos e em dia. Sem a opção, o formato de saída original é preservado. O limite deve ser um inteiro não negativo.

## Testes

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar --self-test
```

Os testes cobrem invariantes, entradas inválidas, fronteiras dos limiares e contagem por status. A preparação local não dispõe deste runtime; o workflow em `.github/workflows/ci.yml` executa a validação no GitHub Actions. O resultado de cada execução está na aba Actions, associado ao commit.

## Organização e limites

Código e exemplos: `Main.kt e examples/componentes.tsv`. Os dados de exemplo são fictícios. As funções de domínio são separadas da leitura de argumentos no mesmo arquivo para manter a primeira versão pequena e revisável.

Intervalos são informados pelo usuário; não são recomendações de manutenção. Cadastro persistente, Compose e Room estão planejados.

A proposta completa de evolução está em `ARQUITETURA.md`. As tecnologias propostas nessa arquitetura não devem ser interpretadas como dependências já implementadas.

## Próximas entregas

- [ ] Cadastrar bicicleta e componentes — ampliar o núcleo atual até o fluxo completo.
- [ ] Registrar quilometragem e manutenção — ampliar o núcleo atual até o fluxo completo.
- [x] Listar serviços previstos e resumir seu status por quilometragem na CLI.
- [ ] Registrar intervenções e persistir o histórico de manutenção.

Licença: ainda não definida.
