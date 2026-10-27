package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// Multi-Color 3D Sculpted Palette (Crimson Red, Sapphire Blue, Emerald Green, Royal Violet, Cyber Cyan, Hyper Magenta — NO Gold/Yellow)
val RoseNeon = Color(0xFFFF2A5F)         // Vibrant 3D Crimson Red
val SapphireBlue = Color(0xFF2979FF)     // Electric 3D Cobalt Blue
val TitaniumCyan = Color(0xFF00E5FF)     // Cyber 3D Neon Cyan
val RoyalViolet = Color(0xFF9D4EDD)      // Sculpted 3D Amethyst Violet
val MintTurquoise = Color(0xFF00E676)    // Matrix 3D Emerald Green
val ElectricMagenta = Color(0xFFF50057)  // Hyper 3D Rose Magenta
val CoralSunset = Color(0xFFFF5252)      // Ruby 3D Red-Coral

// Mapped to Sapphire Blue & Ice Specular so nothing ever renders yellow/gold
val LuxuryGold = SapphireBlue
val ChampagneHighlight = Color(0xFF82B1FF)

val ExecutiveSlateDark = Color(0xFF050A14)
val ExecutiveSlateMid = Color(0xFF0B1426)
val ExecutiveSlateLight = Color(0xFF13223D)
val ExecutiveBorder = Color(0xFF22385E)

val DeepObsidian = ExecutiveSlateDark
val GlassEmeraldDark = ExecutiveSlateMid
val GlassEmeraldMid = ExecutiveSlateLight
val GlassCyanDark = ExecutiveSlateMid

private val Tab3DPalette = listOf(
    MintTurquoise,   // Emerald Green
    RoseNeon,        // Crimson Red
    SapphireBlue,    // Electric Blue
    RoyalViolet,     // Amethyst Violet
    TitaniumCyan,    // Cyber Cyan
    ElectricMagenta  // Hyper Magenta
)

/**
 * 3D Sculpted Multi-Color Modal Shell with raised bevel category tabs (Red, Blue, Emerald, Violet, Cyan, Magenta),
 * top specular glass highlight, and bottom 3D depth rim.
 */
