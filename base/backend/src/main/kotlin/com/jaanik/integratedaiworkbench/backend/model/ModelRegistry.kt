package com.jaanik.integratedaiworkbench.backend.model

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.jaanik.integratedaiworkbench.shared.model.ExecutionPolicy
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
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
    var profile: PersistedModelProfile? = null,
)

/**
 * XML-friendly representation of [ModelProfile]. IntelliJ persistent-state
 * serialization requires a no-argument-compatible object, while ModelProfile
 * intentionally requires validated constructor arguments.
 */
data class PersistedModelProfile(
    var id: String = "",
    var displayName: String = "",
    var providerId: String = "",
    var modelReference: String = "",
    var temperature: Double = 0.2,
    var topP: Double = 0.95,
    var maxOutputTokens: Int = 2_048,
    var contextWindowTokens: Int? = null,
    var timeoutSeconds: Long = 60,
    var executionPolicy: String = ExecutionPolicy.LOCAL_ONLY.name,
    var thinkingLevel: String = ThinkingLevel.DEFAULT.name,
    var structuredOutputSchema: String? = null,
    var requiresNativeStructuredOutput: Boolean = false,
) {
    fun toModelProfile(): ModelProfile? = runCatching {
        ModelProfile(
            id = id,
            displayName = displayName,
            providerId = providerId,
            modelReference = modelReference,
            temperature = temperature,
            topP = topP,
            maxOutputTokens = maxOutputTokens,
            contextWindowTokens = contextWindowTokens,
            timeout = Duration.ofSeconds(timeoutSeconds),
            executionPolicy = ExecutionPolicy.valueOf(executionPolicy),
            thinkingLevel = ThinkingLevel.valueOf(thinkingLevel),
            structuredOutputSchema = structuredOutputSchema,
            requiresNativeStructuredOutput = requiresNativeStructuredOutput,
        )
    }.getOrNull()

    companion object {
        fun from(profile: ModelProfile) = PersistedModelProfile(
            id = profile.id,
            displayName = profile.displayName,
            providerId = profile.providerId,
            modelReference = profile.modelReference,
            temperature = profile.temperature,
            topP = profile.topP,
            maxOutputTokens = profile.maxOutputTokens,
            contextWindowTokens = profile.contextWindowTokens,
            timeoutSeconds = profile.timeout.seconds,
            executionPolicy = profile.executionPolicy.name,
            thinkingLevel = profile.thinkingLevel.name,
            structuredOutputSchema = profile.structuredOutputSchema,
            requiresNativeStructuredOutput = profile.requiresNativeStructuredOutput,
        )
    }
}

/** A saved registry entry for the UI, including temporarily inaccessible files. */
data class PersistentRegisteredModel(
    val modelId: String,
    val displayName: String,
    val canonicalPath: String,
    val profile: ModelProfile?,
    val isAccessible: Boolean,
)

/** Application-level, user-specific registry state. Model contents and credentials are never persisted. */
@State(name = "IntegratedAiWorkbenchModelRegistry", storages = [Storage("integrated-ai-workbench-models.xml")])
@Service(Service.Level.APP)
class PersistentModelRegistry : PersistentStateComponent<ModelRegistryState> {
    private var state = ModelRegistryState()
    private var restoredRegistry: InMemoryModelRegistry? = null

    fun registry(): ModelRegistry = restoredRegistry ?: EmptyPersistentRegistry

    /** Inspects only the selected GGUF header; tensor weights never leave the local file. */
    @Synchronized
    fun inspectGguf(path: Path): GgufModelMetadata {
        val canonical = path.toRealPath()
        require(Files.isRegularFile(canonical) && Files.isReadable(canonical)) { "GGUF file is missing or unreadable." }
        require(canonical.fileName.toString().endsWith(".gguf", ignoreCase = true)) { "Selected file is not a GGUF model." }
        return GgufMetadataReader.read(canonical)
    }

