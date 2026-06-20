package org.bettamind.shared.sync

import org.bettamind.shared.daily.CalendarHandoffDecision
import org.bettamind.shared.daily.CalendarHandoffPolicy
import org.bettamind.shared.daily.DailyToolRecordKind
import org.bettamind.shared.daily.PrivateCalendarEntry
import org.bettamind.shared.privacy.EncryptedBackupPackage
import org.bettamind.shared.privacy.SensitiveAction
import org.bettamind.shared.safety.HarmSafetyDecision
import org.bettamind.shared.safety.HarmSafetyPolicy
import org.bettamind.shared.safety.RelationalBoundaryAssessment
import org.bettamind.shared.safety.RelationalBoundaryPolicy
import org.bettamind.shared.security.sha256Hex
import org.bettamind.shared.support.SafetySupportSummary

const val ExportSyncAlgorithm: String = "XChaCha20-Poly1305"

private val SafeTokenPattern = Regex("[A-Za-z0-9._:-]+")
private val Sha256HexPattern = Regex("[a-fA-F0-9]{64}")

enum class ExportSyncRecordKind {
    DailyTool,
    GrowthNarrative,
    AiGeneratedOutput,
    RelationalBoundaryMetadata,
    HarmSafetyMetadata,
    SafetySupportSummary,
    EncryptedBackup,
    CalendarHandoffReceipt,
}

enum class ExportSyncDecisionReason {
    BackendOptional,
    SyncDisabledByDefault,
    UserApprovalMissing,
    ExplicitSelectionRequired,
    PreviewRequired,
    StepUpAuthenticationRequired,
    EncryptedPayloadRequired,
    CiphertextChecksumMismatch,
    DailyToolExcludedByDefault,
    RelationalSensitiveExcludedByDefault,
    HarmSafetySensitiveExcludedByDefault,
    SupportSummaryExcludedByDefault,
    ActionableHarmDetailsRejected,
    CalendarHandoffRequiresExplicitApproval,
    BroadCalendarReadRejected,
    ConflictWouldOverwriteLocalData,
    DeviceRevocationRequiresExplicitAction,
}

data class ExportSyncRecordDescriptor(
    val recordId: String,
    val kind: ExportSyncRecordKind,
    val dailyKind: DailyToolRecordKind? = null,
    val relationalAssessment: RelationalBoundaryAssessment? = null,
    val harmDecision: HarmSafetyDecision? = null,
    val safetySupportSummary: SafetySupportSummary? = null,
    val containsRawNarrative: Boolean = false,
    val containsActionableHarmDetails: Boolean = false,
    val encryptedAtRest: Boolean = true,
) {
    init {
        require(recordId.isNotBlank() && recordId.matches(SafeTokenPattern)) {
            "Export and sync record IDs must be non-empty and local-safe."
        }
        require(kind == ExportSyncRecordKind.DailyTool || dailyKind == null) {
            "Daily record kind can only be attached to daily-tool records."
        }
        require(kind != ExportSyncRecordKind.DailyTool || dailyKind != null) {
            "Daily-tool records must declare their daily record kind."
        }
    }

    val isDailyToolRecord: Boolean
        get() = kind == ExportSyncRecordKind.DailyTool
}

data class EncryptedSyncEnvelope(
    val schemaVersion: Int = 1,
    val envelopeId: String,
    val deviceId: String,
    val recordId: String,
    val recordKind: ExportSyncRecordKind,
    val manifestVersion: Int,
    val keyVersion: Int,
    val algorithm: String = ExportSyncAlgorithm,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val ciphertextSha256: String = ciphertext.sha256Hex(),
) {
    init {
        require(schemaVersion > 0) { "Sync envelope schema version must be positive." }
        require(manifestVersion > 0) { "Sync manifest version must be positive." }
        require(keyVersion > 0) { "Sync key version must be positive." }
        require(envelopeId.isNotBlank() && envelopeId.matches(SafeTokenPattern)) {
            "Sync envelope IDs must be non-empty and local-safe."
        }
        require(deviceId.isNotBlank() && deviceId.matches(SafeTokenPattern)) {
            "Sync device IDs must be non-empty and local-safe."
        }
        require(recordId.isNotBlank() && recordId.matches(SafeTokenPattern)) {
            "Sync record IDs must be non-empty and local-safe."
        }
        require(algorithm == ExportSyncAlgorithm) {
            "Sync envelopes must declare $ExportSyncAlgorithm."
        }
        require(nonce.isNotEmpty()) { "Sync envelopes require a nonce." }
        require(ciphertext.isNotEmpty()) { "Sync envelopes require ciphertext." }
        require(ciphertextSha256.matches(Sha256HexPattern)) {
            "Sync ciphertext checksum must be SHA-256 hex."
        }
    }

    fun checksumMatches(): Boolean =
        ciphertext.sha256Hex().equals(ciphertextSha256, ignoreCase = true)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedSyncEnvelope) return false
        return schemaVersion == other.schemaVersion &&
            envelopeId == other.envelopeId &&
            deviceId == other.deviceId &&
            recordId == other.recordId &&
            recordKind == other.recordKind &&
            manifestVersion == other.manifestVersion &&
            keyVersion == other.keyVersion &&
            algorithm == other.algorithm &&
            nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext) &&
            ciphertextSha256 == other.ciphertextSha256
    }

    override fun hashCode(): Int {
        var result = schemaVersion
        result = 31 * result + envelopeId.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + recordId.hashCode()
        result = 31 * result + recordKind.hashCode()
        result = 31 * result + manifestVersion
        result = 31 * result + keyVersion
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + ciphertextSha256.hashCode()
        return result
    }
}

