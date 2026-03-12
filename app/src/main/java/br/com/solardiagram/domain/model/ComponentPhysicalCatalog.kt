package br.com.solardiagram.domain.model

data class ComponentPhysicalBehavior(
    val componentType: ComponentType,
    val title: String,
    val realWorldDescription: String,
    val terminalSummary: String,
    val keyBehaviors: List<String>,
    val recommendedModelingNotes: List<String>
)

object ComponentPhysicalCatalog {
    val PV_MODULE = ComponentPhysicalBehavior(
        componentType = ComponentType.PV_MODULE,
        title = "Módulo fotovoltaico",
        realWorldDescription = "Gerador DC com dois terminais físicos de fábrica, normalmente MC4 positivo e negativo.",
        terminalSummary = "DC+ e DC-; não possui DC IN/DC OUT.",
        keyBehaviors = listOf(
            "permite ligação em série entre módulos pela conexão do terminal positivo de um módulo ao negativo do módulo seguinte",
            "os extremos livres do string permanecem como positivo e negativo do arranjo",
            "cada terminal deve receber no máximo um cabo no modelo básico"
        ),
        recommendedModelingNotes = listOf(
            "modelar apenas dois terminais DC reais",
            "não representar o módulo como elemento pass-through com entrada e saída",
            "permitir validações futuras de string e polaridade"
        )
    )

    val MICROINVERTER = ComponentPhysicalBehavior(
        componentType = ComponentType.MICROINVERTER,
        title = "Microinversor",
        realWorldDescription = "Conversor CC/CA com entradas DC independentes por canal e saída AC única para acoplamento em rede.",
        terminalSummary = "pares DC por canal, terminais AC L/N/L2 conforme topologia e PE.",
        keyBehaviors = listOf(
            "cada entrada DC recebe um módulo ou um pequeno string conforme o fabricante",
            "lado AC é de saída para acoplamento em barramento ou circuito derivado",
            "PE deve existir para aterramento do equipamento"
        ),
        recommendedModelingNotes = listOf(
            "manter canais DC separados por par positivo/negativo",
            "não mesclar entradas DC em uma única porta genérica",
            "explicitar fase do lado AC e presença de neutro"
        )
    )

    val STRING_INVERTER = ComponentPhysicalBehavior(
        componentType = ComponentType.STRING_INVERTER,
        title = "Inversor string",
        realWorldDescription = "Conversor CC/CA com múltiplos MPPTs de entrada e terminais AC de saída para o quadro ou barramento.",
        terminalSummary = "pares DC por MPPT, terminais AC conforme número de fases e PE.",
        keyBehaviors = listOf(
            "cada MPPT idealmente possui um par DC+ e DC- dedicado",
            "saída AC alimenta o circuito de geração",
            "PE é obrigatório para aterramento funcional e de proteção"
        ),
        recommendedModelingNotes = listOf(
            "modelar pares DC por MPPT em vez de uma entrada DC única",
            "manter saída AC coerente com a fase configurada",
            "preparar o componente para futura validação de rastreadores MPPT"
        )
    )

    val AC_BUS = ComponentPhysicalBehavior(
        componentType = ComponentType.AC_BUS,
        title = "Barramento AC",
        realWorldDescription = "Conjunto condutivo comum que recebe múltiplas derivações sobre as mesmas barras elétricas.",
        terminalSummary = "terminais por barra L1/L2/L3/N/PE; não possui conceito rígido de entrada e saída.",
        keyBehaviors = listOf(
            "funciona como ponto comum de interligação",
            "cada barra pode receber múltiplos conectores no modelo lógico",
            "fase e neutro dependem da configuração mono/bi/tri"
        ),
        recommendedModelingNotes = listOf(
            "usar portas bidirecionais do tipo barramento",
            "permitir múltiplas conexões por barra",
            "evitar nomenclatura AC IN/AC OUT no próprio barramento"
        )
    )

    val BARL = ComponentPhysicalBehavior(
        componentType = ComponentType.BARL,
        title = "Junção de linha",
        realWorldDescription = "Bloco de junção/equipotencialização para um único condutor de fase.",
        terminalSummary = "IN e OUT para uma fase única; internamente representa uma mesma barra condutiva.",
        keyBehaviors = listOf(
            "permite reunir correntes de ramais da mesma fase",
            "o trecho de saída deve ser dimensionado para a corrente somada",
            "a fase pode ser L1, L2 ou L3 conforme configuração"
        ),
        recommendedModelingNotes = listOf(
            "usar um componente por condutor",
            "não misturar fases diferentes no mesmo BARL"
        )
    )

    val BARN = ComponentPhysicalBehavior(
        componentType = ComponentType.BARN,
        title = "Junção de neutro",
        realWorldDescription = "Bloco de junção/equipotencialização para o neutro.",
        terminalSummary = "IN e OUT para o condutor N.",
        keyBehaviors = listOf(
            "reúne neutros do mesmo circuito",
            "o condutor de saída deve suportar a corrente consolidada"
        ),
        recommendedModelingNotes = listOf(
            "não misturar fase ou PE neste componente"
        )
    )

