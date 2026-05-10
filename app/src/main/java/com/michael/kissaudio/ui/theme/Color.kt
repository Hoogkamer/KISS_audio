package com.michael.kissaudio.ui.theme

import androidx.compose.ui.graphics.Color

// Braun Inspired Palette
val BraunWhite = Color(0xFFF2F2F2)
val BraunGray = Color(0xFFE0E0E0)
val SignalGray = Color(0xFF8E8E93)
val BraunMatteCharcoal = Color(0xFF1A1A1A) // Deep matte-charcoal, not blue-black
val BraunOrange = Color(0xFFF2994A) // The "Active" functional color

// Light theme mapping
val LightPrimary = BraunOrange
val LightOnPrimary = Color.White
val LightPrimaryContainer = BraunGray
val LightOnPrimaryContainer = BraunMatteCharcoal
val LightBackground = BraunWhite
val LightOnBackground = BraunMatteCharcoal
val LightSurface = Color.White
val LightOnSurface = BraunMatteCharcoal
val LightOutline = SignalGray

// Dark theme mapping
val DarkPrimary = BraunOrange
val DarkOnPrimary = BraunMatteCharcoal
val DarkPrimaryContainer = Color(0xFF262626)
val DarkOnPrimaryContainer = BraunWhite
val DarkBackground = Color(0xFF121212) // Pure dark matte
val DarkOnBackground = BraunWhite
val DarkSurface = BraunMatteCharcoal
val DarkOnSurface = BraunWhite
val DarkOutline = SignalGray
