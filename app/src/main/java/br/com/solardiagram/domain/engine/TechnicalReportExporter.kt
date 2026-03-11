package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TechnicalReportExporter(
    private val insightEngine: ProjectInsightEngine = ProjectInsightEngine()
) {
    fun export(project: DiagramProject, validation: ProjectValidationOutput?, targetDir: File): File {
        targetDir.mkdirs()
        val file = File(targetDir, "relatorio_${project.id}.md")
        val insights = insightEngine.analyze(project)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val componentsByType = project.components.groupingBy { it.type }.eachCount().toSortedMap(compareBy { it.name })
        val validationIssues = validation?.report?.issues.orEmpty()
        val content = buildString {
            appendLine("# Relatório Técnico do Projeto")
            appendLine()
            appendLine("- Projeto: ${project.name}")
            appendLine("- Local: ${project.location ?: "-"}")
            appendLine("- Gerado em: $now")
            appendLine()
            appendLine("## Resumo Técnico")
            insights.toSummaryPairs().forEach { (title, value) -> appendLine("- **$title**: $value") }
            appendLine()
            appendLine("## Inventário de Componentes")
            ComponentType.entries.forEach { type ->
                val count = componentsByType[type] ?: return@forEach
                appendLine("- ${type.name}: $count")
            }
            appendLine()
            appendLine("## Conexões")
            appendLine("- Total de componentes: ${project.components.size}")
            appendLine("- Total de conexões: ${project.connections.size}")
            appendLine()
            appendLine("## Validação")
            if (validationIssues.isEmpty()) {
                appendLine("- Nenhuma inconsistência registrada na última validação.")
            } else {
                validationIssues.take(20).forEach { issue ->
                    appendLine("- [${issue.severity}] ${issue.code}: ${issue.message}")
                }
                if (validationIssues.size > 20) appendLine("- ... ${validationIssues.size - 20} ocorrência(s) adicionais")
            }
        }
        file.writeText(content)
        return file
    }
}
