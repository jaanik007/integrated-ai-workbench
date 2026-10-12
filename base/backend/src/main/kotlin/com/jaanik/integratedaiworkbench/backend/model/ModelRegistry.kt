package com.jaanik.integratedaiworkbench.backend.model

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

interface ModelRegistry {
    fun profiles(): List<ModelProfile>
    fun find(profileId: String): ModelProfile?
    fun models(): List<RegisteredModel> = emptyList()
    fun findModel(modelId: String): RegisteredModel? = null
}

data class RegisteredModel(
    val id: String,
    val displayName: String,
    val canonicalPath: String,
    val sizeBytes: Long,
    val candidateFormat: String,
    val profileIds: Set<String> = emptySet(),
    val registeredAt: Instant = Instant.now(),
)

data class DiscoveredModel(
    val canonicalPath: String,
    val displayName: String,
    val sizeBytes: Long,
    val candidateFormat: String,
)

class ModelRegistryException(message: String) : IllegalArgumentException(message)

class ModelPathValidator(
    private val approvedFiles: Set<Path> = emptySet(),
    private val approvedDirectories: Set<Path> = emptySet(),
    private val supportedExtensions: Set<String> = DEFAULT_MODEL_EXTENSIONS,
) {
    fun validateSelectedFile(path: Path): Path {
        val canonical = canonicalRegularFile(path)
        if (canonical !in approvedFiles && approvedDirectories.none { canonical.startsWith(it) }) {
            throw ModelRegistryException("Model file is outside the approved locations.")
        }
        return canonical
    }

    fun validateDiscoveryDirectory(path: Path): Path {
        val canonical = canonicalDirectory(path)
        if (canonical !in approvedDirectories) {
            throw ModelRegistryException("Model discovery directory is not approved.")
        }
        return canonical
    }

    fun validatePersistedFile(path: Path): Path = validateSelectedFile(path)

    fun isSupported(path: Path): Boolean = path.fileName.toString()
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase() in supportedExtensions

    private fun canonicalRegularFile(path: Path): Path {
        require(path.isAbsolute) { "Model path must be absolute." }
        val canonical = try {
            path.toRealPath()
        } catch (_: Exception) {
            throw ModelRegistryException("Model file is missing or inaccessible.")
        }
        if (!Files.isRegularFile(canonical) || !Files.isReadable(canonical)) {
            throw ModelRegistryException("Model reference must be a readable regular file.")
        }
        if (!isSupported(canonical)) {
            throw ModelRegistryException("Model file type is not supported.")
        }
        return canonical
    }

    private fun canonicalDirectory(path: Path): Path {
        require(path.isAbsolute) { "Model directory path must be absolute." }
        val canonical = try {
            path.toRealPath()
        } catch (_: Exception) {
            throw ModelRegistryException("Model directory is missing or inaccessible.")
        }
        if (!Files.isDirectory(canonical) || !Files.isReadable(canonical)) {
            throw ModelRegistryException("Model discovery reference must be a readable directory.")
        }
        return canonical
    }

    companion object {
        val DEFAULT_MODEL_EXTENSIONS = setOf("gguf", "safetensors", "onnx", "pt", "pth")

        fun fromSelections(selectedFiles: Collection<Path>, selectedDirectories: Collection<Path>): ModelPathValidator =
            ModelPathValidator(
                approvedFiles = selectedFiles.map { it.toRealPath() }.toSet(),
                approvedDirectories = selectedDirectories.map { it.toRealPath() }.toSet(),
            )
    }
}

