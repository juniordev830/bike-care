# Desenvolvimento e validação — Bike Care

## Estado implementado

Núcleo Kotlin de histórico de pedais e manutenção de componentes, com serialização TSV local.

## Estrutura relevante

`BikeCare.kt`, `BikeCareTests.kt`, `Main.kt` e `ARQUITETURA.md`.

## Como reproduzir verificações

`kotlinc Main.kt -include-runtime -d app.jar` e `java -jar app.jar --self-test`; compilar e executar também `BikeCareTests.kt` conforme README.

Os exemplos devem usar entradas fictícias. Registrar explicitamente o que foi de fato executado no ambiente e comparar com o CI.

## Regressão prioritária

Testar intervalos por quilometragem, manutenção retroativa, prévia de pedal sem mutação, importação TSV adulterada e totais por mês.

## Próximos limites a resolver

TSV não é armazenamento seguro nem sincronizado; próxima etapa: camada de persistência com migração e backup antes de criar app móvel.

## Revisão de contribuição

- [ ] Mudança melhora uma regra, teste ou integração real.
- [ ] Cenários de entrada, saída e erro estão documentados.
- [ ] Nada de dados pessoais, credenciais ou arquivos gerados versionados.
- [ ] PR separado antes de alterar a principal.
