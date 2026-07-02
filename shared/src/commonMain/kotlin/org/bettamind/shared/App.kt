package org.bettamind.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.bettamind.shared.ai.AiGrowthMode
import org.bettamind.shared.ai.AiGrowthModeEngine
import org.bettamind.shared.ai.AiGrowthModeRequest
import org.bettamind.shared.ai.AiGrowthModeResponse
import org.bettamind.shared.ai.AiGrowthFallbackReason
import org.bettamind.shared.ai.AiGrowthResponseSource
import org.bettamind.shared.daily.BreathingExerciseCatalog
import org.bettamind.shared.daily.DailyMetricLevel
import org.bettamind.shared.daily.DecisionWorksheetCatalog
import org.bettamind.shared.daily.DecisionWorksheetKind
import org.bettamind.shared.daily.GroundingExerciseCatalog
import org.bettamind.shared.growth.AdultGateState
import org.bettamind.shared.growth.DeterministicGrowthEngine
import org.bettamind.shared.growth.GrowthSessionState
import org.bettamind.shared.growth.GrowthStep
import org.bettamind.shared.growth.NarrativeStorageStatus
import org.bettamind.shared.design.BettamindThemeMode
import org.bettamind.shared.design.BettamindTheme
import org.bettamind.shared.generated.resources.Res
import org.bettamind.shared.generated.resources.*
import org.bettamind.shared.privacy.PrivacyLockTimeout
import org.bettamind.shared.support.LocalSupportResourceScope
import org.bettamind.shared.support.SafetySupportActionType
import org.bettamind.shared.support.SafetySupportBridgeEngine
import org.bettamind.shared.support.SafetySupportDecision
import org.bettamind.shared.support.SafetySupportRiskLevel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BettamindDestination(
    val title: StringResource,
    val description: StringResource,
) {
    Today(Res.string.destination_today, Res.string.destination_today_description),
    Reflect(Res.string.destination_reflect, Res.string.destination_reflect_description),
    Grow(Res.string.destination_grow, Res.string.destination_grow_description),
    Support(Res.string.destination_support, Res.string.destination_support_description),
    Settings(Res.string.destination_settings, Res.string.destination_settings_description),
}

private val primaryDestinations = BettamindDestination.entries