/** In-memory registry used by runtime services and deterministic tests. */
open class InMemoryModelRegistry(
    private val pathValidator: ModelPathValidator,
) : ModelRegistry {
    private val entries = linkedMapOf<String, RegisteredModel>()
    private val profileEntries = linkedMapOf<String, ModelProfile>()

    override fun profiles(): List<ModelProfile> = profileEntries.values.sortedBy(ModelProfile::displayName)
    override fun find(profileId: String): ModelProfile? = profileEntries[profileId]
    override fun models(): List<RegisteredModel> = entries.values.sortedBy(RegisteredModel::displayName)
    override fun findModel(modelId: String): RegisteredModel? = entries[modelId]

    fun register(path: Path, displayName: String = path.fileName.toString(), modelId: String = UUID.randomUUID().toString()): RegisteredModel {
        require(modelId.isNotBlank()) { "Model id must not be blank." }
        require(displayName.isNotBlank()) { "Model display name must not be blank." }
        check(modelId !in entries) { "Model id '$modelId' is already registered." }
        val canonical = pathValidator.validateSelectedFile(path)
        check(entries.values.none { it.canonicalPath == canonical.toString() }) { "Model file is already registered." }
        return RegisteredModel(
            id = modelId,
            displayName = displayName,
            canonicalPath = canonical.toString(),
            sizeBytes = Files.size(canonical),
            candidateFormat = canonical.fileName.toString().substringAfterLast('.').lowercase(),
        ).also { entries[it.id] = it }
    }

    fun updateDisplayName(modelId: String, displayName: String): RegisteredModel {
        require(displayName.isNotBlank()) { "Model display name must not be blank." }
        val existing = entries[modelId] ?: throw ModelRegistryException("Registered model was not found.")
        return existing.copy(displayName = displayName).also { entries[modelId] = it }
    }

    fun associate(profile: ModelProfile, modelId: String) {
        val model = entries[modelId] ?: throw ModelRegistryException("Registered model was not found.")
        val canonical = pathValidator.validatePersistedFile(Path.of(model.canonicalPath))
        if (canonical.toString() != model.canonicalPath || profile.modelReference != model.canonicalPath) {
            throw ModelRegistryException("Model profile must reference the registered canonical model path.")
        }
        if (profile.id in profileEntries && profileEntries.getValue(profile.id) != profile) {
            throw ModelRegistryException("Model profile id is already associated with a different profile.")
        }
        profileEntries[profile.id] = profile
        entries[modelId] = model.copy(profileIds = model.profileIds + profile.id)
    }

    fun remove(modelId: String) {
        val model = entries[modelId] ?: throw ModelRegistryException("Registered model was not found.")
        if (model.profileIds.isNotEmpty()) {
            throw ModelRegistryException("Remove model profile associations before removing a registered model.")
        }
        entries.remove(modelId)
    }

    fun discover(directory: Path, maxDepth: Int = 2, maxResults: Int = 50): List<DiscoveredModel> {
        require(maxDepth in 0..8) { "Discovery depth must be between 0 and 8." }
        require(maxResults in 1..500) { "Discovery result limit must be between 1 and 500." }
        val root = pathValidator.validateDiscoveryDirectory(directory)
        return Files.walk(root, maxDepth).use { paths ->
            paths.filter { Files.isRegularFile(it) && Files.isReadable(it) && pathValidator.isSupported(it) }
                .limit(maxResults.toLong())
                .map { file ->
                    val canonical = file.toRealPath()
                    DiscoveredModel(
                        canonicalPath = canonical.toString(),
                        displayName = canonical.fileName.toString(),
                        sizeBytes = Files.size(canonical),
                        candidateFormat = canonical.fileName.toString().substringAfterLast('.').lowercase(),
                    )
                }
                .toList()
        }
    }
}

data class ModelRegistryState(
    var models: MutableList<PersistedModel> = mutableListOf(),
    var profiles: MutableList<PersistedProfile> = mutableListOf(),
)

data class PersistedModel(
    var id: String = "",
    var displayName: String = "",
    var canonicalPath: String = "",
    var profileIds: MutableList<String> = mutableListOf(),
)

data class PersistedProfile(
    var modelId: String = "",
    var profile: ModelProfile? = null,
)

/** Application-level, user-specific registry state. Model contents and credentials are never persisted. */
@State(name = "IntegratedAiWorkbenchModelRegistry", storages = [Storage("integrated-ai-workbench-models.xml")])
@Service(Service.Level.APP)
class PersistentModelRegistry : PersistentStateComponent<ModelRegistryState> {
    private var state = ModelRegistryState()
    private var restoredRegistry: InMemoryModelRegistry? = null

    fun registry(): ModelRegistry = restoredRegistry ?: EmptyPersistentRegistry