    /**
     * Returns every saved entry. Unlike [registry], this preserves a model whose
     * external file is temporarily offline so it can be reconnected safely.
     */
    @Synchronized
    fun registeredModels(): List<PersistentRegisteredModel> = state.models
        .map { model ->
            val profile = state.profiles.firstOrNull { it.modelId == model.id }?.profile?.toModelProfile()
            PersistentRegisteredModel(
                modelId = model.id,
                displayName = model.displayName,
                canonicalPath = model.canonicalPath,
                profile = profile,
                isAccessible = isAccessibleModel(model.canonicalPath),
            )
        }
        .sortedBy(PersistentRegisteredModel::displayName)

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
            it.profiles += PersistedProfile(modelId, PersistedModelProfile.from(profile))
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
        val existing = requireNotNull(current.profile?.toModelProfile()) {
            "Registered model profile is invalid."
        }

        require(updated.providerId == existing.providerId) {
            "A model preset cannot change inference provider."
        }
        require(updated.modelReference == existing.modelReference) {
            "A model preset cannot change its registered model file."
        }

        val updatedState = getState()
        val profileIndex = updatedState.profiles.indexOfFirst { it.profile?.id == updated.id }
        updatedState.profiles[profileIndex] = updatedState.profiles[profileIndex].copy(
            profile = PersistedModelProfile.from(updated),
        )

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

    /** Removes only the Workbench registry entry and its profile, never the user file. */
    @Synchronized
    fun removeLocalModel(modelId: String) {
        require(state.models.any { it.id == modelId }) { "Registered model was not found." }
        val updated = getState().also {
            it.models.removeIf { model -> model.id == modelId }
            it.profiles.removeIf { profile -> profile.modelId == modelId }
        }
        loadState(updated)
    }

    /** Rebinds an unavailable entry to a user-selected GGUF without creating a duplicate entry. */
    @Synchronized
    fun reconnectLocalModel(modelId: String, path: Path) {
        val validator = ModelPathValidator.fromSelections(listOf(path), emptyList())
        val canonical = validator.validateSelectedFile(path)
        val updated = getState()
        val index = updated.models.indexOfFirst { it.id == modelId }
        require(index >= 0) { "Registered model was not found." }
        require(updated.models.none { it.id != modelId && it.canonicalPath == canonical.toString() }) {
            "This model file is already registered."
        }
        updated.models[index] = updated.models[index].copy(
            canonicalPath = canonical.toString(),
            displayName = updated.models[index].displayName.ifBlank { canonical.fileName.toString() },
        )
        updated.profiles.replaceAll { persisted ->
            if (persisted.modelId == modelId && persisted.profile != null) {
                persisted.copy(profile = persisted.profile!!.copy(modelReference = canonical.toString()))
            } else persisted
        }
        loadState(updated)
    }

    override fun getState(): ModelRegistryState = state.copy(
        models = state.models.map { it.copy(profileIds = it.profileIds.toMutableList()) }.toMutableList(),
        profiles = state.profiles.map { it.copy(profile = it.profile?.copy()) }.toMutableList(),
    )

