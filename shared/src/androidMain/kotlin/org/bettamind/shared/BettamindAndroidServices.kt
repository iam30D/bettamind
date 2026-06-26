package org.bettamind.shared

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.bettamind.shared.ai.BettamindModelPackTrustPolicy
import org.bettamind.shared.ai.ModelPackTrustReleaseGate
import org.bettamind.shared.privacy.AndroidKeystoreStorageKeyManager
import org.bettamind.shared.privacy.AndroidSqlCipherEncryptedRecordStore

fun createAndroidBettamindAppServices(context: Context): BettamindAppServices =
    BettamindAppServices(
        dailyRecords = EncryptedDailyRecordService(
            store = AndroidSqlCipherEncryptedRecordStore(context),
            keyManager = AndroidKeystoreStorageKeyManager(context),
            nowEpochMillis = { System.currentTimeMillis() },
            localDate = { AndroidLocalDateFormatter.format(Date()) },
        ),
        reminders = AndroidReminderPlatformService,
        calendar = AndroidCalendarPlatformService,
        speech = AndroidSpeechPlatformService,
        modelPacks = AndroidModelPackPlatformService,
    )

private val AndroidLocalDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private object AndroidReminderPlatformService : ReminderPlatformService {
    override fun status(): ReminderPlatformStatus =
        ReminderPlatformStatus(
            available = true,
            neutralPreviewOnly = true,
            platformName = "android-local-notifications",
        )
}

private object AndroidCalendarPlatformService : CalendarPlatformService {
    override fun status(): CalendarPlatformStatus =
        CalendarPlatformStatus(
            available = true,
            explicitHandoffOnly = true,
            readsBroadCalendarData = false,
            platformName = "android-calendar-insert-intent",
        )
}

private object AndroidSpeechPlatformService : SpeechPlatformService {
    override fun status(): SpeechPlatformStatus =
        SpeechPlatformStatus(
            available = true,
            osOfflinePreferred = true,
            requiresExplicitMicrophonePermission = true,
            textFallbackAvailable = true,
            platformName = "android-os-speech",
        )
}

private object AndroidModelPackPlatformService : ModelPackPlatformService {
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