data class OptionalBackendSyncSettings(
    val backendConfigured: Boolean = false,
    val syncEnabled: Boolean = false,
    val userApprovedEncryptedSync: Boolean = false,
    val appLockReauthenticationCompleted: Boolean = false,
    val includeDailyToolRecords: Boolean = false,
    val includeSensitiveRelationalRecords: Boolean = false,
    val includeSensitiveHarmSafetyRecords: Boolean = false,
) {
    val backendRequiredForCoreUse: Boolean = false
    val accountRequiredForCoreUse: Boolean = false
    val effectiveSyncEnabled: Boolean =
        backendConfigured &&
            syncEnabled &&
            userApprovedEncryptedSync &&
            appLockReauthenticationCompleted
}

data class ExportPolicyDecision(
    val allowed: Boolean,
    val includedByDefault: Boolean,
    val requiresExplicitSelection: Boolean,
    val requiresPreview: Boolean,
    val requiresStepUpAuthentication: Boolean,
    val sensitiveAction: SensitiveAction? = null,
    val reasons: Set<ExportSyncDecisionReason> = emptySet(),
)

data class SyncPolicyDecision(
    val allowed: Boolean,
    val requiresUserApproval: Boolean,
    val requiresStepUpAuthentication: Boolean,
    val sensitiveAction: SensitiveAction? = null,
    val reasons: Set<ExportSyncDecisionReason> = emptySet(),
) {
    val backendRequiredForCoreUse: Boolean = false
    val accountRequiredForCoreUse: Boolean = false
}

object BettamindExportSyncPolicy {
    fun reviewExport(
        descriptor: ExportSyncRecordDescriptor,
        encryptedPackagePrepared: Boolean,
        explicitSelection: Boolean = false,
        previewAccepted: Boolean = false,
        stepUpAuthenticationCompleted: Boolean = false,
    ): ExportPolicyDecision {
        val reasons = linkedSetOf<ExportSyncDecisionReason>()
        val sensitive = descriptor.isPrivateOrSensitive()
        val requiresExplicitSelection = sensitive || descriptor.isDailyToolRecord
        val requiresPreview = sensitive || descriptor.isDailyToolRecord
        val requiresStepUpAuthentication = sensitive || descriptor.isDailyToolRecord

        if (!encryptedPackagePrepared || !descriptor.encryptedAtRest) {
            reasons += ExportSyncDecisionReason.EncryptedPayloadRequired
        }
        if (descriptor.isDailyToolRecord) {
            reasons += ExportSyncDecisionReason.DailyToolExcludedByDefault
        }
        descriptor.relationalAssessment?.let { assessment ->
            if (!RelationalBoundaryPolicy.reviewExport(assessment).allowed) {
                reasons += ExportSyncDecisionReason.RelationalSensitiveExcludedByDefault
            }
        }
        descriptor.harmDecision?.let { decision ->
            val exportDecision = HarmSafetyPolicy.reviewExport(
                decision = decision,
                explicitSelection = explicitSelection,
                previewAccepted = previewAccepted,
            )
            if (exportDecision.excludedByDefault) {
                reasons += ExportSyncDecisionReason.HarmSafetySensitiveExcludedByDefault
            }
        }
        descriptor.safetySupportSummary?.let { summary ->
            if (!summary.exportAllowedByDefault) {
                reasons += ExportSyncDecisionReason.SupportSummaryExcludedByDefault
            }
        }
        if (descriptor.containsActionableHarmDetails) {
            reasons += ExportSyncDecisionReason.ActionableHarmDetailsRejected
        }
        if (requiresExplicitSelection && !explicitSelection) {
            reasons += ExportSyncDecisionReason.ExplicitSelectionRequired
        }
        if (requiresPreview && !previewAccepted) {
            reasons += ExportSyncDecisionReason.PreviewRequired
        }
        if (requiresStepUpAuthentication && !stepUpAuthenticationCompleted) {
            reasons += ExportSyncDecisionReason.StepUpAuthenticationRequired
        }

        val allowed = ExportSyncDecisionReason.EncryptedPayloadRequired !in reasons &&
            ExportSyncDecisionReason.ActionableHarmDetailsRejected !in reasons &&
            (!requiresExplicitSelection || explicitSelection) &&
            (!requiresPreview || previewAccepted) &&
            (!requiresStepUpAuthentication || stepUpAuthenticationCompleted)

        return ExportPolicyDecision(
            allowed = allowed,
            includedByDefault = !requiresExplicitSelection,
            requiresExplicitSelection = requiresExplicitSelection,
            requiresPreview = requiresPreview,
            requiresStepUpAuthentication = requiresStepUpAuthentication,
            sensitiveAction = SensitiveAction.ExportPrivateInformation.takeIf { requiresStepUpAuthentication },
            reasons = reasons,
        )
    }

