package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class SpiderThemePreset(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val cardBackground: Color
) {
    GOTHIC_NOIR(
        title = "Gothic Noir",
        primaryColor = Color(0xFFECEFF1),
        secondaryColor = Color(0xFF78909C),
        accentColor = Color(0xFFFF3D00),
        backgroundColor = Color(0xFF090B10),
        cardBackground = Color(0xFF131722)
    ),
    NEON_CYBER(
        title = "Neon Cyber",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF80D8FF),
        accentColor = Color(0xFFFF4081),
        backgroundColor = Color(0xFF080D1A),
        cardBackground = Color(0xFF10192E)
    ),
    STEAMPUNK_BRASS(
        title = "Steampunk Brass",
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFFB74D),
        accentColor = Color(0xFFFF7043),
        backgroundColor = Color(0xFF100B08),
        cardBackground = Color(0xFF1F1510)
    ),
    VENOM_EMERALD(
        title = "Venom Emerald",
        primaryColor = Color(0xFF69F0AE),
        secondaryColor = Color(0xFF00E676),
        accentColor = Color(0xFF76FF03),
        backgroundColor = Color(0xFF07110B),
        cardBackground = Color(0xFF0E1F16)
    )
}