    override fun loadState(state: ModelRegistryState) {
        val savedModels = state.models.distinctBy(PersistedModel::id).filter { candidate ->
            candidate.id.isNotBlank() && candidate.displayName.isNotBlank() && candidate.canonicalPath.isNotBlank()
        }
        val validModels = savedModels.filter { candidate -> isAccessibleModel(candidate.canonicalPath) }
        val persistedProfiles = state.profiles
            .filter { persisted -> savedModels.any { it.id == persisted.modelId } }
            .distinctBy(PersistedProfile::modelId)
        val normalizedProfiles = savedModels.mapNotNull { model ->
            val persisted = persistedProfiles.firstOrNull { it.modelId == model.id }
            val profile = persisted?.profile?.toModelProfile()
            when {
                profile != null && profile.modelReference == model.canonicalPath -> persisted
                model in validModels -> PersistedProfile(
                    modelId = model.id,
                    profile = recoveredLocalProfile(model, persisted?.profile),
                )
                else -> persisted
            }
        }
        val validator = runCatching {
            ModelPathValidator.fromSelections(validModels.map { Path.of(it.canonicalPath) }, emptyList())
        }.getOrNull()
        val rebuilt = validator?.let(::InMemoryModelRegistry)
        validModels.forEach { candidate ->
            runCatching { rebuilt?.register(Path.of(candidate.canonicalPath), candidate.displayName, candidate.id) }
        }
        normalizedProfiles.forEach { persisted ->
            val profile = persisted.profile?.toModelProfile()
            if (profile != null && rebuilt?.findModel(persisted.modelId) != null) {
                runCatching { rebuilt.associate(profile, persisted.modelId) }
            }
        }
        restoredRegistry = rebuilt
        this.state = ModelRegistryState(
            models = savedModels.map { model ->
                val profileId = normalizedProfiles.firstOrNull { it.modelId == model.id }?.profile?.id
                model.copy(
                    profileIds = profileId?.let { mutableListOf(it) } ?: model.profileIds.toMutableList(),
                )
            }.toMutableList(),
            profiles = normalizedProfiles.map { it.copy(profile = it.profile?.copy()) }.toMutableList(),
        )
    }

    /**
     * The first frontend versions persisted a raw ModelProfile, which IntelliJ's
     * XML serializer restored as an empty object.  An existing readable GGUF is
     * enough to safely reconstruct its local-only profile; unavailable paths are
     * deliberately left untouched until the user reconnects them.
     */
    private fun recoveredLocalProfile(model: PersistedModel, source: PersistedModelProfile?): PersistedModelProfile =
        PersistedModelProfile(
            id = source?.id?.takeIf(String::isNotBlank)
                ?: model.profileIds.firstOrNull(String::isNotBlank)
                ?: "local-${model.id}",
            displayName = source?.displayName?.takeIf(String::isNotBlank) ?: model.displayName,
            providerId = source?.providerId?.takeIf(String::isNotBlank) ?: "llama-cpp-server",
            modelReference = model.canonicalPath,
            temperature = source?.temperature?.takeIf { it.isFinite() && it in 0.0..2.0 } ?: 0.2,
            topP = source?.topP?.takeIf { it.isFinite() && it in 0.0..1.0 } ?: 0.95,
            maxOutputTokens = source?.maxOutputTokens?.takeIf { it > 0 } ?: 2_048,
            contextWindowTokens = source?.contextWindowTokens?.takeIf { it > 0 },
            timeoutSeconds = source?.timeoutSeconds?.takeIf { it > 0 } ?: 60,
            executionPolicy = source?.executionPolicy
                ?.takeIf { policy -> ExecutionPolicy.entries.any { it.name == policy } }
                ?: ExecutionPolicy.LOCAL_ONLY.name,
            thinkingLevel = source?.thinkingLevel
                ?.takeIf { level -> ThinkingLevel.entries.any { it.name == level } }
                ?: ThinkingLevel.DEFAULT.name,
            structuredOutputSchema = source?.structuredOutputSchema?.takeIf(String::isNotBlank),
            requiresNativeStructuredOutput = source?.requiresNativeStructuredOutput == true &&
                source.structuredOutputSchema?.isNotBlank() == true,
        )

    private fun isAccessibleModel(pathText: String): Boolean = runCatching {
        val path = Path.of(pathText)
        path.isAbsolute && Files.isRegularFile(path.toRealPath()) && Files.isReadable(path.toRealPath()) &&
            path.fileName.toString().endsWith(".gguf", ignoreCase = true)
    }.getOrDefault(false)

    private object EmptyPersistentRegistry : ModelRegistry {
        override fun profiles(): List<ModelProfile> = emptyList()
        override fun find(profileId: String): ModelProfile? = null
    }
}