    fun reviewSync(
        descriptor: ExportSyncRecordDescriptor,
        settings: OptionalBackendSyncSettings = OptionalBackendSyncSettings(),
        envelope: EncryptedSyncEnvelope? = null,
    ): SyncPolicyDecision {
        val reasons = linkedSetOf<ExportSyncDecisionReason>()

        if (!settings.backendConfigured) {
            reasons += ExportSyncDecisionReason.BackendOptional
        }
        if (!settings.syncEnabled) {
            reasons += ExportSyncDecisionReason.SyncDisabledByDefault
        }
        if (!settings.userApprovedEncryptedSync) {
            reasons += ExportSyncDecisionReason.UserApprovalMissing
        }
        if (!settings.appLockReauthenticationCompleted) {
            reasons += ExportSyncDecisionReason.StepUpAuthenticationRequired
        }
        if (envelope == null) {
            reasons += ExportSyncDecisionReason.EncryptedPayloadRequired
        } else if (!envelope.checksumMatches()) {
            reasons += ExportSyncDecisionReason.CiphertextChecksumMismatch
        }
        if (descriptor.isDailyToolRecord && !settings.includeDailyToolRecords) {
            reasons += ExportSyncDecisionReason.DailyToolExcludedByDefault
        }
        descriptor.relationalAssessment?.let { assessment ->
            val relationalAllowedByDefault = RelationalBoundaryPolicy.reviewSync(assessment).allowed
            if (!relationalAllowedByDefault && !settings.includeSensitiveRelationalRecords) {
                reasons += ExportSyncDecisionReason.RelationalSensitiveExcludedByDefault
            }
        }
        descriptor.harmDecision?.let { decision ->
            val syncDecision = HarmSafetyPolicy.reviewSync(decision)
            if (syncDecision.excludedByDefault && !settings.includeSensitiveHarmSafetyRecords) {
                reasons += ExportSyncDecisionReason.HarmSafetySensitiveExcludedByDefault
            }
        }
        descriptor.safetySupportSummary?.let { summary ->
            if (!summary.syncAllowedByDefault && !settings.includeSensitiveHarmSafetyRecords) {
                reasons += ExportSyncDecisionReason.SupportSummaryExcludedByDefault
            }
        }
        if (!descriptor.encryptedAtRest) {
            reasons += ExportSyncDecisionReason.EncryptedPayloadRequired
        }
        if (descriptor.containsActionableHarmDetails) {
            reasons += ExportSyncDecisionReason.ActionableHarmDetailsRejected
        }

        return SyncPolicyDecision(
            allowed = reasons.isEmpty(),
            requiresUserApproval = true,
            requiresStepUpAuthentication = true,
            sensitiveAction = SensitiveAction.EnableSync,
            reasons = reasons,
        )
    }

    fun reviewEncryptedBackupExport(
        backup: EncryptedBackupPackage,
        explicitSelection: Boolean,
        previewAccepted: Boolean,
        stepUpAuthenticationCompleted: Boolean,
    ): ExportPolicyDecision =
        reviewExport(
            descriptor = ExportSyncRecordDescriptor(
                recordId = "encrypted-backup-v${backup.formatVersion}",
                kind = ExportSyncRecordKind.EncryptedBackup,
                containsRawNarrative = true,
                encryptedAtRest = true,
            ),
            encryptedPackagePrepared = backup.ciphertext.isNotEmpty(),
            explicitSelection = explicitSelection,
            previewAccepted = previewAccepted,
            stepUpAuthenticationCompleted = stepUpAuthenticationCompleted,
        )

