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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
private const val PhaseFourEncryptedStorageAvailable = false

@Composable
fun BettamindApp() {
    var selectedDestination by remember { mutableStateOf(BettamindDestination.Today) }
    var themeMode by remember { mutableStateOf(BettamindThemeMode.Light) }
    var useAccessibleTypography by remember { mutableStateOf(false) }
    var growthState by remember { mutableStateOf(DeterministicGrowthEngine.locked()) }
    var privacyLockTimeout by remember { mutableStateOf(PrivacyLockTimeout.OneMinute) }

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
                    privacyLockTimeout = privacyLockTimeout,
                    onPrivacyLockTimeoutChange = { privacyLockTimeout = it },
                    growthState = growthState,
                    onConfirmAdult = {
                        growthState = DeterministicGrowthEngine.adultConfirmed(
                            encryptedStorageAvailable = PhaseFourEncryptedStorageAvailable,
                        )
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
            contentDescription = null,
            modifier = Modifier.size(64.dp),
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
                text = stringResource(Res.string.phase_four_status),
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
    privacyLockTimeout: PrivacyLockTimeout,
    onPrivacyLockTimeoutChange: (PrivacyLockTimeout) -> Unit,
    growthState: GrowthSessionState,
    onConfirmAdult: () -> Unit,
    onBlockMinorOrUnknown: () -> Unit,
    onAdvanceGrowth: () -> Unit,
    onResetGrowth: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                    privacyLockTimeout = privacyLockTimeout,
                    onPrivacyLockTimeoutChange = onPrivacyLockTimeoutChange,
                )
            } else {
                GrowthDestinationContent(
                    destination = destination,
                    growthState = growthState,
                    onConfirmAdult = onConfirmAdult,
                    onBlockMinorOrUnknown = onBlockMinorOrUnknown,
                    onAdvanceGrowth = onAdvanceGrowth,
                    onResetGrowth = onResetGrowth,
                )
            }
        }
    }
}

@Composable
private fun GrowthDestinationContent(
    destination: BettamindDestination,
    growthState: GrowthSessionState,
    onConfirmAdult: () -> Unit,
    onBlockMinorOrUnknown: () -> Unit,
    onAdvanceGrowth: () -> Unit,
    onResetGrowth: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AdultGatePanel(
            growthState = growthState,
            onConfirmAdult = onConfirmAdult,
            onBlockMinorOrUnknown = onBlockMinorOrUnknown,
        )
        StorageStatusPanel(growthState.narrativeStorageStatus)
        when (destination) {
            BettamindDestination.Today -> TodayGrowthPanel(
                growthState = growthState,
                onAdvanceGrowth = onAdvanceGrowth,
                onResetGrowth = onResetGrowth,
            )

            BettamindDestination.Reflect -> StepMapPanel(growthState)
            BettamindDestination.Grow -> GrowthSummaryPanel(growthState)
            BettamindDestination.Support -> SupportPanel()
            BettamindDestination.Settings -> Unit
        }
    }
}

@Composable
private fun AdultGatePanel(
    growthState: GrowthSessionState,
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
                    onClick = onConfirmAdult,
                ) {
                    Text(stringResource(Res.string.growth_age_confirm_adult))
                }
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    onClick = onBlockMinorOrUnknown,
                ) {
                    Text(stringResource(Res.string.growth_age_not_adult))
                }
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
private fun SupportPanel() {
    StatusBlock(
        title = Res.string.growth_support_title,
        body = Res.string.growth_support_description,
    )
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
    privacyLockTimeout: PrivacyLockTimeout,
    onPrivacyLockTimeoutChange: (PrivacyLockTimeout) -> Unit,
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
                    text = stringResource(Res.string.settings_font_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        if (useAccessibleTypography) {
                            Res.string.settings_font_accessible
                        } else {
                            Res.string.settings_font_default
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = useAccessibleTypography,
                onCheckedChange = onAccessibleTypographyChange,
            )
        }
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
            text = stringResource(Res.string.relational_boundary_notification_note),
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
