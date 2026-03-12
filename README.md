# CopelSolarDiagramApp (Android Studio / Jetpack Compose)

Projeto MVP para editor de diagrama elétrico FV:

- Projetos locais (JSON em filesDir/projects)
- Editor Canvas 2D (pan, snap em grid, componentes, portas)
- Conexão por fios (Bezier) com compatibilidade de portas
- Seleção de fio (hit-test) e painel de propriedades do trecho
- Validação (queda de tensão e ampacidade) gerando issues por conexão e destaque no canvas

## Como abrir
1. Abra a pasta `CopelSolarDiagramApp` no Android Studio
2. Aguarde o Gradle sync
3. Execute em um device/emulador Android 8+ (minSdk 26)

## Onde ficam os arquivos
- `domain/` modelos e engines
- `data/` repo local JSON + DTO + mapper
- `ui/` telas + editor + viewmodels + navgraph

## Observação
Os valores do catálogo de ampacidade são placeholders; substitua por tabelas normativas (NBR 5410 / catálogos do fabricante) conforme sua fonte.