    fun reviewEncryptedBackupRestore(
        backup: EncryptedBackupPackage,
        explicitSelection: Boolean,
        stepUpAuthenticationCompleted: Boolean,
    ): ExportPolicyDecision {
        val reasons = linkedSetOf<ExportSyncDecisionReason>()
        if (backup.ciphertext.isEmpty()) {
            reasons += ExportSyncDecisionReason.EncryptedPayloadRequired
        }
        if (!explicitSelection) {
            reasons += ExportSyncDecisionReason.ExplicitSelectionRequired
        }
        if (!stepUpAuthenticationCompleted) {
            reasons += ExportSyncDecisionReason.StepUpAuthenticationRequired
        }
        return ExportPolicyDecision(
            allowed = reasons.isEmpty(),
            includedByDefault = false,
            requiresExplicitSelection = true,
            requiresPreview = false,
            requiresStepUpAuthentication = true,
            sensitiveAction = SensitiveAction.ViewRecoveryData,
            reasons = reasons,
        )
    }

    fun reviewCalendarHandoff(
        entry: PrivateCalendarEntry,
        explicitUserApproval: Boolean,
    ): CalendarHandoffDecision {
        val decision = CalendarHandoffPolicy.evaluate(
            entry = entry,
            explicitUserApproval = explicitUserApproval,
        )
        require(!decision.readsSystemCalendar) {
            ExportSyncDecisionReason.BroadCalendarReadRejected.name
        }
        return decision
    }

    private fun ExportSyncRecordDescriptor.isPrivateOrSensitive(): Boolean =
        containsRawNarrative ||
            isDailyToolRecord ||
            relationalAssessment?.let { !RelationalBoundaryPolicy.reviewExport(it).allowed } == true ||
            harmDecision?.let { HarmSafetyPolicy.reviewExport(it).excludedByDefault } == true ||
            safetySupportSummary?.exportAllowedByDefault == false ||
            kind in setOf(
                ExportSyncRecordKind.GrowthNarrative,
                ExportSyncRecordKind.AiGeneratedOutput,
                ExportSyncRecordKind.RelationalBoundaryMetadata,
                ExportSyncRecordKind.HarmSafetyMetadata,
                ExportSyncRecordKind.SafetySupportSummary,
                ExportSyncRecordKind.EncryptedBackup,
            )
}

data class SyncRecordVersion(
    val recordId: String,
    val updatedAtEpochMillis: Long,
    val encryptedPayloadSha256: String,
    val sourceDeviceId: String,
    val deleted: Boolean = false,
) {
    init {
        require(recordId.isNotBlank() && recordId.matches(SafeTokenPattern)) {
            "Sync record version IDs must be non-empty and local-safe."
        }
        require(updatedAtEpochMillis >= 0) { "Sync record versions require non-negative update times." }
        require(encryptedPayloadSha256.matches(Sha256HexPattern)) {
            "Sync record versions require SHA-256 payload checksums."
        }
        require(sourceDeviceId.isNotBlank() && sourceDeviceId.matches(SafeTokenPattern)) {
            "Sync source device IDs must be non-empty and local-safe."
        }
    }
}

enum class SyncConflictResolutionAction {
    NoConflict,
    KeepLocal,
    KeepRemote,
    KeepBothAndRequireReview,
}

data class SyncConflictResolution(
    val action: SyncConflictResolutionAction,
    val requiresUserReview: Boolean,
    val overwritesLocalData: Boolean,
    val reasons: Set<ExportSyncDecisionReason> = emptySet(),
)

object SyncConflictResolver {
    fun resolve(
        local: SyncRecordVersion?,
        remote: SyncRecordVersion?,
    ): SyncConflictResolution =
        when {
            local == null && remote == null -> SyncConflictResolution(
                action = SyncConflictResolutionAction.NoConflict,
                requiresUserReview = false,
                overwritesLocalData = false,
            )

            local == null -> SyncConflictResolution(
                action = SyncConflictResolutionAction.KeepRemote,
                requiresUserReview = false,
                overwritesLocalData = false,
            )

            remote == null -> SyncConflictResolution(
                action = SyncConflictResolutionAction.KeepLocal,
                requiresUserReview = false,
                overwritesLocalData = false,
            )

            local.encryptedPayloadSha256 == remote.encryptedPayloadSha256 -> SyncConflictResolution(
                action = SyncConflictResolutionAction.NoConflict,
                requiresUserReview = false,
                overwritesLocalData = false,
            )

            else -> SyncConflictResolution(
                action = SyncConflictResolutionAction.KeepBothAndRequireReview,
                requiresUserReview = true,
                overwritesLocalData = false,
                reasons = setOf(ExportSyncDecisionReason.ConflictWouldOverwriteLocalData),
            )
        }
}

