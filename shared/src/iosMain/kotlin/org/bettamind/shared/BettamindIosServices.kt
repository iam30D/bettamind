package org.bettamind.shared

import org.bettamind.shared.ai.BettamindModelPackTrustPolicy
import org.bettamind.shared.ai.ModelPackTrustReleaseGate
import org.bettamind.shared.privacy.IosKeychainStorageKeyManager
import org.bettamind.shared.privacy.IosSqlCipherEncryptedRecordStore
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

fun createIosBettamindAppServices(): BettamindAppServices =
    BettamindAppServices(
        dailyRecords = EncryptedDailyRecordService(
            store = IosSqlCipherEncryptedRecordStore(),
            keyManager = IosKeychainStorageKeyManager(),
            nowEpochMillis = { (NSDate().timeIntervalSince1970 * 1000).toLong() },
            localDate = { IosLocalDateFormatter.stringFromDate(NSDate()) },
        ),
        reminders = IosReminderPlatformService,
        calendar = IosCalendarPlatformService,
        speech = IosSpeechPlatformService,
        modelPacks = IosModelPackPlatformService,
    )

private val IosLocalDateFormatter = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
}

private object IosReminderPlatformService : ReminderPlatformService {
    override fun status(): ReminderPlatformStatus =
        ReminderPlatformStatus(
            available = true,
            neutralPreviewOnly = true,
            platformName = "ios-user-notifications",
        )
}

private object IosCalendarPlatformService : CalendarPlatformService {
    override fun status(): CalendarPlatformStatus =
        CalendarPlatformStatus(
            available = true,
            explicitHandoffOnly = true,
            readsBroadCalendarData = false,
            platformName = "ios-eventkit-handoff",
        )
}

private object IosSpeechPlatformService : SpeechPlatformService {
    override fun status(): SpeechPlatformStatus =
        SpeechPlatformStatus(
            available = true,
            osOfflinePreferred = true,
            requiresExplicitMicrophonePermission = true,
            textFallbackAvailable = true,
            platformName = "ios-os-speech",
        )
}

private object IosModelPackPlatformService : ModelPackPlatformService {
    override fun status(): ModelPackPlatformStatus {
        val trustReady = BettamindModelPackTrustPolicy.releaseGate() == ModelPackTrustReleaseGate.Ready
        return ModelPackPlatformStatus(
            installerAvailable = trustReady,
            runtimeAvailable = false,
            firstModelId = BettamindModelPackTrustPolicy.firstProductionModelId,
            requiresSignedManifest = true,
            autoDownloadDisabled = true,
        )
    }
}
