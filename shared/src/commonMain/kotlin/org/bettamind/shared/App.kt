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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.bettamind.shared.design.BettamindThemeMode
import org.bettamind.shared.design.BettamindTheme
import org.bettamind.shared.generated.resources.Res
import org.bettamind.shared.generated.resources.*
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

@Composable
fun BettamindApp() {
    var selectedDestination by remember { mutableStateOf(BettamindDestination.Today) }
    var themeMode by remember { mutableStateOf(BettamindThemeMode.Light) }
    var useAccessibleTypography by remember { mutableStateOf(false) }

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
                text = stringResource(Res.string.phase_two_status),
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
                )
            } else {
                PlaceholderStatus()
            }
        }
    }
}

@Composable
private fun PlaceholderStatus() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.placeholder_status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SettingsPanel(
    themeMode: BettamindThemeMode,
    onThemeModeChange: (BettamindThemeMode) -> Unit,
    useAccessibleTypography: Boolean,
    onAccessibleTypographyChange: (Boolean) -> Unit,
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
    }
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
