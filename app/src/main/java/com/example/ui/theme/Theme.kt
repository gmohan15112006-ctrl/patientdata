package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = HospitalTealPrimary,
    onPrimary = HospitalTealOnPrimary,
    primaryContainer = HospitalTealContainer,
    onPrimaryContainer = HospitalTealOnContainer,
    secondary = HospitalNavySecondary,
    onSecondary = HospitalNavyOnSecondary,
    secondaryContainer = HospitalNavyContainer,
    onSecondaryContainer = HospitalNavyOnContainer,
    tertiary = HospitalEmergencyTertiary,
    onTertiary = HospitalEmergencyOnTertiary,
    tertiaryContainer = HospitalEmergencyContainer,
    onTertiaryContainer = HospitalEmergencyOnContainer,
    background = ClinicalBackground,
    onBackground = ClinicalTextPrimary,
    surface = ClinicalSurface,
    onSurface = ClinicalTextPrimary,
    surfaceVariant = ClinicalSurfaceVariant,
    onSurfaceVariant = ClinicalTextSecondary,
    outline = ClinicalOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = HospitalTealDark,
    onPrimary = HospitalTealOnDark,
    primaryContainer = HospitalTealContainerDark,
    onPrimaryContainer = HospitalTealContainer,
    secondary = HospitalNavyDark,
    onSecondary = HospitalNavyOnSecondary,
    secondaryContainer = HospitalNavySecondary,
    onSecondaryContainer = HospitalNavyContainer,
    tertiary = HospitalEmergencyDark,
    onTertiary = HospitalEmergencyOnTertiary,
    tertiaryContainer = HospitalEmergencyContainer,
    onTertiaryContainer = HospitalEmergencyOnContainer,
    background = ClinicalBackgroundDark,
    onBackground = ClinicalSurface,
    surface = ClinicalSurfaceDark,
    onSurface = ClinicalSurface,
    surfaceVariant = ClinicalSurfaceVariantDark,
    onSurfaceVariant = ClinicalOutline,
    outline = ClinicalOutlineDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our purposeful clinical colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