@Composable
fun PremiumModalShell(
    badgeText: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    secondaryAccent: Color = RoseNeon,
    onClose: () -> Unit,
    tabs: List<Pair<String, String>> = emptyList(),
    selectedTab: String = "",
    onSelectTab: (String) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "modal_glow")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "modal_wave_phase"
    )

    val activeTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
    val dynamicAccent = if (tabs.isNotEmpty()) {
        Tab3DPalette[activeTabIndex % Tab3DPalette.size]
    } else {
        accentColor
    }

    val outerShape = RoundedCornerShape(22.dp)
    val headerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 14.dp, bottomEnd = 14.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2030710))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = outerShape,
            color = ExecutiveSlateDark,
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        dynamicAccent.copy(alpha = 0.90f),
                        SapphireBlue.copy(alpha = 0.70f),
                        RoseNeon.copy(alpha = 0.65f),
                        TitaniumCyan.copy(alpha = 0.75f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 16.dp, shape = outerShape, spotColor = dynamicAccent)
                .testTag("jarvis_active_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Compact 3D Sculpted Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(headerShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    dynamicAccent.copy(alpha = 0.22f),
                                    Color(0xFF0E192D),
                                    Color(0xFF070D18)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    dynamicAccent.copy(alpha = 0.70f),
                                    Color(0xFF040811)
                                )
                            ),
                            shape = headerShape
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dynamicAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            color = Color(0xFFB8CEEA),
                                            fontSize = 10.2.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Compact 3D Crimson Red Sculpted Close Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = RoseNeon)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                RoseNeon.copy(alpha = 0.85f),
                                                Color(0xFF9E1136),
                                                Color(0xFF52071B)
                                            )
                                        )
                                    )
                                    .border(
                                        1.2.dp,
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.85f), RoseNeon)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onClose() }
                                    .testTag("modal_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Compact Multi-Page 3D Sculpted Navigation Tabs
                        if (tabs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tabs.forEachIndexed { index, (tabId, tabLabel) ->
                                    val isSelected = selectedTab == tabId
                                    val tabColor = Tab3DPalette[index % Tab3DPalette.size]
                                    val tabShape = RoundedCornerShape(10.dp)
                                    Box(
                                        modifier = Modifier
                                            .shadow(
                                                elevation = if (isSelected) 6.dp else 1.dp,
                                                shape = tabShape,
                                                spotColor = tabColor
                                            )
                                            .clip(tabShape)
                                            .background(
                                                if (isSelected) {
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            tabColor,
                                                            tabColor.copy(alpha = 0.78f),
                                                            Color(0xFF0A1324)
                                                        )
                                                    )
                                                } else {
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color(0xFF1A2942),
                                                            Color(0xFF0D1628),
                                                            Color(0xFF070C18)
                                                        )
                                                    )
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.4.dp else 1.dp,
                                                brush = if (isSelected) {
                                                    Brush.verticalGradient(
                                                        listOf(Color.White, tabColor)
                                                    )
                                                } else {
                                                    Brush.verticalGradient(
                                                        listOf(tabColor.copy(alpha = 0.65f), Color(0xFF16243B))
                                                    )
                                                },
                                                shape = tabShape
                                            )
                                            .clickable { onSelectTab(tabId) }
                                            .padding(horizontal = 11.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tabLabel,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Main Modal Body Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Compact 3D Sculpted Multi-Color Section Card
 */
@Composable
fun PremiumSectionCard(
    title: String,
    subtitle: String = "",
    icon: ImageVector? = null,
    accentColor: Color = SapphireBlue,
    badgeText: String = "",
    modifier: Modifier = Modifier,
    headerTrailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = cardShape, spotColor = accentColor)
            .clip(cardShape)
            .background(accentColor.copy(alpha = 0.38f))
            .padding(bottom = 2.dp)
    ) {
        Surface(
            shape = cardShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        accentColor.copy(alpha = 0.80f),
                        accentColor.copy(alpha = 0.28f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.14f),
                                Color(0xFF0E1829),
                                Color(0xFF070D18)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = accentColor)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                accentColor,
                                                accentColor.copy(alpha = 0.65f),
                                                Color(0xFF0A1426)
                                            )
                                        )
                                    )
                                    .border(
                                        1.1.dp,
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.80f), accentColor)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(9.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = Color(0xFFB8CCE6),
                                    fontSize = 10.2.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (headerTrailing != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        headerTrailing()
                    } else if (badgeText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = accentColor.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.65f), accentColor))
                            )
                        ) {
                            Text(
                                text = badgeText,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

/**
 * Compact 3D Sculpted Multi-Color Button
 */
@Composable
fun PremiumActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryColor: Color = SapphireBlue,
    secondaryColor: Color = RoseNeon,
    isOutlined: Boolean = false,
    testTag: String = ""
) {
    val shape = RoundedCornerShape(12.dp)
    val baseMod = if (testTag.isNotBlank()) modifier.testTag(testTag) else modifier

    if (isOutlined) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = baseMod
                .height(38.dp)
                .shadow(3.dp, shape, spotColor = primaryColor)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f),
                            Color(0xFF121E34),
                            Color(0xFF09101E)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            primaryColor,
                            secondaryColor.copy(alpha = 0.80f)
                        )
                    ),
                    shape = shape
                )
                .clickable { onClick() }
                .padding(horizontal = 11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        Box(
            modifier = baseMod
                .height(39.dp)
                .shadow(5.dp, shape, spotColor = primaryColor)
                .clip(shape)
                .background(Color(0xFF040812))
                .padding(bottom = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.85f),
                                secondaryColor.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .border(
                        width = 1.3.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.90f),
                                primaryColor.copy(alpha = 0.60f)
                            )
                        ),
                        shape = shape
                    )
                    .clickable { onClick() }
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 3D Sculpted Multi-Color Hardware Control Tile:
 * Uses its passed `activeColor` (Red, Blue, Emerald, Violet, Cyan, Magenta) with a 3D raised bevel.
 */