    /**
     * Registers one user-selected local model and creates its local-only execution profile.
     * The persistent state contains the canonical path and profile metadata, never model contents.
     */
    @Synchronized
    fun registerLocalModel(path: Path, displayName: String = path.fileName.toString()): ModelProfile {
        require(displayName.isNotBlank()) { "Model display name must not be blank." }
        val validator = ModelPathValidator.fromSelections(listOf(path), emptyList())
        val canonical = validator.validateSelectedFile(path)
        if (state.models.any { it.canonicalPath == canonical.toString() }) {
            throw ModelRegistryException("This model is already registered.")
        }
        val modelId = UUID.randomUUID().toString()
        val profile = ModelProfile(
            id = "local-${UUID.randomUUID()}",
            displayName = displayName,
            providerId = "llama-cpp-server",
            modelReference = canonical.toString(),
        )
        val updated = getState().also {
            it.models += PersistedModel(modelId, displayName, canonical.toString(), mutableListOf(profile.id))
            it.profiles += PersistedProfile(modelId, profile)
        }
        loadState(updated)
        return requireNotNull(registry().find(profile.id))
    }

    /**
     * Updates only a registered model's persisted profile. The model reference,
     * provider and stable profile id are immutable after registration so an edit
     * cannot silently point a preset at a different local file.
     */
    @Synchronized
    fun updateLocalProfile(updated: ModelProfile): ModelProfile {
        val current = state.profiles.firstOrNull { it.profile?.id == updated.id }
            ?: throw ModelRegistryException("Registered model profile was not found.")
        val existing = requireNotNull(current.profile)

        require(updated.providerId == existing.providerId) {
            "A model preset cannot change inference provider."
        }
        require(updated.modelReference == existing.modelReference) {
            "A model preset cannot change its registered model file."
        }

        val updatedState = getState()
        val profileIndex = updatedState.profiles.indexOfFirst { it.profile?.id == updated.id }
        updatedState.profiles[profileIndex] = updatedState.profiles[profileIndex].copy(profile = updated)

        val modelId = updatedState.profiles[profileIndex].modelId
        updatedState.models.indexOfFirst { it.id == modelId }
            .takeIf { it >= 0 }
            ?.let { modelIndex ->
                updatedState.models[modelIndex] = updatedState.models[modelIndex].copy(
                    displayName = updated.displayName,
                )
            }

        loadState(updatedState)
        return requireNotNull(registry().find(updated.id))
    }

    override fun getState(): ModelRegistryState = state.copy(
        models = state.models.map { it.copy(profileIds = it.profileIds.toMutableList()) }.toMutableList(),
        profiles = state.profiles.map { it.copy() }.toMutableList(),
    )

    override fun loadState(state: ModelRegistryState) {
        val validModels = state.models.distinctBy(PersistedModel::id).filter { candidate ->
            candidate.id.isNotBlank() && candidate.displayName.isNotBlank() && runCatching {
                val path = Path.of(candidate.canonicalPath)
                path.isAbsolute && Files.isRegularFile(path.toRealPath()) && Files.isReadable(path.toRealPath())
            }.getOrDefault(false)
        }
        val validator = runCatching {
            ModelPathValidator.fromSelections(validModels.map { Path.of(it.canonicalPath) }, emptyList())
        }.getOrNull()
        if (validator == null) {
            this.state = ModelRegistryState()
            restoredRegistry = null
            return
        }
        val rebuilt = InMemoryModelRegistry(validator)
        validModels.forEach { candidate ->
            runCatching { rebuilt.register(Path.of(candidate.canonicalPath), candidate.displayName, candidate.id) }
        }
        state.profiles.forEach { persisted ->
            val profile = persisted.profile
            if (profile != null && rebuilt.findModel(persisted.modelId) != null) {
                runCatching { rebuilt.associate(profile, persisted.modelId) }
            }
        }
        restoredRegistry = rebuilt
        this.state = ModelRegistryState(
            models = rebuilt.models().map { model ->
                PersistedModel(model.id, model.displayName, model.canonicalPath, model.profileIds.toMutableList())
            }.toMutableList(),
            profiles = rebuilt.models().flatMap { model ->
                model.profileIds.mapNotNull { profileId -> rebuilt.find(profileId)?.let { PersistedProfile(model.id, it) } }
            }.toMutableList(),
        )
    }

    private object EmptyPersistentRegistry : ModelRegistry {
        override fun profiles(): List<ModelProfile> = emptyList()
        override fun find(profileId: String): ModelProfile? = null
    }
}