    val BARPE = ComponentPhysicalBehavior(
        componentType = ComponentType.BARPE,
        title = "Junção de proteção PE",
        realWorldDescription = "Bloco de junção/equipotencialização para condutores de proteção.",
        terminalSummary = "IN e OUT para PE.",
        keyBehaviors = listOf(
            "reúne condutores de proteção",
            "não deve misturar condutores ativos"
        ),
        recommendedModelingNotes = listOf(
            "usar para continuidade de aterramento entre equipamentos"
        )
    )

    val BREAKER = ComponentPhysicalBehavior(
        componentType = ComponentType.BREAKER,
        title = "Disjuntor",
        realWorldDescription = "Dispositivo de proteção em série com lado de linha e lado de carga.",
        terminalSummary = "terminais de linha e de carga por polo; pode ou não chavear neutro conforme o polo/modelo.",
        keyBehaviors = listOf(
            "interrompe o circuito em sobrecorrente",
            "é um elemento em série, não um barramento",
            "quantidade de polos deve refletir o esquema elétrico"
        ),
        recommendedModelingNotes = listOf(
            "representar terminais line/load por polo",
            "validar coerência entre polos e quantidade de terminais",
            "não usar mais de uma conexão por terminal"
        )
    )


    val GRID_SOURCE = ComponentPhysicalBehavior(
        componentType = ComponentType.GRID_SOURCE,
        title = "Alimentação da rede",
        realWorldDescription = "Ponto de entrada da concessionária ou alimentador principal a montante do quadro de distribuição.",
        terminalSummary = "terminais AC de saída por fase/neutro e PE de referência/proteção.",
        keyBehaviors = listOf(
            "atua como origem externa de energia no diagrama",
            "não representa distribuição interna do quadro",
            "deve alimentar QDG, disjuntores ou ponto equivalente de entrada"
        ),
        recommendedModelingNotes = listOf(
            "usar como fonte semântica primária da rede",
            "evitar usar QDG como origem quando GRID_SOURCE estiver presente"
        )
    )

    val QDG = ComponentPhysicalBehavior(
        componentType = ComponentType.QDG,
        title = "Quadro/QDG",
        realWorldDescription = "Ponto de distribuição com alimentação principal, barramentos internos e circuitos derivados.",
        terminalSummary = "modelo simplificado com alimentação e derivação AC, além de PE.",
        keyBehaviors = listOf(
            "atua como concentrador de distribuição",
            "internamente possui barramentos, mas no editor é representado de forma resumida",
            "deve manter coerência com a fase configurada"
        ),
        recommendedModelingNotes = listOf(
            "tratar como componente composto simplificado",
            "usar terminais de alimentação e de derivação até futura modelagem interna detalhada"
        )
    )

    val DPS = ComponentPhysicalBehavior(
        componentType = ComponentType.DPS,
        title = "DPS",
        realWorldDescription = "Dispositivo de proteção contra surtos ligado entre condutores ativos e PE.",
        terminalSummary = "L/N/PE ou L1/L2/L3/N/PE conforme aplicação.",
        keyBehaviors = listOf(
            "não conduz potência principal em série como um disjuntor",
            "atua em derivação para escoamento de surtos",
            "PE é obrigatório"
        ),
        recommendedModelingNotes = listOf(
            "representar terminais em derivação",
            "não exigir sentido de fluxo"
        )
    )

    val GROUND_BAR = ComponentPhysicalBehavior(
        componentType = ComponentType.GROUND_BAR,
        title = "Barramento de terra",
        realWorldDescription = "Barra equipotencial que concentra condutores de proteção e aterramento.",
        terminalSummary = "porta lógica PE com múltiplas conexões, ou múltiplos terminais PE equivalentes.",
        keyBehaviors = listOf(
            "recebe várias conexões de proteção",
            "não mistura condutores ativos",
            "pode ser principal ou secundário"
        ),
        recommendedModelingNotes = listOf(
            "permitir múltiplas conexões PE",
            "não modelar condutores ativos neste componente"
        )
    )

    val LOAD = ComponentPhysicalBehavior(
        componentType = ComponentType.LOAD,
        title = "Carga",
        realWorldDescription = "Consumidor final alimentado por circuito AC.",
        terminalSummary = "terminais de alimentação AC e PE quando aplicável.",
        keyBehaviors = listOf(
            "é ponto terminal do circuito",
            "normalmente recebe linha, neutro e eventualmente PE",
            "não redistribui potência para outro equipamento no modelo simples"
        ),
        recommendedModelingNotes = listOf(
            "manter terminais de alimentação como entrada",
            "não usar saída AC na modelagem simples"
        )
    )
}