@Composable
fun PremiumControlCenterTile(
    title: String,
    statusLabel: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color = SapphireBlue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val bgBrush = if (isActive) {
        Brush.verticalGradient(
            colors = listOf(
                activeColor.copy(alpha = 0.34f),
                Color(0xFF121E36),
                Color(0xFF09101E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF152238),
                Color(0xFF0A1220)
            )
        )
    }

    val borderColor by animateColorAsState(
        targetValue = if (isActive) activeColor else ExecutiveBorder,
        label = "tile_border"
    )

    val tileShape = RoundedCornerShape(17.dp)
    val taggedMod = if (testTag.isNotBlank()) modifier.testTag(testTag) else modifier

    Box(
        modifier = taggedMod
            .shadow(if (isActive) 8.dp else 3.dp, tileShape, spotColor = activeColor)
            .clip(tileShape)
            .background(if (isActive) activeColor.copy(alpha = 0.55f) else Color(0xFF050912))
            .padding(bottom = 2.5.dp)
            .clip(tileShape)
            .background(bgBrush)
            .border(
                width = if (isActive) 1.6.dp else 1.1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isActive) Color.White.copy(alpha = 0.75f) else activeColor.copy(alpha = 0.45f),
                        borderColor
                    )
                ),
                shape = tileShape
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (isActive) {
                                Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.65f)))
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        activeColor.copy(alpha = 0.22f),
                                        Color(0xFF0F192B)
                                    )
                                )
                            }
                        )
                        .border(
                            1.1.dp,
                            if (isActive) Color.White.copy(alpha = 0.85f) else activeColor.copy(alpha = 0.55f),
                            RoundedCornerShape(11.dp)
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) Color.White else activeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusLabel,
                        color = if (isActive) Color.White else Color(0xFFA2B9D6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }
            }

            // Glowing 3D Status LED
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else Color(0xFF283A54))
                    .border(1.dp, if (isActive) Color.White else Color.Transparent, CircleShape)
            )
        }
    }
}

/**
 * 3D Multi-Color Studio Equalizer / Volume Channel Row using its passed `accentColor` (Red, Blue, Emerald, Violet, Cyan).
 */
