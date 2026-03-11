package br.com.solardiagram.data.local

import android.content.Context
import br.com.solardiagram.data.local.dto.ProjectDtoV1
import br.com.solardiagram.data.local.dto.ProjectMapperV1
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.util.AppResult
import kotlinx.serialization.json.Json
import java.io.File

interface ProjectRepository {
    suspend fun listProjects(): AppResult<List<DiagramProject>>
    suspend fun loadProject(id: String): AppResult<DiagramProject>
    suspend fun saveProject(project: DiagramProject): AppResult<Unit>
    suspend fun createDemoIfEmpty(): AppResult<Unit>
    suspend fun deleteProject(id: String): AppResult<Unit>
    suspend fun createProject(name: String, location: String?): AppResult<DiagramProject>
}

class LocalJsonProjectRepository(
    private val ctx: Context
): ProjectRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dir: File = File(ctx.filesDir, "projects").apply { mkdirs() }

    override suspend fun listProjects(): AppResult<List<DiagramProject>> = runCatching {
        val projects = dir.listFiles { f -> f.extension == "json" }?.mapNotNull { f ->
            val dto = json.decodeFromString(ProjectDtoV1.serializer(), f.readText())
            ProjectMapperV1.toDomain(dto)
        } ?: emptyList()
        AppResult.Ok(projects.sortedByDescending { it.updatedAtEpochMs })
    }.getOrElse { AppResult.Err(it) }

    override suspend fun loadProject(id: String): AppResult<DiagramProject> = runCatching {
        val f = File(dir, "$id.json")
        val dto = json.decodeFromString(ProjectDtoV1.serializer(), f.readText())
        AppResult.Ok(ProjectMapperV1.toDomain(dto))
    }.getOrElse { AppResult.Err(it) }

    override suspend fun saveProject(project: DiagramProject): AppResult<Unit> = runCatching {
        val f = File(dir, "${project.id}.json")
        val dto = ProjectMapperV1.fromDomain(project)
        f.writeText(json.encodeToString(ProjectDtoV1.serializer(), dto))
        AppResult.Ok(Unit)
    }.getOrElse { AppResult.Err(it) }

    override suspend fun createDemoIfEmpty(): AppResult<Unit> = runCatching {
        val existing = dir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        if (existing.isNotEmpty()) return@runCatching AppResult.Ok(Unit)

        val demo = DiagramProject(
            id = br.com.solardiagram.util.Ids.newId(),
            name = "Projeto Demo",
            location = "Foz do Iguaçu/PR",
            components = emptyList(),
            connections = emptyList()
        )
        saveProject(demo)
        AppResult.Ok(Unit)
    }.getOrElse { AppResult.Err(it) }

    override suspend fun createProject(name: String, location: String?): AppResult<DiagramProject> = runCatching {
        val p = DiagramProject(
            id = br.com.solardiagram.util.Ids.newId(),
            name = name,
            location = location,
            components = emptyList(),
            connections = emptyList()
        )
        when (val saved = saveProject(p)) {
            is AppResult.Ok -> AppResult.Ok(p)
            is AppResult.Err -> AppResult.Err(saved.error)
        }
    }.getOrElse { AppResult.Err(it) }

    override suspend fun deleteProject(id: String): AppResult<Unit> = runCatching {
        val f = File(dir, "$id.json")
        if (f.exists()) f.delete()
        AppResult.Ok(Unit)
    }.getOrElse { AppResult.Err(it) }
}