data class DeviceRevocationRecord(
    val schemaVersion: Int = 1,
    val revocationVersion: Int,
    val deviceId: String,
    val revokedAtEpochMillis: Long,
    val reason: String,
) {
    init {
        require(schemaVersion > 0) { "Device revocation schema version must be positive." }
        require(revocationVersion > 0) { "Device revocation version must be positive." }
        require(deviceId.isNotBlank() && deviceId.matches(SafeTokenPattern)) {
            "Device IDs must be non-empty and local-safe."
        }
        require(revokedAtEpochMillis >= 0) { "Device revocation time must be non-negative." }
        require(reason.isNotBlank()) { "Device revocation needs a reason." }
    }
}

data class DeviceRevocationDecision(
    val allowed: Boolean,
    val record: DeviceRevocationRecord? = null,
    val requiresExplicitUserAction: Boolean = true,
    val requiresStepUpAuthentication: Boolean = true,
    val sensitiveAction: SensitiveAction = SensitiveAction.ChangeSecuritySettings,
    val reasons: Set<ExportSyncDecisionReason> = emptySet(),
)

object DeviceRevocationPolicy {
    fun revoke(
        deviceId: String,
        nextRevocationVersion: Int,
        revokedAtEpochMillis: Long,
        reason: String,
        explicitUserAction: Boolean,
        stepUpAuthenticationCompleted: Boolean,
    ): DeviceRevocationDecision {
        val reasons = linkedSetOf<ExportSyncDecisionReason>()
        if (!explicitUserAction) {
            reasons += ExportSyncDecisionReason.DeviceRevocationRequiresExplicitAction
        }
        if (!stepUpAuthenticationCompleted) {
            reasons += ExportSyncDecisionReason.StepUpAuthenticationRequired
        }
        val record = if (reasons.isEmpty()) {
            DeviceRevocationRecord(
                revocationVersion = nextRevocationVersion,
                deviceId = deviceId,
                revokedAtEpochMillis = revokedAtEpochMillis,
                reason = reason,
            )
        } else {
            null
        }
        return DeviceRevocationDecision(
            allowed = reasons.isEmpty(),
            record = record,
            reasons = reasons,
        )
    }
}

data class SyncManifest(
    val schemaVersion: Int = 1,
    val manifestVersion: Int,
    val deviceId: String,
    val envelopeIds: List<String>,
    val revokedDeviceIds: List<String> = emptyList(),
) {
    init {
        require(schemaVersion > 0) { "Sync manifest schema version must be positive." }
        require(manifestVersion > 0) { "Sync manifest version must be positive." }
        require(deviceId.isNotBlank() && deviceId.matches(SafeTokenPattern)) {
            "Sync manifest device ID must be non-empty and local-safe."
        }
        require(envelopeIds.all { it.isNotBlank() && it.matches(SafeTokenPattern) }) {
            "Sync manifest envelope IDs must be local-safe."
        }
        require(revokedDeviceIds.all { it.isNotBlank() && it.matches(SafeTokenPattern) }) {
            "Sync manifest revoked device IDs must be local-safe."
        }
    }
}

object CiphertextOnlyBackendContract {
    fun accepts(envelope: EncryptedSyncEnvelope): Boolean =
        envelope.checksumMatches()

    fun rejectsPlaintextFieldNames(fieldNames: Set<String>): Boolean =
        fieldNames.any { fieldName ->
            fieldName.equals("plaintext", ignoreCase = true) ||
                fieldName.equals("plainText", ignoreCase = true) ||
                fieldName.equals("journalText", ignoreCase = true) ||
                fieldName.equals("rawNarrative", ignoreCase = true)
        }
}

fun encryptedBackupAsSyncEnvelope(
    backup: EncryptedBackupPackage,
    envelopeId: String,
    deviceId: String,
    manifestVersion: Int,
    keyVersion: Int,
    nonce: ByteArray,
): EncryptedSyncEnvelope =
    EncryptedSyncEnvelope(
        envelopeId = envelopeId,
        deviceId = deviceId,
        recordId = "encrypted-backup-v${backup.formatVersion}",
        recordKind = ExportSyncRecordKind.EncryptedBackup,
        manifestVersion = manifestVersion,
        keyVersion = keyVersion,
        nonce = nonce,
        ciphertext = backup.ciphertext,
    )
