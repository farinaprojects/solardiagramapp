# Modelo físico dos componentes – SolarDiagramApp

## Objetivo

Registrar o comportamento elétrico real esperado de cada componente, para orientar renderização, validação e evolução do motor de cálculo.

## Diretrizes centrais

- **Módulo FV**: possui apenas dois terminais físicos de fábrica, `DC+` e `DC-`. Não deve ser modelado como `DC IN` e `DC OUT`.
- **String FV**: a ligação em série é formada pela conexão do `DC+` de um módulo com o `DC-` do módulo seguinte. Os extremos livres formam o positivo e o negativo do string.
- **Microinversor**: possui entradas DC por canal e saída AC para acoplamento em rede. O lado AC pode ser monofásico, bifásico ou trifásico, com `PE`.
- **Inversor string**: possui pares DC por MPPT e terminais AC de saída coerentes com a fase configurada.
- **Barramento AC**: representa barras comuns `L1/L2/L3/N/PE`, com múltiplas derivações; não deve usar a semântica de `AC IN`/`AC OUT`.
- **Disjuntor**: possui lado `LINE` e lado `LOAD` por polo.
- **DPS**: atua em derivação entre condutores ativos e `PE`, não em série como um disjuntor.
- **Barramento de terra**: deve aceitar múltiplas conexões `PE`.
- **Carga**: deve ser tratada como terminal do circuito, apenas com terminais de alimentação e, quando necessário, `PE`.

## Impacto no projeto

Essas regras servem como base para:

1. renderização fiel;
2. validação topológica;
3. validação por tipo de componente;
4. cálculo elétrico futuro;
5. exportação de diagramas com semântica correta.