@Composable
fun BettamindApp(services: BettamindAppServices = BettamindAppServices()) {
    val coroutineScope = rememberCoroutineScope()
    val modelPackStatus by services.modelPacks.status.collectAsState()
    val aiGrowthEngine = remember(services.aiRuntime) { AiGrowthModeEngine(services.aiRuntime) }
    val supportEngine = remember { SafetySupportBridgeEngine() }
    var selectedDestination by remember { mutableStateOf(BettamindDestination.Today) }
    var themeMode by remember { mutableStateOf(BettamindThemeMode.Light) }
    var useAccessibleTypography by remember { mutableStateOf(false) }
    var useReducedMotion by remember { mutableStateOf(false) }
    var useLowLiteracyCopy by remember { mutableStateOf(false) }
    var growthState by remember { mutableStateOf(DeterministicGrowthEngine.locked()) }
    var privacyLockTimeout by remember { mutableStateOf(PrivacyLockTimeout.OneMinute) }
    var selectedAiMode by remember { mutableStateOf(AiGrowthMode.QuickGuidance) }
    var concernText by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf<AiGrowthModeResponse?>(null) }
    var aiResponseRunning by remember { mutableStateOf(false) }
    var aiPromptSubmittedBlank by remember { mutableStateOf(false) }
    var mood by remember { mutableStateOf(DailyMetricLevel.Steady) }
    var energy by remember { mutableStateOf(DailyMetricLevel.Steady) }
    var stress by remember { mutableStateOf(DailyMetricLevel.Steady) }
    var sleep by remember { mutableStateOf(DailyMetricLevel.Steady) }
    var checkInCaptured by remember { mutableStateOf(false) }
    var dailyRecordSaveResult by remember { mutableStateOf<DailyRecordSaveResult?>(null) }
    var dailyRecordCount by remember { mutableStateOf(0) }
    var breathingStepIndex by remember { mutableStateOf(0) }
    var breathingTimerRunning by remember { mutableStateOf(false) }
    var breathingTimerRemainingSeconds by remember {
        mutableStateOf(BreathingExerciseCatalog.boxBreathing().steps.first().durationSeconds)
    }
    var groundingStepIndex by remember { mutableStateOf(0) }
    var selectedWorksheetKind by remember { mutableStateOf(DecisionWorksheetKind.ValuesToAction) }
    var supportText by remember { mutableStateOf("") }
    var supportDecision by remember { mutableStateOf<SafetySupportDecision?>(null) }
    var supportPromptSubmittedBlank by remember { mutableStateOf(false) }
    var supportActionOpened by remember { mutableStateOf<SafetySupportActionType?>(null) }
    var adultConfirmationRunning by remember { mutableStateOf(false) }
    var dailyRecordSaving by remember { mutableStateOf(false) }

    LaunchedEffect(breathingStepIndex) {
        breathingTimerRunning = false
        breathingTimerRemainingSeconds = BreathingExerciseCatalog.boxBreathing()
            .steps[breathingStepIndex]
            .durationSeconds
    }
    LaunchedEffect(breathingTimerRunning, breathingTimerRemainingSeconds) {
        if (breathingTimerRunning && breathingTimerRemainingSeconds > 0) {
            delay(1_000L)
            breathingTimerRemainingSeconds -= 1
        } else if (breathingTimerRunning && breathingTimerRemainingSeconds == 0) {
            breathingTimerRunning = false
            val stepCount = BreathingExerciseCatalog.boxBreathing().steps.size
            breathingStepIndex = (breathingStepIndex + 1) % stepCount
        }
    }

    BettamindTheme(
        themeMode = themeMode,
        useAccessibleTypography = useAccessibleTypography,
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    primaryDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedDestination == destination,
                            onClick = { selectedDestination = destination },
                            icon = {
                                DestinationIndicator(selectedDestination == destination)
                            },
                            label = {
                                Text(stringResource(destination.title))
                            },
                        )
                    }
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                AppHeader()
                DestinationPanel(
                    destination = selectedDestination,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                    useAccessibleTypography = useAccessibleTypography,
                    onAccessibleTypographyChange = { useAccessibleTypography = it },
                    useReducedMotion = useReducedMotion,
                    onReducedMotionChange = { useReducedMotion = it },
                    useLowLiteracyCopy = useLowLiteracyCopy,
                    onLowLiteracyCopyChange = { useLowLiteracyCopy = it },
                    privacyLockTimeout = privacyLockTimeout,
                    onPrivacyLockTimeoutChange = { privacyLockTimeout = it },
                    growthState = growthState,
                    adultConfirmationRunning = adultConfirmationRunning,
                    onConfirmAdult = {
                        if (!adultConfirmationRunning) {
                            adultConfirmationRunning = true
                            coroutineScope.launch {
                                try {
                                    val encryptedStorageAvailable = withContext(Dispatchers.Default) {
                                        services.dailyRecords.available()
                                    }
                                    growthState = DeterministicGrowthEngine.adultConfirmed(
                                        encryptedStorageAvailable = encryptedStorageAvailable,
                                    )
                                    if (encryptedStorageAvailable) {
                                        dailyRecordCount = withContext(Dispatchers.Default) {
                                            services.dailyRecords.recordCount()
                                        }
                                    }
                                } finally {
                                    adultConfirmationRunning = false
                                }
                            }
                        }
                    },
                    onBlockMinorOrUnknown = {
                        growthState = DeterministicGrowthEngine.blockedMinorOrUnknown()
                    },
                    onAdvanceGrowth = {
                        growthState = DeterministicGrowthEngine.advance(growthState)
                    },
                    onResetGrowth = {
                        growthState = DeterministicGrowthEngine.resetFrom(growthState)
                    },
                    selectedAiMode = selectedAiMode,
                    onSelectedAiModeChange = { selectedAiMode = it },
                    concernText = concernText,
                    onConcernTextChange = {
                        concernText = it
                        aiPromptSubmittedBlank = false
                        aiResponse = null
                    },
                    aiResponse = aiResponse,
                    aiResponseRunning = aiResponseRunning,
                    aiPromptSubmittedBlank = aiPromptSubmittedBlank,
                    onRunAiGrowthMode = {
                        val input = concernText.trim()
                        if (input.isBlank()) {
                            aiPromptSubmittedBlank = true
                        } else if (!aiResponseRunning) {
                            aiResponseRunning = true
                            aiResponse = null
                            coroutineScope.launch {
                                try {
                                    aiResponse = aiGrowthEngine.respond(
                                        AiGrowthModeRequest(
                                            mode = selectedAiMode,
                                            userInput = input,
                                        ),
                                    )
                                } finally {
                                    aiResponseRunning = false
                                }
                            }
                        }
                    },
                    mood = mood,
                    onMoodChange = {
                        mood = it
                        checkInCaptured = false
                        dailyRecordSaveResult = null
                    },
                    energy = energy,
                    onEnergyChange = {
                        energy = it
                        checkInCaptured = false
                        dailyRecordSaveResult = null
                    },
                    stress = stress,
                    onStressChange = {
                        stress = it
                        checkInCaptured = false
                        dailyRecordSaveResult = null
                    },
                    sleep = sleep,
                    onSleepChange = {
                        sleep = it
                        checkInCaptured = false
                        dailyRecordSaveResult = null
                    },
                    checkInCaptured = checkInCaptured,
                    dailyToolsUnlocked = growthState.personalFeaturesAvailable,
                    dailyRecordSaving = dailyRecordSaving,
                    onCaptureCheckIn = {
                        if (!dailyRecordSaving) {
                            dailyRecordSaving = true
                            coroutineScope.launch {
                                try {
                                    val result = withContext(Dispatchers.Default) {
                                        services.dailyRecords.saveCheckIn(
                                            mood = mood,
                                            energy = energy,
                                            stress = stress,
                                            sleep = sleep,
                                        )
                                    }
                                    dailyRecordSaveResult = result
                                    checkInCaptured = result is DailyRecordSaveResult.Saved
                                    dailyRecordCount = withContext(Dispatchers.Default) {
                                        services.dailyRecords.recordCount()
                                    }
                                } finally {
                                    dailyRecordSaving = false
                                }
                            }
                        }
                    },
                    dailyRecordSaveResult = dailyRecordSaveResult,
                    dailyRecordCount = dailyRecordCount,
                    breathingStepIndex = breathingStepIndex,
                    breathingTimerRunning = breathingTimerRunning,
                    breathingTimerRemainingSeconds = breathingTimerRemainingSeconds,
                    onToggleBreathingTimer = {
                        breathingTimerRunning = !breathingTimerRunning
                    },
                    onNextBreathingStep = {
                        val stepCount = BreathingExerciseCatalog.boxBreathing().steps.size
                        breathingStepIndex = (breathingStepIndex + 1) % stepCount
                    },
                    groundingStepIndex = groundingStepIndex,
                    onNextGroundingStep = {
                        val stepCount = GroundingExerciseCatalog.fiveFourThreeTwoOne().size
                        groundingStepIndex = (groundingStepIndex + 1) % stepCount
                    },
                    selectedWorksheetKind = selectedWorksheetKind,
                    onSelectedWorksheetKindChange = { selectedWorksheetKind = it },
                    supportText = supportText,
                    onSupportTextChange = {
                        supportText = it
                        supportPromptSubmittedBlank = false
                        supportDecision = null
                        supportActionOpened = null
                    },
                    supportDecision = supportDecision,
                    supportPromptSubmittedBlank = supportPromptSubmittedBlank,
                    onAssessSupport = {
                        val input = supportText.trim()
                        if (input.isBlank()) {
                            supportPromptSubmittedBlank = true
                        } else {
                            supportDecision = supportEngine.assess(input)
                            supportActionOpened = null
                        }
                    },
                    supportActionOpened = supportActionOpened,
                    onOpenSupportAction = { actionType ->
                        supportActionOpened = actionType
                        when (actionType) {
                            SafetySupportActionType.DailyCheckIn,
                            SafetySupportActionType.GroundingExercise,
                            SafetySupportActionType.BreathingTimer,
                            -> {
                                selectedDestination = BettamindDestination.Today
                            }

                            SafetySupportActionType.DelayAction -> {
                                selectedWorksheetKind = DecisionWorksheetKind.ProblemSolving
                                selectedDestination = BettamindDestination.Today
                            }

                            SafetySupportActionType.ConflictReflection,
                            SafetySupportActionType.NonviolentMessage,
                            -> {
                                selectedWorksheetKind = DecisionWorksheetKind.DifficultConversation
                                selectedDestination = BettamindDestination.Today
                            }

                            SafetySupportActionType.RepairPlanning -> {
                                selectedWorksheetKind = DecisionWorksheetKind.RepairPreparation
                                selectedDestination = BettamindDestination.Today
                            }

                            SafetySupportActionType.ValuesToAction -> {
                                selectedWorksheetKind = DecisionWorksheetKind.ValuesToAction
                                selectedDestination = BettamindDestination.Today
                            }

                            SafetySupportActionType.ContactTrustedPerson,
                            SafetySupportActionType.UseLocalEmergencyHelp,
                            SafetySupportActionType.SafePrevention,
                            SafetySupportActionType.LeaveSituation,
                            SafetySupportActionType.NoActionNeeded,
                            -> Unit
                        }
                    },
                    modelPackStatus = modelPackStatus,
                    onInstallModelPack = {
                        services.modelPacks.requestUserInstall()
                    },
                    onRemoveModelPack = {
                        services.modelPacks.removeInstalledModel()
                    },
                    services = services,
                )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.bettamind_mark),
            contentDescription = stringResource(Res.string.app_header_logo_description),
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.app_status_release_candidate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DestinationIndicator(selected: Boolean) {
    val fill = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

@Composable
private fun DestinationPanel(
    destination: BettamindDestination,
    themeMode: BettamindThemeMode,
    onThemeModeChange: (BettamindThemeMode) -> Unit,
    useAccessibleTypography: Boolean,
    onAccessibleTypographyChange: (Boolean) -> Unit,
    useReducedMotion: Boolean,
    onReducedMotionChange: (Boolean) -> Unit,
    useLowLiteracyCopy: Boolean,
    onLowLiteracyCopyChange: (Boolean) -> Unit,
    privacyLockTimeout: PrivacyLockTimeout,
    onPrivacyLockTimeoutChange: (PrivacyLockTimeout) -> Unit,
    growthState: GrowthSessionState,
    adultConfirmationRunning: Boolean,
    onConfirmAdult: () -> Unit,
    onBlockMinorOrUnknown: () -> Unit,
    onAdvanceGrowth: () -> Unit,
    onResetGrowth: () -> Unit,
    selectedAiMode: AiGrowthMode,
    onSelectedAiModeChange: (AiGrowthMode) -> Unit,
    concernText: String,
    onConcernTextChange: (String) -> Unit,
    aiResponse: AiGrowthModeResponse?,
    aiResponseRunning: Boolean,
    aiPromptSubmittedBlank: Boolean,
    onRunAiGrowthMode: () -> Unit,
    mood: DailyMetricLevel,
    onMoodChange: (DailyMetricLevel) -> Unit,
    energy: DailyMetricLevel,
    onEnergyChange: (DailyMetricLevel) -> Unit,
    stress: DailyMetricLevel,
    onStressChange: (DailyMetricLevel) -> Unit,
    sleep: DailyMetricLevel,
    onSleepChange: (DailyMetricLevel) -> Unit,
    checkInCaptured: Boolean,
    dailyToolsUnlocked: Boolean,
    dailyRecordSaving: Boolean,
    onCaptureCheckIn: () -> Unit,
    dailyRecordSaveResult: DailyRecordSaveResult?,
    dailyRecordCount: Int,
    breathingStepIndex: Int,
    breathingTimerRunning: Boolean,
    breathingTimerRemainingSeconds: Int,
    onToggleBreathingTimer: () -> Unit,
    onNextBreathingStep: () -> Unit,
    groundingStepIndex: Int,
    onNextGroundingStep: () -> Unit,
    selectedWorksheetKind: DecisionWorksheetKind,
    onSelectedWorksheetKindChange: (DecisionWorksheetKind) -> Unit,
    supportText: String,
    onSupportTextChange: (String) -> Unit,
    supportDecision: SafetySupportDecision?,
    supportPromptSubmittedBlank: Boolean,
    onAssessSupport: () -> Unit,
    supportActionOpened: SafetySupportActionType?,
    onOpenSupportAction: (SafetySupportActionType) -> Unit,
    modelPackStatus: ModelPackPlatformStatus,
    onInstallModelPack: () -> Unit,
    onRemoveModelPack: () -> Unit,
    services: BettamindAppServices,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(destination.title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(destination.description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (destination == BettamindDestination.Settings) {
            SettingsPanel(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                useAccessibleTypography = useAccessibleTypography,
                onAccessibleTypographyChange = onAccessibleTypographyChange,
                useReducedMotion = useReducedMotion,
                onReducedMotionChange = onReducedMotionChange,
                useLowLiteracyCopy = useLowLiteracyCopy,
                onLowLiteracyCopyChange = onLowLiteracyCopyChange,
                privacyLockTimeout = privacyLockTimeout,
                onPrivacyLockTimeoutChange = onPrivacyLockTimeoutChange,
                modelPackStatus = modelPackStatus,
                onInstallModelPack = onInstallModelPack,
                onRemoveModelPack = onRemoveModelPack,
                services = services,
            )
        } else {
            GrowthDestinationContent(
                destination = destination,
                growthState = growthState,
                adultConfirmationRunning = adultConfirmationRunning,
                onConfirmAdult = onConfirmAdult,
                onBlockMinorOrUnknown = onBlockMinorOrUnknown,
                onAdvanceGrowth = onAdvanceGrowth,
                onResetGrowth = onResetGrowth,
                selectedAiMode = selectedAiMode,
                onSelectedAiModeChange = onSelectedAiModeChange,
                concernText = concernText,
                onConcernTextChange = onConcernTextChange,
                aiResponse = aiResponse,
                aiResponseRunning = aiResponseRunning,
                aiPromptSubmittedBlank = aiPromptSubmittedBlank,
                onRunAiGrowthMode = onRunAiGrowthMode,
                mood = mood,
                onMoodChange = onMoodChange,
                energy = energy,
                onEnergyChange = onEnergyChange,
                stress = stress,
                onStressChange = onStressChange,
                sleep = sleep,
                onSleepChange = onSleepChange,
                checkInCaptured = checkInCaptured,
                dailyToolsUnlocked = dailyToolsUnlocked,
                dailyRecordSaving = dailyRecordSaving,
                onCaptureCheckIn = onCaptureCheckIn,
                dailyRecordSaveResult = dailyRecordSaveResult,
                dailyRecordCount = dailyRecordCount,
                breathingStepIndex = breathingStepIndex,
                breathingTimerRunning = breathingTimerRunning,
                breathingTimerRemainingSeconds = breathingTimerRemainingSeconds,
                onToggleBreathingTimer = onToggleBreathingTimer,
                onNextBreathingStep = onNextBreathingStep,
                groundingStepIndex = groundingStepIndex,
                onNextGroundingStep = onNextGroundingStep,
                selectedWorksheetKind = selectedWorksheetKind,
                onSelectedWorksheetKindChange = onSelectedWorksheetKindChange,
                supportText = supportText,
                onSupportTextChange = onSupportTextChange,
                supportDecision = supportDecision,
                supportPromptSubmittedBlank = supportPromptSubmittedBlank,
                onAssessSupport = onAssessSupport,
                supportActionOpened = supportActionOpened,
                onOpenSupportAction = onOpenSupportAction,
                modelPackStatus = modelPackStatus,
                onInstallModelPack = onInstallModelPack,
                onRemoveModelPack = onRemoveModelPack,
                services = services,
            )
        }
    }
}

@Composable
private fun GrowthDestinationContent(
    destination: BettamindDestination,
    growthState: GrowthSessionState,
    adultConfirmationRunning: Boolean,
    onConfirmAdult: () -> Unit,
    onBlockMinorOrUnknown: () -> Unit,
    onAdvanceGrowth: () -> Unit,
    onResetGrowth: () -> Unit,
    selectedAiMode: AiGrowthMode,
    onSelectedAiModeChange: (AiGrowthMode) -> Unit,
    concernText: String,
    onConcernTextChange: (String) -> Unit,
    aiResponse: AiGrowthModeResponse?,
    aiResponseRunning: Boolean,
    aiPromptSubmittedBlank: Boolean,
    onRunAiGrowthMode: () -> Unit,
    mood: DailyMetricLevel,
    onMoodChange: (DailyMetricLevel) -> Unit,
    energy: DailyMetricLevel,
    onEnergyChange: (DailyMetricLevel) -> Unit,
    stress: DailyMetricLevel,
    onStressChange: (DailyMetricLevel) -> Unit,
    sleep: DailyMetricLevel,
    onSleepChange: (DailyMetricLevel) -> Unit,
    checkInCaptured: Boolean,
    dailyToolsUnlocked: Boolean,
    dailyRecordSaving: Boolean,
    onCaptureCheckIn: () -> Unit,
    dailyRecordSaveResult: DailyRecordSaveResult?,
    dailyRecordCount: Int,
    breathingStepIndex: Int,
    breathingTimerRunning: Boolean,
    breathingTimerRemainingSeconds: Int,
    onToggleBreathingTimer: () -> Unit,
    onNextBreathingStep: () -> Unit,
    groundingStepIndex: Int,
    onNextGroundingStep: () -> Unit,
    selectedWorksheetKind: DecisionWorksheetKind,
    onSelectedWorksheetKindChange: (DecisionWorksheetKind) -> Unit,
    supportText: String,
    onSupportTextChange: (String) -> Unit,
    supportDecision: SafetySupportDecision?,
    supportPromptSubmittedBlank: Boolean,
    onAssessSupport: () -> Unit,
    supportActionOpened: SafetySupportActionType?,
    onOpenSupportAction: (SafetySupportActionType) -> Unit,
    modelPackStatus: ModelPackPlatformStatus,
    onInstallModelPack: () -> Unit,
    onRemoveModelPack: () -> Unit,
    services: BettamindAppServices,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AdultGatePanel(
            growthState = growthState,
            adultConfirmationRunning = adultConfirmationRunning,
            onConfirmAdult = onConfirmAdult,
            onBlockMinorOrUnknown = onBlockMinorOrUnknown,
        )
        StorageStatusPanel(growthState.narrativeStorageStatus)
        when (destination) {
            BettamindDestination.Today -> {
                DailyToolsPanel(
                    mood = mood,
                    onMoodChange = onMoodChange,
                    energy = energy,
                    onEnergyChange = onEnergyChange,
                    stress = stress,
                    onStressChange = onStressChange,
                    sleep = sleep,
                    onSleepChange = onSleepChange,
                    checkInCaptured = checkInCaptured,
                    dailyToolsUnlocked = growthState.personalFeaturesAvailable,
                    dailyRecordSaving = dailyRecordSaving,
                    onCaptureCheckIn = onCaptureCheckIn,
                    dailyRecordSaveResult = dailyRecordSaveResult,
                    dailyRecordCount = dailyRecordCount,
                    breathingStepIndex = breathingStepIndex,
                    breathingTimerRunning = breathingTimerRunning,
                    breathingTimerRemainingSeconds = breathingTimerRemainingSeconds,
                    onToggleBreathingTimer = onToggleBreathingTimer,
                    onNextBreathingStep = onNextBreathingStep,
                    groundingStepIndex = groundingStepIndex,
                    onNextGroundingStep = onNextGroundingStep,
                    selectedWorksheetKind = selectedWorksheetKind,
                    onSelectedWorksheetKindChange = onSelectedWorksheetKindChange,
                )
                TodayGrowthPanel(
                    growthState = growthState,
                    onAdvanceGrowth = onAdvanceGrowth,
                    onResetGrowth = onResetGrowth,
                )
            }

            BettamindDestination.Reflect -> StepMapPanel(growthState)
            BettamindDestination.Grow -> {
                AiGrowthModesPanel(
                    selectedMode = selectedAiMode,
                    onSelectedModeChange = onSelectedAiModeChange,
                    concernText = concernText,
                    onConcernTextChange = onConcernTextChange,
                    response = aiResponse,
                    responseRunning = aiResponseRunning,
                    promptSubmittedBlank = aiPromptSubmittedBlank,
                    onRun = onRunAiGrowthMode,
                    modelPackStatus = modelPackStatus,
                    onInstallModelPack = onInstallModelPack,
                    onRemoveModelPack = onRemoveModelPack,
                )
                GrowthSummaryPanel(growthState)
            }
            BettamindDestination.Support -> SupportPanel(
                supportText = supportText,
                onSupportTextChange = onSupportTextChange,
                supportDecision = supportDecision,
                promptSubmittedBlank = supportPromptSubmittedBlank,
                onAssessSupport = onAssessSupport,
                openedAction = supportActionOpened,
                onOpenSupportAction = onOpenSupportAction,
            )
            BettamindDestination.Settings -> Unit
        }
    }
}

@Composable
private fun DailyToolsPanel(
    mood: DailyMetricLevel,
    onMoodChange: (DailyMetricLevel) -> Unit,
    energy: DailyMetricLevel,
    onEnergyChange: (DailyMetricLevel) -> Unit,
    stress: DailyMetricLevel,
    onStressChange: (DailyMetricLevel) -> Unit,
    sleep: DailyMetricLevel,
    onSleepChange: (DailyMetricLevel) -> Unit,
    checkInCaptured: Boolean,
    dailyToolsUnlocked: Boolean,
    dailyRecordSaving: Boolean,
    onCaptureCheckIn: () -> Unit,
    dailyRecordSaveResult: DailyRecordSaveResult?,
    dailyRecordCount: Int,
    breathingStepIndex: Int,
    breathingTimerRunning: Boolean,
    breathingTimerRemainingSeconds: Int,
    onToggleBreathingTimer: () -> Unit,
    onNextBreathingStep: () -> Unit,
    groundingStepIndex: Int,
    onNextGroundingStep: () -> Unit,
    selectedWorksheetKind: DecisionWorksheetKind,
    onSelectedWorksheetKindChange: (DecisionWorksheetKind) -> Unit,
) {
    val breathing = BreathingExerciseCatalog.boxBreathing()
    val breathingStep = breathing.steps[breathingStepIndex.coerceIn(0, breathing.steps.lastIndex)]
    val grounding = GroundingExerciseCatalog.fiveFourThreeTwoOne()
    val groundingStep = grounding[groundingStepIndex.coerceIn(0, grounding.lastIndex)]
    val worksheet = DecisionWorksheetCatalog.template(selectedWorksheetKind)
    StatusBlock(
        title = Res.string.daily_tools_title,
        body = Res.string.daily_tools_description,
    ) {
        Text(
            text = stringResource(Res.string.daily_checkin_session_title),
            style = MaterialTheme.typography.titleMedium,
        )
        MetricSelector(Res.string.daily_checkin_mood, mood, onMoodChange)
        MetricSelector(Res.string.daily_checkin_energy, energy, onEnergyChange)
        MetricSelector(Res.string.daily_checkin_stress, stress, onStressChange)
        MetricSelector(Res.string.daily_checkin_sleep, sleep, onSleepChange)
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = dailyToolsUnlocked && !dailyRecordSaving,
            onClick = onCaptureCheckIn,
        ) {
            Text(
                stringResource(
                    if (dailyRecordSaving) {
                        Res.string.daily_checkin_saving_session
                    } else {
                        Res.string.daily_checkin_capture_session
                    },
                ),
            )
        }
        if (dailyRecordSaving) {
            Text(
                text = stringResource(Res.string.daily_checkin_saving_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                dailyRecordSaveResult.messageResource(
                    checkInCaptured = checkInCaptured,
                    dailyToolsUnlocked = dailyToolsUnlocked,
                ),
                dailyRecordCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusLine(
            title = Res.string.daily_checkin_history_title,
            body = Res.string.daily_checkin_history_description,
        )
        Text(
            text = stringResource(Res.string.daily_checkin_history_count, dailyRecordCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.daily_breathing_box_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.daily_breathing_pattern, breathing.cycleDurationSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                Res.string.daily_breathing_step_label,
                breathingStepIndex + 1,
                breathing.steps.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(breathingStep.instruction()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(
                Res.string.daily_breathing_timer_remaining,
                breathingTimerRemainingSeconds,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = onToggleBreathingTimer,
        ) {
            Text(
                stringResource(
                    if (breathingTimerRunning) {
                        Res.string.daily_breathing_pause_timer
                    } else {
                        Res.string.daily_breathing_start_timer
                    },
                ),
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = onNextBreathingStep,
        ) {
            Text(stringResource(Res.string.daily_breathing_next_step))
        }
        Text(
            text = stringResource(Res.string.daily_grounding_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                Res.string.daily_grounding_step_label,
                groundingStepIndex + 1,
                grounding.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(groundingStep.prompt()),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = onNextGroundingStep,
        ) {
            Text(stringResource(Res.string.daily_grounding_next_step))
        }
        Text(
            text = stringResource(Res.string.daily_worksheet_select_title),
            style = MaterialTheme.typography.titleMedium,
        )
        WorksheetButtonRow(
            leftKind = DecisionWorksheetKind.ValuesToAction,
            leftSelected = selectedWorksheetKind == DecisionWorksheetKind.ValuesToAction,
            onLeftClick = { onSelectedWorksheetKindChange(DecisionWorksheetKind.ValuesToAction) },
            rightKind = DecisionWorksheetKind.ProblemSolving,
            rightSelected = selectedWorksheetKind == DecisionWorksheetKind.ProblemSolving,
            onRightClick = { onSelectedWorksheetKindChange(DecisionWorksheetKind.ProblemSolving) },
        )
        WorksheetButtonRow(
            leftKind = DecisionWorksheetKind.RepairPreparation,
            leftSelected = selectedWorksheetKind == DecisionWorksheetKind.RepairPreparation,
            onLeftClick = { onSelectedWorksheetKindChange(DecisionWorksheetKind.RepairPreparation) },
            rightKind = DecisionWorksheetKind.DifficultConversation,
            rightSelected = selectedWorksheetKind == DecisionWorksheetKind.DifficultConversation,
            onRightClick = { onSelectedWorksheetKindChange(DecisionWorksheetKind.DifficultConversation) },
        )
        worksheet.promptKeys.forEachIndexed { index, key ->
            Text(
                text = stringResource(
                    Res.string.daily_worksheet_prompt_label,
                    index + 1,
                    stringResource(worksheetPrompt(key)),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        StatusLine(
            title = Res.string.daily_reminder_title,
            body = Res.string.daily_reminder_description,
        )
        Text(
            text = stringResource(Res.string.daily_reminder_neutral_preview),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusLine(
            title = Res.string.daily_calendar_title,
            body = Res.string.daily_calendar_description,
        )
        StatusLine(
            title = Res.string.daily_worksheet_title,
            body = Res.string.daily_worksheet_description,
        )
        Text(
            text = stringResource(Res.string.daily_record_storage_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiGrowthModesPanel(
    selectedMode: AiGrowthMode,
    onSelectedModeChange: (AiGrowthMode) -> Unit,
    concernText: String,
    onConcernTextChange: (String) -> Unit,
    response: AiGrowthModeResponse?,
    responseRunning: Boolean,
    promptSubmittedBlank: Boolean,
    onRun: () -> Unit,
    modelPackStatus: ModelPackPlatformStatus,
    onInstallModelPack: () -> Unit,
    onRemoveModelPack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    fun submitPrompt() {
        onRun()
        focusManager.clearFocus()
    }

    StatusBlock(
        title = Res.string.ai_growth_modes_title,
        body = Res.string.ai_growth_modes_description,
    ) {
        StatusLine(
            title = Res.string.platform_model_pack_title,
            body = modelPackStatus.resource(),
        )
        ModelPackControls(
            status = modelPackStatus,
            onInstallModelPack = onInstallModelPack,
            onRemoveModelPack = onRemoveModelPack,
        )
        AiModeButtonRow(
            leftMode = AiGrowthMode.QuickGuidance,
            leftSelected = selectedMode == AiGrowthMode.QuickGuidance,
            onLeftClick = { onSelectedModeChange(AiGrowthMode.QuickGuidance) },
            rightMode = AiGrowthMode.GuidedReflection,
            rightSelected = selectedMode == AiGrowthMode.GuidedReflection,
            onRightClick = { onSelectedModeChange(AiGrowthMode.GuidedReflection) },
        )
        AiModeButtonRow(
            leftMode = AiGrowthMode.DeepExploration,
            leftSelected = selectedMode == AiGrowthMode.DeepExploration,
            onLeftClick = { onSelectedModeChange(AiGrowthMode.DeepExploration) },
            rightMode = AiGrowthMode.ActionOnly,
            rightSelected = selectedMode == AiGrowthMode.ActionOnly,
            onRightClick = { onSelectedModeChange(AiGrowthMode.ActionOnly) },
        )
        Text(
            text = stringResource(selectedMode.description()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
            value = concernText,
            onValueChange = onConcernTextChange,
            label = { Text(stringResource(Res.string.ai_growth_prompt_label)) },
            placeholder = { Text(stringResource(Res.string.ai_growth_prompt_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submitPrompt() }),
            minLines = 3,
        )
        if (promptSubmittedBlank) {
            Text(
                text = stringResource(Res.string.ai_growth_blank_prompt_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !responseRunning,
            onClick = { submitPrompt() },
        ) {
            Text(
                stringResource(
                    if (responseRunning) {
                        Res.string.ai_growth_running_button
                    } else {
                        Res.string.ai_growth_run_button
                    },
                ),
            )
        }
        if (responseRunning) {
            Text(
                text = stringResource(Res.string.ai_growth_running_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        response?.let { AiGrowthResponsePanel(it) }
        Text(
            text = stringResource(Res.string.ai_growth_no_model_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ai_growth_safety_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ai_growth_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiGrowthResponsePanel(response: AiGrowthModeResponse) {
    StatusBlock(
        title = Res.string.ai_growth_response_title,
        body = if (response.modelText == null) {
            fallbackResource(response.fallbackLocalizationKey)
        } else {
            Res.string.ai_growth_model_text_available
        },
    ) {
        response.modelText?.let { modelText ->
            Text(
                text = modelText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = stringResource(
                Res.string.ai_growth_response_source,
                stringResource(response.source.label()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (response.fallbackReason != AiGrowthFallbackReason.None) {
            Text(
                text = stringResource(
                    Res.string.ai_growth_response_fallback_reason,
                    stringResource(response.fallbackReason.label()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                if (response.metadata.safetyBoundaryApplied) {
                    Res.string.ai_growth_safety_boundary_applied
                } else {
                    Res.string.ai_growth_safety_boundary_clear
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ai_growth_actions_title),
            style = MaterialTheme.typography.titleSmall,
        )
        response.actionKeys.forEach { key ->
            Text(
                text = stringResource(actionResource(key)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(Res.string.ai_growth_memory_export_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AdultGatePanel(
    growthState: GrowthSessionState,
    adultConfirmationRunning: Boolean,
    onConfirmAdult: () -> Unit,
    onBlockMinorOrUnknown: () -> Unit,
) {
    StatusBlock(
        title = Res.string.growth_age_gate_title,
        body = when (growthState.adultGateState) {
            AdultGateState.NeedsAdultConfirmation -> Res.string.growth_age_gate_description
            AdultGateState.AdultConfirmed -> Res.string.growth_age_adult_ready
            AdultGateState.BlockedMinorOrUnknown -> Res.string.growth_age_blocked
        },
    ) {
        if (growthState.adultGateState == AdultGateState.NeedsAdultConfirmation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !adultConfirmationRunning,
                    onClick = onConfirmAdult,
                ) {
                    Text(
                        stringResource(
                            if (adultConfirmationRunning) {
                                Res.string.growth_age_checking_storage
                            } else {
                                Res.string.growth_age_confirm_adult
                            },
                        ),
                    )
                }
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !adultConfirmationRunning,
                    onClick = onBlockMinorOrUnknown,
                ) {
                    Text(stringResource(Res.string.growth_age_not_adult))
                }
            }
            if (adultConfirmationRunning) {
                Text(
                    text = stringResource(Res.string.growth_age_checking_storage_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StorageStatusPanel(status: NarrativeStorageStatus) {
    val body = when (status) {
        NarrativeStorageStatus.LockedUntilAdult -> Res.string.growth_storage_locked
        NarrativeStorageStatus.EncryptedStorageUnavailable -> Res.string.growth_storage_unavailable
        NarrativeStorageStatus.EncryptedStorageReady -> Res.string.growth_storage_ready
    }
    StatusBlock(
        title = Res.string.growth_storage_title,
        body = body,
    )
}

@Composable
private fun TodayGrowthPanel(
    growthState: GrowthSessionState,
    onAdvanceGrowth: () -> Unit,
    onResetGrowth: () -> Unit,
) {
    val stepCopy = growthState.currentStep.copy()
    StatusBlock(
        title = Res.string.growth_today_title,
        body = if (growthState.personalFeaturesAvailable) {
            stepCopy.description
        } else {
            Res.string.growth_step_locked
        },
    ) {
        Text(
            text = stringResource(stepCopy.title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                enabled = growthState.personalFeaturesAvailable && !growthState.isComplete,
                onClick = onAdvanceGrowth,
            ) {
                Text(
                    stringResource(
                        if (growthState.isComplete) {
                            Res.string.growth_step_finished
                        } else {
                            Res.string.growth_step_complete_button
                        },
                    ),
                )
            }
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = growthState.personalFeaturesAvailable && growthState.completedSteps.isNotEmpty(),
                onClick = onResetGrowth,
            ) {
                Text(stringResource(Res.string.growth_reset_session))
            }
        }
    }
}

@Composable
private fun StepMapPanel(growthState: GrowthSessionState) {
    StatusBlock(
        title = Res.string.growth_reflect_title,
        body = Res.string.growth_reflect_description,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GrowthStep.entries.forEach { step ->
                StepRow(
                    step = step,
                    active = growthState.currentStep == step,
                    completed = step in growthState.completedSteps,
                )
            }
        }
    }
}

@Composable
private fun GrowthSummaryPanel(growthState: GrowthSessionState) {
    StatusBlock(
        title = Res.string.growth_summary_title,
        body = when {
            !growthState.personalFeaturesAvailable -> Res.string.growth_summary_locked
            growthState.isComplete -> Res.string.growth_summary_complete
            else -> Res.string.growth_summary_in_progress
        },
    ) {
        Text(
            text = stringResource(
                Res.string.growth_summary_count,
                growthState.completedSteps.size,
                GrowthStep.entries.size,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SupportPanel(
    supportText: String,
    onSupportTextChange: (String) -> Unit,
    supportDecision: SafetySupportDecision?,
    promptSubmittedBlank: Boolean,
    onAssessSupport: () -> Unit,
    openedAction: SafetySupportActionType?,
    onOpenSupportAction: (SafetySupportActionType) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    fun submitSupportPrompt() {
        onAssessSupport()
        focusManager.clearFocus()
    }

    StatusBlock(
        title = Res.string.support_bridge_title,
        body = Res.string.support_bridge_description,
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
            value = supportText,
            onValueChange = onSupportTextChange,
            label = { Text(stringResource(Res.string.support_prompt_label)) },
            placeholder = { Text(stringResource(Res.string.support_prompt_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submitSupportPrompt() }),
            minLines = 3,
        )
        if (promptSubmittedBlank) {
            Text(
                text = stringResource(Res.string.ai_growth_blank_prompt_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = { submitSupportPrompt() },
        ) {
            Text(stringResource(Res.string.support_assess_button))
        }
        supportDecision?.let {
            SupportDecisionPanel(
                decision = it,
                openedAction = openedAction,
                onOpenSupportAction = onOpenSupportAction,
            )
        }
        StatusLine(
            title = Res.string.support_bridge_no_auto_contact_title,
            body = Res.string.support_bridge_no_auto_contact_body,
        )
        StatusLine(
            title = Res.string.support_bridge_preview_title,
            body = Res.string.support_bridge_preview_body,
        )
        StatusLine(
            title = Res.string.support_bridge_daily_tools_title,
            body = Res.string.support_bridge_daily_tools_body,
        )
        StatusLine(
            title = Res.string.support_bridge_resources_title,
            body = Res.string.support_bridge_resources_body,
        )
        Text(
            text = stringResource(Res.string.support_bridge_review_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SupportDecisionPanel(
    decision: SafetySupportDecision,
    openedAction: SafetySupportActionType?,
    onOpenSupportAction: (SafetySupportActionType) -> Unit,
) {
    StatusBlock(
        title = Res.string.support_result_title,
        body = Res.string.support_result_description,
    ) {
        Text(
            text = stringResource(
                Res.string.support_result_risk_level,
                stringResource(decision.riskLevel.label()),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(Res.string.support_result_actions_title),
            style = MaterialTheme.typography.titleSmall,
        )
        decision.actions.forEach { action ->
            SupportActionRow(
                actionType = action.type,
                opened = openedAction == action.type,
                onOpen = onOpenSupportAction,
            )
        }
        Text(
            text = stringResource(Res.string.support_result_resources_title),
            style = MaterialTheme.typography.titleSmall,
        )
        decision.localResources.forEach { resource ->
            StatusLine(
                title = resource.scope.title(),
                body = resource.scope.body(),
            )
        }
        Text(
            text = stringResource(
                if (decision.noAutomaticContact) {
                    Res.string.support_no_auto_contact_short
                } else {
                    Res.string.support_auto_contact_forbidden
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.support_share_preview_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SupportActionRow(
    actionType: SafetySupportActionType,
    opened: Boolean,
    onOpen: (SafetySupportActionType) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatusLine(
            title = actionType.title(),
            body = actionType.body(),
        )
        if (actionType.opensLocalTool()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                onClick = { onOpen(actionType) },
            ) {
                Text(
                    stringResource(
                        if (opened) {
                            Res.string.support_opened_action_button
                        } else {
                            Res.string.support_open_action_button
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatusLine(
    title: StringResource,
    body: StringResource,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StepRow(
    step: GrowthStep,
    active: Boolean,
    completed: Boolean,
) {
    val copy = step.copy()
    Surface(
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(copy.title),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (completed) {
                    Text(
                        text = stringResource(Res.string.growth_step_completed_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                text = stringResource(copy.description),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatusBlock(
    title: StringResource,
    body: StringResource,
    content: @Composable () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
            )
            content()
        }
    }
}

private data class GrowthStepCopy(
    val title: StringResource,
    val description: StringResource,
)

private fun GrowthStep.copy(): GrowthStepCopy = when (this) {
    GrowthStep.Awareness -> GrowthStepCopy(
        title = Res.string.growth_step_awareness,
        description = Res.string.growth_step_awareness_description,
    )

    GrowthStep.Choice -> GrowthStepCopy(
        title = Res.string.growth_step_choice,
        description = Res.string.growth_step_choice_description,
    )

    GrowthStep.Action -> GrowthStepCopy(
        title = Res.string.growth_step_action,
        description = Res.string.growth_step_action_description,
    )

    GrowthStep.Consequence -> GrowthStepCopy(
        title = Res.string.growth_step_consequence,
        description = Res.string.growth_step_consequence_description,
    )

    GrowthStep.Reflection -> GrowthStepCopy(
        title = Res.string.growth_step_reflection,
        description = Res.string.growth_step_reflection_description,
    )

    GrowthStep.Repair -> GrowthStepCopy(
        title = Res.string.growth_step_repair,
        description = Res.string.growth_step_repair_description,
    )

    GrowthStep.Growth -> GrowthStepCopy(
        title = Res.string.growth_step_growth,
        description = Res.string.growth_step_growth_description,
    )
}

@Composable
private fun SettingsPanel(
    themeMode: BettamindThemeMode,
    onThemeModeChange: (BettamindThemeMode) -> Unit,
    useAccessibleTypography: Boolean,
    onAccessibleTypographyChange: (Boolean) -> Unit,
    useReducedMotion: Boolean,
    onReducedMotionChange: (Boolean) -> Unit,
    useLowLiteracyCopy: Boolean,
    onLowLiteracyCopyChange: (Boolean) -> Unit,
    privacyLockTimeout: PrivacyLockTimeout,
    onPrivacyLockTimeoutChange: (PrivacyLockTimeout) -> Unit,
    modelPackStatus: ModelPackPlatformStatus,
    onInstallModelPack: () -> Unit,
    onRemoveModelPack: () -> Unit,
    services: BettamindAppServices,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
        )
        ThemeButtonRow(
            leftLabel = Res.string.settings_theme_light,
            leftSelected = themeMode == BettamindThemeMode.Light,
            onLeftClick = { onThemeModeChange(BettamindThemeMode.Light) },
            rightLabel = Res.string.settings_theme_dark,
            rightSelected = themeMode == BettamindThemeMode.Dark,
            onRightClick = { onThemeModeChange(BettamindThemeMode.Dark) },
        )
        ThemeButtonRow(
            leftLabel = Res.string.settings_theme_calm,
            leftSelected = themeMode == BettamindThemeMode.Calm,
            onLeftClick = { onThemeModeChange(BettamindThemeMode.Calm) },
            rightLabel = Res.string.settings_theme_high_contrast,
            rightSelected = themeMode == BettamindThemeMode.HighContrast,
            onRightClick = { onThemeModeChange(BettamindThemeMode.HighContrast) },
        )
        AccessibilitySettingsPanel(
            useAccessibleTypography = useAccessibleTypography,
            onAccessibleTypographyChange = onAccessibleTypographyChange,
            useReducedMotion = useReducedMotion,
            onReducedMotionChange = onReducedMotionChange,
            useLowLiteracyCopy = useLowLiteracyCopy,
            onLowLiteracyCopyChange = onLowLiteracyCopyChange,
        )
        PlatformIntegrationsSettingsPanel(
            services = services,
            modelPackStatus = modelPackStatus,
            onInstallModelPack = onInstallModelPack,
            onRemoveModelPack = onRemoveModelPack,
        )
        OfflineSpeechSettingsPanel()
        Text(
            text = stringResource(Res.string.settings_locale_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrivacyLockSettingsPanel(
            privacyLockTimeout = privacyLockTimeout,
            onPrivacyLockTimeoutChange = onPrivacyLockTimeoutChange,
        )
        RelationalBoundarySettingsPanel()
        HarmSafetySettingsPanel()
        EncryptedExportSyncSettingsPanel()
        ReleaseReadinessSettingsPanel()
    }
}

@Composable
private fun PlatformIntegrationsSettingsPanel(
    services: BettamindAppServices,
    modelPackStatus: ModelPackPlatformStatus,
    onInstallModelPack: () -> Unit,
    onRemoveModelPack: () -> Unit,
) {
    val reminderStatus = services.reminders.status()
    val calendarStatus = services.calendar.status()
    val speechStatus = services.speech.status()
    StatusBlock(
        title = Res.string.platform_integrations_title,
        body = Res.string.platform_integrations_description,
    ) {
        StatusLine(
            title = Res.string.platform_reminder_title,
            body = reminderStatus.resource(),
        )
        StatusLine(
            title = Res.string.platform_calendar_title,
            body = calendarStatus.resource(),
        )
        StatusLine(
            title = Res.string.platform_speech_title,
            body = speechStatus.resource(),
        )
        StatusLine(
            title = Res.string.platform_model_pack_title,
            body = modelPackStatus.resource(),
        )
        StatusLine(
            title = Res.string.platform_model_pack_first_title,
            body = Res.string.platform_model_pack_qwen_first,
        )
        ModelPackControls(
            status = modelPackStatus,
            onInstallModelPack = onInstallModelPack,
            onRemoveModelPack = onRemoveModelPack,
        )
    }
}

@Composable
private fun ModelPackControls(
    status: ModelPackPlatformStatus,
    onInstallModelPack: () -> Unit,
    onRemoveModelPack: () -> Unit,
) {
    StatusLine(
        title = Res.string.model_pack_install_state_title,
        body = status.installState.resource(),
    )
    status.installedModelId?.let { modelId ->
        Text(
            text = stringResource(
                Res.string.model_pack_installed_detail,
                modelId,
                status.installedModelVersion ?: 0,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (status.installState == ModelPackInstallState.Installing && status.installTotalBytes > 0L) {
        Text(
            text = stringResource(
                Res.string.model_pack_install_progress,
                status.installProgressBytes / 1_000_000L,
                status.installTotalBytes / 1_000_000L,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    status.lastFailure?.let { failure ->
        Text(
            text = stringResource(failure.resource()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (status.installState == ModelPackInstallState.Installed) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = onRemoveModelPack,
        ) {
            Text(stringResource(Res.string.model_pack_remove_button))
        }
    } else {
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = status.installerAvailable &&
                status.installState != ModelPackInstallState.Installing &&
                status.installState != ModelPackInstallState.AwaitingUserSelection,
            onClick = onInstallModelPack,
        ) {
            Text(stringResource(Res.string.model_pack_install_button))
        }
    }
    Text(
        text = stringResource(Res.string.model_pack_install_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccessibilitySettingsPanel(
    useAccessibleTypography: Boolean,
    onAccessibleTypographyChange: (Boolean) -> Unit,
    useReducedMotion: Boolean,
    onReducedMotionChange: (Boolean) -> Unit,
    useLowLiteracyCopy: Boolean,
    onLowLiteracyCopyChange: (Boolean) -> Unit,
) {
    StatusBlock(
        title = Res.string.accessibility_title,
        body = Res.string.accessibility_description,
    ) {
        SettingsSwitchRow(
            title = Res.string.settings_font_title,
            description = if (useAccessibleTypography) {
                Res.string.settings_font_accessible
            } else {
                Res.string.settings_font_default
            },
            checked = useAccessibleTypography,
            onCheckedChange = onAccessibleTypographyChange,
        )
        SettingsSwitchRow(
            title = Res.string.accessibility_reduced_motion_title,
            description = Res.string.accessibility_reduced_motion_description,
            checked = useReducedMotion,
            onCheckedChange = onReducedMotionChange,
        )
        SettingsSwitchRow(
            title = Res.string.accessibility_low_literacy_title,
            description = Res.string.accessibility_low_literacy_description,
            checked = useLowLiteracyCopy,
            onCheckedChange = onLowLiteracyCopyChange,
        )
        Text(
            text = stringResource(Res.string.accessibility_review_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: StringResource,
    description: StringResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val titleText = stringResource(title)
    val stateText = stringResource(
        if (checked) {
            Res.string.accessibility_toggle_on
        } else {
            Res.string.accessibility_toggle_off
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            modifier = Modifier.semantics {
                contentDescription = titleText
                stateDescription = stateText
            },
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun OfflineSpeechSettingsPanel() {
    StatusBlock(
        title = Res.string.offline_speech_title,
        body = Res.string.offline_speech_description,
    ) {
        StatusLine(
            title = Res.string.offline_speech_text_fallback_title,
            body = Res.string.offline_speech_text_fallback_body,
        )
        StatusLine(
            title = Res.string.offline_speech_microphone_title,
            body = Res.string.offline_speech_microphone_body,
        )
        StatusLine(
            title = Res.string.offline_speech_audio_retention_title,
            body = Res.string.offline_speech_audio_retention_body,
        )
        StatusLine(
            title = Res.string.offline_speech_packs_title,
            body = Res.string.offline_speech_packs_body,
        )
        Text(
            text = stringResource(Res.string.offline_speech_safety_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyLockSettingsPanel(
    privacyLockTimeout: PrivacyLockTimeout,
    onPrivacyLockTimeoutChange: (PrivacyLockTimeout) -> Unit,
) {
    StatusBlock(
        title = Res.string.privacy_lock_title,
        body = Res.string.privacy_lock_description,
    ) {
        Text(
            text = stringResource(
                Res.string.privacy_lock_current_timeout,
                stringResource(privacyLockTimeout.label()),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThemeButtonRow(
            leftLabel = Res.string.privacy_lock_timeout_immediate,
            leftSelected = privacyLockTimeout == PrivacyLockTimeout.Immediate,
            onLeftClick = { onPrivacyLockTimeoutChange(PrivacyLockTimeout.Immediate) },
            rightLabel = Res.string.privacy_lock_timeout_one_minute,
            rightSelected = privacyLockTimeout == PrivacyLockTimeout.OneMinute,
            onRightClick = { onPrivacyLockTimeoutChange(PrivacyLockTimeout.OneMinute) },
        )
        ThemeButtonRow(
            leftLabel = Res.string.privacy_lock_timeout_five_minutes,
            leftSelected = privacyLockTimeout == PrivacyLockTimeout.FiveMinutes,
            onLeftClick = { onPrivacyLockTimeoutChange(PrivacyLockTimeout.FiveMinutes) },
            rightLabel = Res.string.privacy_lock_timeout_fifteen_minutes,
            rightSelected = privacyLockTimeout == PrivacyLockTimeout.FifteenMinutes,
            onRightClick = { onPrivacyLockTimeoutChange(PrivacyLockTimeout.FifteenMinutes) },
        )
        ThemeModeButton(
            label = Res.string.privacy_lock_timeout_disabled,
            selected = privacyLockTimeout == PrivacyLockTimeout.Disabled,
            onClick = { onPrivacyLockTimeoutChange(PrivacyLockTimeout.Disabled) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.privacy_lock_reauth_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.privacy_lock_runtime_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.privacy_lock_urgent_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RelationalBoundarySettingsPanel() {
    StatusBlock(
        title = Res.string.relational_boundary_title,
        body = Res.string.relational_boundary_description,
    ) {
        Text(
            text = stringResource(Res.string.relational_boundary_memory_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.relational_boundary_memory_runtime_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.relational_boundary_notification_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HarmSafetySettingsPanel() {
    StatusBlock(
        title = Res.string.harm_safety_title,
        body = Res.string.harm_safety_description,
    ) {
        Text(
            text = stringResource(Res.string.harm_safety_capability_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.harm_safety_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EncryptedExportSyncSettingsPanel() {
    StatusBlock(
        title = Res.string.encrypted_sync_title,
        body = Res.string.encrypted_sync_description,
    ) {
        Text(
            text = stringResource(Res.string.encrypted_sync_default_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.encrypted_sync_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.encrypted_sync_calendar_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReleaseReadinessSettingsPanel() {
    StatusBlock(
        title = Res.string.release_readiness_title,
        body = Res.string.release_readiness_description,
    ) {
        StatusLine(
            title = Res.string.release_readiness_red_team_title,
            body = Res.string.release_readiness_red_team_body,
        )
        StatusLine(
            title = Res.string.release_readiness_performance_title,
            body = Res.string.release_readiness_performance_body,
        )
        StatusLine(
            title = Res.string.release_readiness_store_title,
            body = Res.string.release_readiness_store_body,
        )
        StatusLine(
            title = Res.string.release_readiness_owner_gates_title,
            body = Res.string.release_readiness_owner_gates_body,
        )
        StatusLine(
            title = Res.string.release_readiness_artifact_title,
            body = Res.string.release_readiness_artifact_body,
        )
        Text(
            text = stringResource(Res.string.release_readiness_review_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun PrivacyLockTimeout.label(): StringResource = when (this) {
    PrivacyLockTimeout.Disabled -> Res.string.privacy_lock_timeout_disabled
    PrivacyLockTimeout.Immediate -> Res.string.privacy_lock_timeout_immediate
    PrivacyLockTimeout.OneMinute -> Res.string.privacy_lock_timeout_one_minute
    PrivacyLockTimeout.FiveMinutes -> Res.string.privacy_lock_timeout_five_minutes
    PrivacyLockTimeout.FifteenMinutes -> Res.string.privacy_lock_timeout_fifteen_minutes
}

private fun AiGrowthMode.title(): StringResource = when (this) {
    AiGrowthMode.QuickGuidance -> Res.string.ai_growth_quick_guidance_title
    AiGrowthMode.GuidedReflection -> Res.string.ai_growth_guided_reflection_title
    AiGrowthMode.DeepExploration -> Res.string.ai_growth_deep_exploration_title
    AiGrowthMode.ActionOnly -> Res.string.ai_growth_action_only_title
}

private fun AiGrowthMode.description(): StringResource = when (this) {
    AiGrowthMode.QuickGuidance -> Res.string.ai_growth_quick_guidance_description
    AiGrowthMode.GuidedReflection -> Res.string.ai_growth_guided_reflection_description
    AiGrowthMode.DeepExploration -> Res.string.ai_growth_deep_exploration_description
    AiGrowthMode.ActionOnly -> Res.string.ai_growth_action_only_description
}

private fun DailyMetricLevel.label(): StringResource = when (this) {
    DailyMetricLevel.VeryLow -> Res.string.daily_metric_very_low
    DailyMetricLevel.Low -> Res.string.daily_metric_low
    DailyMetricLevel.Steady -> Res.string.daily_metric_steady
    DailyMetricLevel.High -> Res.string.daily_metric_high
    DailyMetricLevel.VeryHigh -> Res.string.daily_metric_very_high
}

private fun DailyRecordSaveResult?.messageResource(
    checkInCaptured: Boolean,
    dailyToolsUnlocked: Boolean,
): StringResource =
    when (this) {
        is DailyRecordSaveResult.Saved -> Res.string.daily_checkin_encrypted_saved
        DailyRecordSaveResult.StorageUnavailable -> Res.string.daily_checkin_storage_unavailable
        DailyRecordSaveResult.Failed -> Res.string.daily_checkin_save_failed
        null -> when {
            !dailyToolsUnlocked -> Res.string.daily_checkin_locked
            checkInCaptured -> Res.string.daily_checkin_encrypted_saved
            else -> Res.string.daily_checkin_privacy_note
        }
    }

private fun ReminderPlatformStatus.resource(): StringResource =
    if (available && neutralPreviewOnly) {
        Res.string.platform_reminder_available
    } else {
        Res.string.platform_reminder_unavailable
    }

private fun CalendarPlatformStatus.resource(): StringResource =
    if (available && explicitHandoffOnly && !readsBroadCalendarData) {
        Res.string.platform_calendar_available
    } else {
        Res.string.platform_calendar_unavailable
    }

private fun SpeechPlatformStatus.resource(): StringResource =
    if (available && osOfflinePreferred && requiresExplicitMicrophonePermission && textFallbackAvailable) {
        Res.string.platform_speech_available
    } else {
        Res.string.platform_speech_unavailable
    }

private fun ModelPackPlatformStatus.resource(): StringResource =
    when {
        installerAvailable && runtimeAvailable && requiresSignedManifest && autoDownloadDisabled ->
            Res.string.platform_model_pack_runtime_available

        installState == ModelPackInstallState.Installed && !runtimeAvailable ->
            Res.string.platform_model_pack_installed_runtime_unavailable

        installerAvailable && requiresSignedManifest && autoDownloadDisabled ->
            Res.string.platform_model_pack_installer_ready

        else -> Res.string.platform_model_pack_unavailable
    }

private fun ModelPackInstallState.resource(): StringResource = when (this) {
    ModelPackInstallState.NotInstalled -> Res.string.model_pack_state_not_installed
    ModelPackInstallState.AwaitingUserSelection -> Res.string.model_pack_state_awaiting_selection
    ModelPackInstallState.Installing -> Res.string.model_pack_state_installing
    ModelPackInstallState.Installed -> Res.string.model_pack_state_installed
    ModelPackInstallState.Failed -> Res.string.model_pack_state_failed
    ModelPackInstallState.InstallerUnavailable -> Res.string.model_pack_state_installer_unavailable
}

private fun ModelPackInstallFailure.resource(): StringResource = when (this) {
    ModelPackInstallFailure.SelectionCanceled -> Res.string.model_pack_failure_selection_canceled
    ModelPackInstallFailure.MissingManifest -> Res.string.model_pack_failure_missing_manifest
    ModelPackInstallFailure.MissingArtifact -> Res.string.model_pack_failure_missing_artifact
    ModelPackInstallFailure.InvalidManifest -> Res.string.model_pack_failure_invalid_manifest
    ModelPackInstallFailure.UnapprovedModel -> Res.string.model_pack_failure_unapproved_model
    ModelPackInstallFailure.UntrustedSigningKey -> Res.string.model_pack_failure_untrusted_signing_key
    ModelPackInstallFailure.InvalidSignature -> Res.string.model_pack_failure_invalid_signature
    ModelPackInstallFailure.ChecksumMismatch -> Res.string.model_pack_failure_checksum_mismatch
    ModelPackInstallFailure.ArtifactSizeMismatch -> Res.string.model_pack_failure_artifact_size_mismatch
    ModelPackInstallFailure.StorageFailed -> Res.string.model_pack_failure_storage_failed
    ModelPackInstallFailure.PlatformVerifierUnavailable -> Res.string.model_pack_failure_platform_verifier_unavailable
}

private fun org.bettamind.shared.daily.BreathingStep.instruction(): StringResource =
    when (instructionKey) {
        "daily_breathing_inhale" -> Res.string.daily_breathing_inhale
        "daily_breathing_hold" -> Res.string.daily_breathing_hold
        "daily_breathing_exhale" -> Res.string.daily_breathing_exhale
        else -> Res.string.daily_breathing_rest
    }

private fun org.bettamind.shared.daily.GroundingStep.prompt(): StringResource =
    when (promptKey) {
        "daily_grounding_see" -> Res.string.daily_grounding_see
        "daily_grounding_touch" -> Res.string.daily_grounding_touch
        "daily_grounding_hear" -> Res.string.daily_grounding_hear
        "daily_grounding_smell" -> Res.string.daily_grounding_smell
        else -> Res.string.daily_grounding_taste
    }

private fun DecisionWorksheetKind.title(): StringResource = when (this) {
    DecisionWorksheetKind.ValuesToAction -> Res.string.daily_worksheet_values_title
    DecisionWorksheetKind.ProblemSolving -> Res.string.daily_worksheet_problem_title
    DecisionWorksheetKind.RepairPreparation -> Res.string.daily_worksheet_repair_title
    DecisionWorksheetKind.DifficultConversation -> Res.string.daily_worksheet_conversation_title
}

private fun worksheetPrompt(key: String): StringResource = when (key) {
    "daily_worksheet_values_prompt" -> Res.string.daily_worksheet_values_prompt
    "daily_worksheet_action_prompt" -> Res.string.daily_worksheet_action_prompt
    "daily_worksheet_next_step_prompt" -> Res.string.daily_worksheet_next_step_prompt
    "daily_worksheet_problem_prompt" -> Res.string.daily_worksheet_problem_prompt
    "daily_worksheet_options_prompt" -> Res.string.daily_worksheet_options_prompt
    "daily_worksheet_first_step_prompt" -> Res.string.daily_worksheet_first_step_prompt
    "daily_worksheet_repair_effect_prompt" -> Res.string.daily_worksheet_repair_effect_prompt
    "daily_worksheet_repair_need_prompt" -> Res.string.daily_worksheet_repair_need_prompt
    "daily_worksheet_repair_action_prompt" -> Res.string.daily_worksheet_repair_action_prompt
    "daily_worksheet_conversation_goal_prompt" -> Res.string.daily_worksheet_conversation_goal_prompt
    "daily_worksheet_conversation_boundary_prompt" -> Res.string.daily_worksheet_conversation_boundary_prompt
    else -> Res.string.daily_worksheet_conversation_next_prompt
}

private fun AiGrowthResponseSource.label(): StringResource = when (this) {
    AiGrowthResponseSource.LocalModel -> Res.string.ai_growth_source_local_model
    AiGrowthResponseSource.DeterministicFallback -> Res.string.ai_growth_source_deterministic_fallback
}

private fun AiGrowthFallbackReason.label(): StringResource = when (this) {
    AiGrowthFallbackReason.None -> Res.string.ai_growth_fallback_reason_none
    AiGrowthFallbackReason.NoModelAvailable -> Res.string.ai_growth_fallback_reason_no_model
    AiGrowthFallbackReason.PreGenerationHarmSafety -> Res.string.ai_growth_fallback_reason_harm_safety
    AiGrowthFallbackReason.PreGenerationRelationalBoundary -> Res.string.ai_growth_fallback_reason_relational_boundary
    AiGrowthFallbackReason.MalformedModelOutput -> Res.string.ai_growth_fallback_reason_malformed_output
    AiGrowthFallbackReason.UnsafeGeneratedOutput -> Res.string.ai_growth_fallback_reason_unsafe_output
    AiGrowthFallbackReason.ModelGenerationFailed -> Res.string.ai_growth_fallback_reason_generation_failed
}

private fun fallbackResource(key: String?): StringResource = when (key) {
    "ai_growth_fallback_quick_guidance" -> Res.string.ai_growth_fallback_quick_guidance
    "ai_growth_fallback_guided_reflection" -> Res.string.ai_growth_fallback_guided_reflection
    "ai_growth_fallback_deep_exploration" -> Res.string.ai_growth_fallback_deep_exploration
    "ai_growth_fallback_action_only" -> Res.string.ai_growth_fallback_action_only
    "ai_growth_fallback_model_generation_failed" -> Res.string.ai_growth_fallback_model_generation_failed
    "ai_growth_fallback_malformed_model_output" -> Res.string.ai_growth_fallback_malformed_model_output
    "relational_fallback_software_boundary" -> Res.string.relational_fallback_software_boundary
    "relational_fallback_sexual_boundary" -> Res.string.relational_fallback_sexual_boundary
    "relational_fallback_dependency_support" -> Res.string.relational_fallback_dependency_support
    "relational_fallback_human_relationship_support" -> Res.string.relational_fallback_human_relationship_support
    "relational_fallback_clinical_scope" -> Res.string.relational_fallback_clinical_scope
    "relational_fallback_emergency_scope" -> Res.string.relational_fallback_emergency_scope
    "relational_fallback_urgent_safety" -> Res.string.relational_fallback_urgent_safety
    "harm_safety_fallback_clarify" -> Res.string.harm_safety_fallback_clarify
    "harm_safety_fallback_refuse_capability" -> Res.string.harm_safety_fallback_refuse_capability
    "harm_safety_fallback_self_harm_support" -> Res.string.harm_safety_fallback_self_harm_support
    "harm_safety_fallback_suicide_immediate" -> Res.string.harm_safety_fallback_suicide_immediate
    "harm_safety_fallback_threat_deescalation" -> Res.string.harm_safety_fallback_threat_deescalation
    "harm_safety_fallback_safe_prevention" -> Res.string.harm_safety_fallback_safe_prevention
    "harm_safety_fallback_emergency_response" -> Res.string.harm_safety_fallback_emergency_response
    "harm_safety_fallback_intrusive_thought" -> Res.string.harm_safety_fallback_intrusive_thought
    "harm_safety_fallback_anger_deescalation" -> Res.string.harm_safety_fallback_anger_deescalation
    "harm_safety_fallback_safe_disposal" -> Res.string.harm_safety_fallback_safe_disposal
    "harm_safety_fallback_policy_bypass" -> Res.string.harm_safety_fallback_policy_bypass
    "compassionate_safety_fallback_anger_without_intent" -> Res.string.compassionate_safety_fallback_anger_without_intent
    "compassionate_safety_fallback_intrusive_thought" -> Res.string.compassionate_safety_fallback_intrusive_thought
    "compassionate_safety_fallback_direct_harm_intent" -> Res.string.compassionate_safety_fallback_direct_harm_intent
    "compassionate_safety_fallback_revenge_planning" -> Res.string.compassionate_safety_fallback_revenge_planning
    "compassionate_safety_fallback_self_harm_concern" -> Res.string.compassionate_safety_fallback_self_harm_concern
    "compassionate_safety_fallback_self_harm_method" -> Res.string.compassionate_safety_fallback_self_harm_method
    "compassionate_safety_fallback_dangerous_capability" -> Res.string.compassionate_safety_fallback_dangerous_capability
    "compassionate_safety_fallback_chemical_weapon_explosive_poison" ->
        Res.string.compassionate_safety_fallback_chemical_weapon_explosive_poison
    "compassionate_safety_fallback_relational_dependency" -> Res.string.compassionate_safety_fallback_relational_dependency
    "compassionate_safety_fallback_romantic_dependency" -> Res.string.compassionate_safety_fallback_romantic_dependency
    "compassionate_safety_fallback_sexualized_bettamind" -> Res.string.compassionate_safety_fallback_sexualized_bettamind
    "compassionate_safety_fallback_unsafe_reminder" -> Res.string.compassionate_safety_fallback_unsafe_reminder
    "compassionate_safety_fallback_ambiguous" -> Res.string.compassionate_safety_fallback_ambiguous
    "compassionate_safety_fallback_safe_prevention" -> Res.string.compassionate_safety_fallback_safe_prevention
    "compassionate_safety_fallback_emergency_response" -> Res.string.compassionate_safety_fallback_emergency_response
    "compassionate_safety_fallback_shame_after_thought" -> Res.string.compassionate_safety_fallback_shame_after_thought
    "compassionate_safety_fallback_help_not_to_act" -> Res.string.compassionate_safety_fallback_help_not_to_act
    "compassionate_safety_fallback_unsafe_generated_output" -> Res.string.compassionate_safety_fallback_unsafe_generated_output
    "compassionate_safety_fallback_invalid_generated_output" -> Res.string.compassionate_safety_fallback_invalid_generated_output
    else -> Res.string.ai_growth_fallback_quick_guidance
}

private fun actionResource(key: String): StringResource = when (key) {
    "ai_growth_action_reflect_choice_consequence" -> Res.string.ai_growth_action_reflect_choice_consequence
    "ai_growth_action_map_pattern_values_repair" -> Res.string.ai_growth_action_map_pattern_values_repair
    "ai_growth_action_choose_one_concrete_step" -> Res.string.ai_growth_action_choose_one_concrete_step
    else -> Res.string.ai_growth_action_pause_name_next_step
}

private fun SafetySupportRiskLevel.label(): StringResource = when (this) {
    SafetySupportRiskLevel.None -> Res.string.support_risk_none
    SafetySupportRiskLevel.Reflective -> Res.string.support_risk_reflective
    SafetySupportRiskLevel.Concern -> Res.string.support_risk_concern
    SafetySupportRiskLevel.Urgent -> Res.string.support_risk_urgent
    SafetySupportRiskLevel.Immediate -> Res.string.support_risk_immediate
    SafetySupportRiskLevel.RefusedCapability -> Res.string.support_risk_refused_capability
}

private fun SafetySupportActionType.title(): StringResource = when (this) {
    SafetySupportActionType.DailyCheckIn -> Res.string.support_bridge_action_check_in_title
    SafetySupportActionType.GroundingExercise -> Res.string.support_bridge_action_grounding_title
    SafetySupportActionType.BreathingTimer -> Res.string.support_bridge_action_breathing_title
    SafetySupportActionType.DelayAction -> Res.string.support_bridge_action_delay_title
    SafetySupportActionType.LeaveSituation -> Res.string.support_bridge_action_leave_title
    SafetySupportActionType.ContactTrustedPerson -> Res.string.support_bridge_action_trusted_person_title
    SafetySupportActionType.UseLocalEmergencyHelp -> Res.string.support_bridge_action_emergency_title
    SafetySupportActionType.ConflictReflection -> Res.string.support_bridge_action_conflict_reflection_title
    SafetySupportActionType.RepairPlanning -> Res.string.support_bridge_action_repair_title
    SafetySupportActionType.NonviolentMessage -> Res.string.support_bridge_action_nonviolent_message_title
    SafetySupportActionType.ValuesToAction -> Res.string.support_bridge_action_values_title
    SafetySupportActionType.SafePrevention -> Res.string.support_bridge_action_safe_prevention_title
    SafetySupportActionType.NoActionNeeded -> Res.string.support_bridge_action_no_action_title
}

private fun SafetySupportActionType.body(): StringResource = when (this) {
    SafetySupportActionType.DailyCheckIn -> Res.string.support_bridge_action_check_in_body
    SafetySupportActionType.GroundingExercise -> Res.string.support_bridge_action_grounding_body
    SafetySupportActionType.BreathingTimer -> Res.string.support_bridge_action_breathing_body
    SafetySupportActionType.DelayAction -> Res.string.support_bridge_action_delay_body
    SafetySupportActionType.LeaveSituation -> Res.string.support_bridge_action_leave_body
    SafetySupportActionType.ContactTrustedPerson -> Res.string.support_bridge_action_trusted_person_body
    SafetySupportActionType.UseLocalEmergencyHelp -> Res.string.support_bridge_action_emergency_body
    SafetySupportActionType.ConflictReflection -> Res.string.support_bridge_action_conflict_reflection_body
    SafetySupportActionType.RepairPlanning -> Res.string.support_bridge_action_repair_body
    SafetySupportActionType.NonviolentMessage -> Res.string.support_bridge_action_nonviolent_message_body
    SafetySupportActionType.ValuesToAction -> Res.string.support_bridge_action_values_body
    SafetySupportActionType.SafePrevention -> Res.string.support_bridge_action_safe_prevention_body
    SafetySupportActionType.NoActionNeeded -> Res.string.support_bridge_action_no_action_body
}

private fun SafetySupportActionType.opensLocalTool(): Boolean = when (this) {
    SafetySupportActionType.DailyCheckIn,
    SafetySupportActionType.GroundingExercise,
    SafetySupportActionType.BreathingTimer,
    SafetySupportActionType.DelayAction,
    SafetySupportActionType.ConflictReflection,
    SafetySupportActionType.RepairPlanning,
    SafetySupportActionType.NonviolentMessage,
    SafetySupportActionType.ValuesToAction,
    -> true

    SafetySupportActionType.LeaveSituation,
    SafetySupportActionType.ContactTrustedPerson,
    SafetySupportActionType.UseLocalEmergencyHelp,
    SafetySupportActionType.SafePrevention,
    SafetySupportActionType.NoActionNeeded,
    -> false
}

private fun LocalSupportResourceScope.title(): StringResource = when (this) {
    LocalSupportResourceScope.LocalEmergency -> Res.string.support_bridge_resource_local_emergency_title
    LocalSupportResourceScope.LocalCrisisOrCommunitySupport -> Res.string.support_bridge_resource_local_crisis_title
    LocalSupportResourceScope.TrustedPerson -> Res.string.support_bridge_resource_trusted_person_title
    LocalSupportResourceScope.ProfessionalSupport -> Res.string.support_bridge_resource_professional_title
}

private fun LocalSupportResourceScope.body(): StringResource = when (this) {
    LocalSupportResourceScope.LocalEmergency -> Res.string.support_bridge_resource_local_emergency_body
    LocalSupportResourceScope.LocalCrisisOrCommunitySupport -> Res.string.support_bridge_resource_local_crisis_body
    LocalSupportResourceScope.TrustedPerson -> Res.string.support_bridge_resource_trusted_person_body
    LocalSupportResourceScope.ProfessionalSupport -> Res.string.support_bridge_resource_professional_body
}

@Composable
private fun MetricSelector(
    title: StringResource,
    selected: DailyMetricLevel,
    onSelectedChange: (DailyMetricLevel) -> Unit,
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
    )
    ThemeButtonRow(
        leftLabel = DailyMetricLevel.VeryLow.label(),
        leftSelected = selected == DailyMetricLevel.VeryLow,
        onLeftClick = { onSelectedChange(DailyMetricLevel.VeryLow) },
        rightLabel = DailyMetricLevel.Low.label(),
        rightSelected = selected == DailyMetricLevel.Low,
        onRightClick = { onSelectedChange(DailyMetricLevel.Low) },
    )
    ThemeButtonRow(
        leftLabel = DailyMetricLevel.Steady.label(),
        leftSelected = selected == DailyMetricLevel.Steady,
        onLeftClick = { onSelectedChange(DailyMetricLevel.Steady) },
        rightLabel = DailyMetricLevel.High.label(),
        rightSelected = selected == DailyMetricLevel.High,
        onRightClick = { onSelectedChange(DailyMetricLevel.High) },
    )
    ThemeModeButton(
        label = DailyMetricLevel.VeryHigh.label(),
        selected = selected == DailyMetricLevel.VeryHigh,
        onClick = { onSelectedChange(DailyMetricLevel.VeryHigh) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WorksheetButtonRow(
    leftKind: DecisionWorksheetKind,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    rightKind: DecisionWorksheetKind,
    rightSelected: Boolean,
    onRightClick: () -> Unit,
) {
    ThemeButtonRow(
        leftLabel = leftKind.title(),
        leftSelected = leftSelected,
        onLeftClick = onLeftClick,
        rightLabel = rightKind.title(),
        rightSelected = rightSelected,
        onRightClick = onRightClick,
    )
}

@Composable
private fun AiModeButtonRow(
    leftMode: AiGrowthMode,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    rightMode: AiGrowthMode,
    rightSelected: Boolean,
    onRightClick: () -> Unit,
) {
    ThemeButtonRow(
        leftLabel = leftMode.title(),
        leftSelected = leftSelected,
        onLeftClick = onLeftClick,
        rightLabel = rightMode.title(),
        rightSelected = rightSelected,
        onRightClick = onRightClick,
    )
}

@Composable
private fun ThemeButtonRow(
    leftLabel: StringResource,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    rightLabel: StringResource,
    rightSelected: Boolean,
    onRightClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeModeButton(
            label = leftLabel,
            selected = leftSelected,
            onClick = onLeftClick,
            modifier = Modifier.weight(1f),
        )
        ThemeModeButton(
            label = rightLabel,
            selected = rightSelected,
            onClick = onRightClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeModeButton(
    label: StringResource,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Text(stringResource(label))
    }
    if (selected) {
        Button(
            modifier = modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(),
            onClick = onClick,
            content = { content() },
        )
    } else {
        OutlinedButton(
            modifier = modifier.heightIn(min = 48.dp),
            onClick = onClick,
            content = { content() },
        )
    }
}
