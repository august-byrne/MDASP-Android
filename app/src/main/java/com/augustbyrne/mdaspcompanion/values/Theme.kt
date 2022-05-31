/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.augustbyrne.mdaspcompanion.values

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = DarkBlue40,
    onSecondary = Color.White,
    secondaryContainer = Green90,//DarkBlue90,
    onSecondaryContainer = Green10,//DarkBlue10,
    tertiary = yellow500,
    onTertiary = Color.White,
    tertiaryContainer = Red90,
    onTertiaryContainer = Red10,
    //surface = yellow50,
    //surfaceVariant = yellow100,
    //background = yellow50,
    outline = BlueGrey60,
    inverseSurface = Color.Gray,
    inverseOnSurface = Color.White
)
private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue90,
    secondary = DarkBlue80,
    onSecondary = DarkBlue20,
    secondaryContainer = Green40,//DarkBlue30,
    onSecondaryContainer = Green90,//DarkBlue90,
    tertiary = yellow700,
    onTertiary = Yellow20,
    tertiaryContainer = Red40,
    onTertiaryContainer = Red90,
    outline = BlueGrey60,
    surfaceVariant = Color.DarkGray,
    inverseSurface = Color.Gray,
    inverseOnSurface = Color.Black
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = typography,
        content = content
    )
}