@Composable
fun PremiumStudioVolumeChannel(
    label: String,
    percent: Int,
    accentColor: Color = SapphireBlue,
    onPercentChange: (Int) -> Unit,
    sliderTestTag: String
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.3.dp,
            Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.75f), ExecutiveBorder))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            Color(0xFF101B2E),
                            Color(0xFF09101C)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // -15% 3D Crimson Red Button
                    Surface(
                        color = RoseNeon.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RoseNeon.copy(alpha = 0.80f)),
                        modifier = Modifier.clickable {
                            onPercentChange((percent - 15).coerceAtLeast(0))
                        }
                    ) {
                        Text(
                            text = "−15%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Percentage Badge
                    Surface(
                        color = accentColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.1.dp, accentColor)
                    ) {
                        Text(
                            text = "$percent%",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // +15% 3D Emerald/Sapphire Button
                    Surface(
                        color = MintTurquoise.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MintTurquoise.copy(alpha = 0.85f)),
                        modifier = Modifier.clickable {
                            onPercentChange((percent + 15).coerceAtMost(100))
                        }
                    ) {
                        Text(
                            text = "+15%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multi-Color 3D Studio LED Bar (Blue -> Emerald -> Crimson Peak)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                val segments = 24
                val gap = 3.dp.toPx()
                val segWidth = (size.width - gap * (segments - 1)) / segments
                val activeSegments = ((percent / 100f) * segments).roundToInt()
                for (i in 0 until segments) {
                    val segColor = when {
                        i >= activeSegments -> Color(0xFF1A283E)
                        i > segments * 0.80f -> RoseNeon
                        i > segments * 0.50f -> accentColor
                        else -> TitaniumCyan
                    }
                    drawRoundRect(
                        color = segColor,
                        topLeft = Offset(i * (segWidth + gap), 0f),
                        size = Size(segWidth, size.height),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            Slider(
                value = percent.toFloat(),
                onValueChange = { onPercentChange(it.roundToInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0xFF1B2A40)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag(sliderTestTag)
            )
        }
    }
}

/**
 * 3D Sculpted Voice Studio Card:
 * Female voices glow with 3D Crimson/Magenta (`RoseNeon`), Male voices glow with 3D Electric Blue (`SapphireBlue`).
 */
@Composable
fun PremiumVoiceStudioTile(
    titleBn: String,
    subtitleBn: String,
    voiceCode: String,
    isFemale: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val baseVoiceColor = if (isFemale) RoseNeon else SapphireBlue
    val accent = if (isSelected) MintTurquoise else baseVoiceColor
    val cardShape = RoundedCornerShape(16.dp)
    val taggedMod = if (testTag.isNotBlank()) modifier.testTag(testTag) else modifier

    Box(
        modifier = taggedMod
            .shadow(if (isSelected) 8.dp else 3.dp, cardShape, spotColor = accent)
            .clip(cardShape)
            .background(if (isSelected) accent.copy(alpha = 0.6f) else baseVoiceColor.copy(alpha = 0.28f))
            .padding(bottom = 2.5.dp)
    ) {
        Surface(
            shape = cardShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = if (isSelected) 1.8.dp else 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isSelected) 0.85f else 0.35f),
                        accent
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.28f),
                                    Color(0xFF101E33),
                                    Color(0xFF08101D)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    baseVoiceColor.copy(alpha = 0.14f),
                                    Color(0xFF111C30),
                                    Color(0xFF09101C)
                                )
                            )
                        }
                    )
                    .padding(11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = baseVoiceColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, baseVoiceColor.copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = if (isFemale) "👩 FEMALE • $voiceCode" else "🎙️ MALE • $voiceCode",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MintTurquoise,
                            modifier = Modifier.size(17.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = baseVoiceColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = titleBn,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitleBn,
                    color = Color(0xFFC2D4EC),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3D Sculpted Permission Status Row:
 * Emerald Green (`MintTurquoise`) when granted, Crimson Red (`RoseNeon`) when action is needed.
 */
@Composable
fun PremiumPermissionStatusRow(
    stepNumber: String,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonTextGranted: String = "সক্রিয় ✓",
    buttonTextDenied: String = "অন করুন",
    accentColor: Color = SapphireBlue,
    onClick: () -> Unit,
    buttonTestTag: String = ""
) {
    val rowShape = RoundedCornerShape(16.dp)
    val statusColor = if (isGranted) MintTurquoise else RoseNeon

    Surface(
        color = Color(0xFF0D1729),
        shape = rowShape,
        border = BorderStroke(
            1.3.dp,
            Brush.horizontalGradient(
                listOf(statusColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.55f))
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(statusColor.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.22f))
                        .border(1.3.dp, statusColor, CircleShape)
                ) {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = Color(0xFFC2D4EB),
                        fontSize = 10.5.sp,
                        lineHeight = 13.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            PremiumActionButton(
                text = if (isGranted) buttonTextGranted else buttonTextDenied,
                onClick = onClick,
                primaryColor = statusColor,
                secondaryColor = accentColor,
                isOutlined = isGranted,
                testTag = buttonTestTag,
                modifier = Modifier.width(106.dp)
            )
        }
    }
}
