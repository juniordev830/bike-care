# Bike Care

Registro da manutenção de bicicletas por componente e quilometragem.

## Implementado na primeira versão

**Kotlin — núcleo executável de domínio.** Calcula serviços previstos por componente e ordena os vencidos antes dos futuros. Rejeita quilometragem incoerente e soma com overflow.

## Como executar

Instale JDK 17+ e o compilador Kotlin 2.1.0 (ou compatível). Os comandos abaixo partem da raiz do repositório.

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar 1200 examples/componentes.tsv
```

## Testes

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar --self-test
```

Os testes cobrem invariantes e entradas inválidas. A preparação local não dispõe deste runtime; o workflow em `.github/workflows/ci.yml` executa a validação no GitHub Actions. O resultado de cada execução está na aba Actions, associado ao commit.

## Organização e limites

Código e exemplos: `Main.kt e examples/componentes.tsv`. Os dados de exemplo são fictícios. As funções de domínio são separadas da leitura de argumentos no mesmo arquivo para manter a primeira versão pequena e revisável.

Intervalos são informados pelo usuário; não são recomendações de manutenção. Cadastro persistente, Compose e Room estão planejados.

A proposta completa de evolução está em `ARQUITETURA.md`. As tecnologias propostas nessa arquitetura não devem ser interpretadas como dependências já implementadas.

## Próximas entregas

- [ ] Cadastrar bicicleta e componentes — ampliar o núcleo atual até o fluxo completo.
- [ ] Registrar quilometragem e manutenção — ampliar o núcleo atual até o fluxo completo.
- [ ] Listar serviços previstos — ampliar o núcleo atual até o fluxo completo.

Licença: ainda não definida.
