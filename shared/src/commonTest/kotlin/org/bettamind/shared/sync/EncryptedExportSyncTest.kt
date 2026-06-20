package org.bettamind.shared.sync

import org.bettamind.shared.daily.DailyToolRecordKind
import org.bettamind.shared.daily.MinuteOfDay
import org.bettamind.shared.daily.PrivateCalendarEntry
import org.bettamind.shared.privacy.EncryptedBackupPackage
import org.bettamind.shared.privacy.SensitiveAction
import org.bettamind.shared.safety.HarmSafetyPolicy
import org.bettamind.shared.safety.RelationalBoundaryPolicy
import org.bettamind.shared.security.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EncryptedExportSyncTest {
    @Test
    fun syncIsOptionalDisabledByDefaultAndDoesNotRequireBackendForCoreUse() {
        val decision = BettamindExportSyncPolicy.reviewSync(
            descriptor = dailyDescriptor(),
        )

        assertFalse(decision.allowed)
        assertFalse(decision.backendRequiredForCoreUse)
        assertFalse(decision.accountRequiredForCoreUse)
        assertTrue(ExportSyncDecisionReason.BackendOptional in decision.reasons)
        assertTrue(ExportSyncDecisionReason.SyncDisabledByDefault in decision.reasons)
        assertTrue(ExportSyncDecisionReason.UserApprovalMissing in decision.reasons)
        assertTrue(ExportSyncDecisionReason.StepUpAuthenticationRequired in decision.reasons)
        assertTrue(ExportSyncDecisionReason.DailyToolExcludedByDefault in decision.reasons)
        assertEquals(SensitiveAction.EnableSync, decision.sensitiveAction)
    }

    @Test
    fun dailyToolExportRequiresEncryptedPackageExplicitPreviewAndStepUp() {
        val defaultDecision = BettamindExportSyncPolicy.reviewExport(
            descriptor = dailyDescriptor(),
            encryptedPackagePrepared = true,
        )

        assertFalse(defaultDecision.allowed)
        assertFalse(defaultDecision.includedByDefault)
        assertTrue(defaultDecision.requiresExplicitSelection)
        assertTrue(defaultDecision.requiresPreview)
        assertTrue(defaultDecision.requiresStepUpAuthentication)
        assertTrue(ExportSyncDecisionReason.DailyToolExcludedByDefault in defaultDecision.reasons)
        assertTrue(ExportSyncDecisionReason.ExplicitSelectionRequired in defaultDecision.reasons)
        assertTrue(ExportSyncDecisionReason.PreviewRequired in defaultDecision.reasons)
        assertTrue(ExportSyncDecisionReason.StepUpAuthenticationRequired in defaultDecision.reasons)
        assertEquals(SensitiveAction.ExportPrivateInformation, defaultDecision.sensitiveAction)

        val approved = BettamindExportSyncPolicy.reviewExport(
            descriptor = dailyDescriptor(),
            encryptedPackagePrepared = true,
            explicitSelection = true,
            previewAccepted = true,
            stepUpAuthenticationCompleted = true,
        )

        assertTrue(approved.allowed)
        assertTrue(ExportSyncDecisionReason.DailyToolExcludedByDefault in approved.reasons)
    }

    @Test
    fun relationalAndHarmSafetyRecordsAreExcludedByDefault() {
        val relationalDescriptor = ExportSyncRecordDescriptor(
            recordId = "relational-1",
            kind = ExportSyncRecordKind.RelationalBoundaryMetadata,
            relationalAssessment = RelationalBoundaryPolicy.assessUserInput("Bettamind, I love you. Be my soulmate."),
        )
        val harmDescriptor = ExportSyncRecordDescriptor(
            recordId = "harm-1",
            kind = ExportSyncRecordKind.HarmSafetyMetadata,
            harmDecision = HarmSafetyPolicy.assessUserInput("I want to hurt myself tonight."),
        )

        val relationalExport = BettamindExportSyncPolicy.reviewExport(
            descriptor = relationalDescriptor,
            encryptedPackagePrepared = true,
        )
        val harmExport = BettamindExportSyncPolicy.reviewExport(
            descriptor = harmDescriptor,
            encryptedPackagePrepared = true,
        )
        val relationalSync = BettamindExportSyncPolicy.reviewSync(
            descriptor = relationalDescriptor,
            settings = approvedSyncSettings(),
            envelope = envelope(recordId = "relational-1", kind = ExportSyncRecordKind.RelationalBoundaryMetadata),
        )
        val harmSync = BettamindExportSyncPolicy.reviewSync(
            descriptor = harmDescriptor,
            settings = approvedSyncSettings(),
            envelope = envelope(recordId = "harm-1", kind = ExportSyncRecordKind.HarmSafetyMetadata),
        )

        assertFalse(relationalExport.allowed)
        assertFalse(harmExport.allowed)
        assertTrue(ExportSyncDecisionReason.RelationalSensitiveExcludedByDefault in relationalExport.reasons)
        assertTrue(ExportSyncDecisionReason.HarmSafetySensitiveExcludedByDefault in harmExport.reasons)
        assertFalse(relationalSync.allowed)
        assertFalse(harmSync.allowed)
        assertTrue(ExportSyncDecisionReason.RelationalSensitiveExcludedByDefault in relationalSync.reasons)
        assertTrue(ExportSyncDecisionReason.HarmSafetySensitiveExcludedByDefault in harmSync.reasons)
    }

    @Test
    fun ciphertextOnlyBackendContractRejectsPlaintextAndChecksumMismatch() {
        val validEnvelope = envelope(recordId = "backup-1", kind = ExportSyncRecordKind.EncryptedBackup)
        val tamperedEnvelope = validEnvelope.copy(ciphertextSha256 = "0".repeat(64))

        assertTrue(CiphertextOnlyBackendContract.accepts(validEnvelope))
        assertFalse(CiphertextOnlyBackendContract.accepts(tamperedEnvelope))
        assertTrue(
            CiphertextOnlyBackendContract.rejectsPlaintextFieldNames(
                setOf("schemaVersion", "ciphertext", "rawNarrative"),
            ),
        )
    }

    @Test
    fun approvedEncryptedDailySyncStillRequiresUserIncludedDailyRecords() {
        val withoutDailyInclusion = BettamindExportSyncPolicy.reviewSync(
            descriptor = dailyDescriptor(),
            settings = approvedSyncSettings(includeDailyToolRecords = false),
            envelope = envelope(recordId = "daily-1", kind = ExportSyncRecordKind.DailyTool),
        )
        val withDailyInclusion = BettamindExportSyncPolicy.reviewSync(
            descriptor = dailyDescriptor(),
            settings = approvedSyncSettings(includeDailyToolRecords = true),
            envelope = envelope(recordId = "daily-1", kind = ExportSyncRecordKind.DailyTool),
        )

        assertFalse(withoutDailyInclusion.allowed)
        assertTrue(ExportSyncDecisionReason.DailyToolExcludedByDefault in withoutDailyInclusion.reasons)
        assertTrue(withDailyInclusion.allowed)
    }

    @Test
    fun divergentSyncConflictsKeepBothAndRequireReview() {
        val local = version(recordId = "daily-1", payload = "local encrypted bytes")
        val remote = version(recordId = "daily-1", payload = "remote encrypted bytes")

        val resolution = SyncConflictResolver.resolve(local, remote)

        assertEquals(SyncConflictResolutionAction.KeepBothAndRequireReview, resolution.action)
        assertTrue(resolution.requiresUserReview)
        assertFalse(resolution.overwritesLocalData)
        assertTrue(ExportSyncDecisionReason.ConflictWouldOverwriteLocalData in resolution.reasons)
    }

    @Test
    fun deviceRevocationRequiresExplicitActionAndStepUpAndProducesVersionedRecord() {
        val blocked = DeviceRevocationPolicy.revoke(
            deviceId = "phone-a",
            nextRevocationVersion = 1,
            revokedAtEpochMillis = 1000L,
            reason = "owner removed device",
            explicitUserAction = false,
            stepUpAuthenticationCompleted = false,
        )

        assertFalse(blocked.allowed)
        assertTrue(ExportSyncDecisionReason.DeviceRevocationRequiresExplicitAction in blocked.reasons)
        assertTrue(ExportSyncDecisionReason.StepUpAuthenticationRequired in blocked.reasons)

        val approved = DeviceRevocationPolicy.revoke(
            deviceId = "phone-a",
            nextRevocationVersion = 2,
            revokedAtEpochMillis = 2000L,
            reason = "owner removed device",
            explicitUserAction = true,
            stepUpAuthenticationCompleted = true,
        )

        assertTrue(approved.allowed)
        assertNotNull(approved.record)
        assertEquals(1, approved.record.schemaVersion)
        assertEquals(2, approved.record.revocationVersion)
        assertEquals("phone-a", approved.record.deviceId)
    }

    @Test
    fun encryptedBackupExportRestoreAndEnvelopeAreSupportedWithoutPlaintext() {
        val backup = EncryptedBackupPackage(
            formatVersion = 1,
            ciphertext = "encrypted backup bytes".encodeToByteArray(),
        )

        val exportDecision = BettamindExportSyncPolicy.reviewEncryptedBackupExport(
            backup = backup,
            explicitSelection = true,
            previewAccepted = true,
            stepUpAuthenticationCompleted = true,
        )
        val restoreDecision = BettamindExportSyncPolicy.reviewEncryptedBackupRestore(
            backup = backup,
            explicitSelection = true,
            stepUpAuthenticationCompleted = true,
        )
        val syncEnvelope = encryptedBackupAsSyncEnvelope(
            backup = backup,
            envelopeId = "backup-envelope-1",
            deviceId = "phone-a",
            manifestVersion = 1,
            keyVersion = 1,
            nonce = byteArrayOf(1, 2, 3, 4),
        )

        assertTrue(exportDecision.allowed)
        assertTrue(restoreDecision.allowed)
        assertTrue(CiphertextOnlyBackendContract.accepts(syncEnvelope))
        assertEquals(ExportSyncRecordKind.EncryptedBackup, syncEnvelope.recordKind)
    }

    @Test
    fun systemCalendarHandoffRequiresApprovalAndDoesNotReadBroadCalendarData() {
        val entry = PrivateCalendarEntry(
            localDate = "2026-06-20",
            titleKey = "daily_calendar_checkin_title",
            startMinuteOfDay = MinuteOfDay(9 * 60),
            durationMinutes = 15,
            systemHandoffRequested = true,
        )

        val blocked = BettamindExportSyncPolicy.reviewCalendarHandoff(
            entry = entry,
            explicitUserApproval = false,
        )
        val approved = BettamindExportSyncPolicy.reviewCalendarHandoff(
            entry = entry,
            explicitUserApproval = true,
        )

        assertTrue(blocked.keepPrivateInApp)
        assertFalse(blocked.offerSystemCalendarHandoff)
        assertTrue(approved.offerSystemCalendarHandoff)
        assertFalse(approved.readsSystemCalendar)
        assertFalse(approved.includesPrivateNotes)
    }

    private fun dailyDescriptor(): ExportSyncRecordDescriptor =
        ExportSyncRecordDescriptor(
            recordId = "daily-1",
            kind = ExportSyncRecordKind.DailyTool,
            dailyKind = DailyToolRecordKind.CheckIn,
        )

    private fun approvedSyncSettings(
        includeDailyToolRecords: Boolean = false,
    ): OptionalBackendSyncSettings =
        OptionalBackendSyncSettings(
            backendConfigured = true,
            syncEnabled = true,
            userApprovedEncryptedSync = true,
            appLockReauthenticationCompleted = true,
            includeDailyToolRecords = includeDailyToolRecords,
        )

    private fun envelope(
        recordId: String,
        kind: ExportSyncRecordKind,
    ): EncryptedSyncEnvelope {
        val ciphertext = "ciphertext for $recordId".encodeToByteArray()
        return EncryptedSyncEnvelope(
            envelopeId = "envelope-$recordId",
            deviceId = "phone-a",
            recordId = recordId,
            recordKind = kind,
            manifestVersion = 1,
            keyVersion = 1,
            nonce = byteArrayOf(9, 8, 7, 6),
            ciphertext = ciphertext,
        )
    }

    private fun version(
        recordId: String,
        payload: String,
    ): SyncRecordVersion =
        SyncRecordVersion(
            recordId = recordId,
            updatedAtEpochMillis = 1000L,
            encryptedPayloadSha256 = payload.encodeToByteArray().sha256Hex(),
            sourceDeviceId = "phone-a",
        )
}
