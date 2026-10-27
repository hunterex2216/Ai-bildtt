package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AgentAbilitySwitchesBus
import com.example.service.FloatingAvatarCharacter
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/**
 * Page 1 of Main Settings ("🤖 এআই অ্যাবিলিটি"):
 * 3D Sculpted Multi-Color 7-Switch Ability Permissions Card (Crimson Red, Sapphire Blue,
 * Emerald Green, Royal Violet, Cyber Cyan, Hyper Magenta — NO gold/yellow).
 */
@Composable
fun AbilityPermissionSwitchesCard(
    geminiApiKey: String,
    modifier: Modifier = Modifier,
    onResultAnnouncement: (String) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AgentAbilitySwitchesBus.ensureInitialized(context)
    }
    val abilityState by AgentAbilitySwitchesBus.state.collectAsState()
    var showAvatarCharacterPicker by rememberSaveable { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, cardShape, spotColor = SapphireBlue)
            .clip(cardShape)
            .background(SapphireBlue.copy(alpha = 0.40f))
            .padding(bottom = 2.dp)
            .testTag("ability_permission_switches_card")
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            color = Color(0xFF061020),
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        RoseNeon,
                        SapphireBlue,
                        MintTurquoise,
                        RoyalViolet
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SapphireBlue.copy(alpha = 0.14f),
                                Color(0xFF091426),
                                Color(0xFF050B16)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Inner 3D container holding the 7 rows
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFF091528),
                    border = BorderStroke(
                        1.1.dp,
                        Brush.verticalGradient(
                            listOf(
                                SapphireBlue.copy(alpha = 0.65f),
                                RoseNeon.copy(alpha = 0.45f),
                                MintTurquoise.copy(alpha = 0.55f)
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Silent execution (3D Crimson Red)
                        AbilitySwitchRowItem(
                            icon = Icons.Default.VolumeOff,
                            iconBadgeBg = RoseNeon,
                            title = "Silent execution",
                            subtitle = if (abilityState.silentExecution) {
                                "On - চুপচাপ কাজ শেষ করে শেষে রিপোর্ট দেবে"
                            } else {
                                "Off - প্রতিটি ধাপ মুখে বলে কাজ করবে"
                            },
                            checked = abilityState.silentExecution,
                            onCheckedChange = { enabled ->
                                AgentAbilitySwitchesBus.setSilentExecution(context, enabled)
                                onResultAnnouncement(
                                    if (enabled) "Silent execution ON" else "Silent execution OFF"
                                )
                            },
                            testTag = "switch_silent_execution"
                        )

                        AbilityRowDivider()

                        // 2. Background web search (3D Sapphire Blue)
                        AbilitySwitchRowItem(
                            icon = Icons.Default.Search,
                            iconBadgeBg = SapphireBlue,
                            title = "Background web search",
                            subtitle = if (abilityState.backgroundWebSearch) {
                                "On - ব্রাউজার না খুলেই ব্যাকগ্রাউন্ডে তথ্য ও ম্যাপস খুঁজবে"
                            } else {
                                "Off - ব্যাকগ্রাউন্ড ওয়েব সার্চ বন্ধ"
                            },
                            checked = abilityState.backgroundWebSearch,
                            onCheckedChange = { enabled ->
                                AgentAbilitySwitchesBus.setBackgroundWebSearch(context, enabled)
                                onResultAnnouncement(
                                    if (enabled) "Background web search ON" else "Background web search OFF"
                                )
                            },
                            testTag = "switch_background_web_search"
                        )

                        AbilityRowDivider()

                        // 3. Make images with Gemini (3D Royal Violet)
                        AbilitySwitchRowItem(
                            icon = Icons.Default.Brush,
                            iconBadgeBg = RoyalViolet,
                            title = "Make images with Gemini",
                            subtitle = if (abilityState.makeImagesWithGemini) {
                                "On - ছবি তৈরি ও এডিট চালু (Download/archer/)"
                            } else {
                                "Off - ছবি তৈরি বন্ধ"
                            },
                            checked = abilityState.makeImagesWithGemini,
                            onCheckedChange = { enabled ->
                                AgentAbilitySwitchesBus.setMakeImagesWithGemini(context, enabled)
                                onResultAnnouncement(
                                    if (enabled) "Make images with Gemini ON" else "Make images with Gemini OFF"
                                )
                            },
                            testTag = "switch_make_images_gemini"
                        )

                        AbilityRowDivider()

                        // 4. Ignore background audio / Noise Cancellation (3D Emerald Green)
                        AbilitySwitchRowItem(
                            icon = Icons.Default.GraphicEq,
                            iconBadgeBg = MintTurquoise,
                            title = "Ignore background audio (Noise Cancel)",
                            subtitle = if (abilityState.ignoreBackgroundAudio) {
                                "On - নয়েজ ক্যান্সেলেশন সক্রিয় (শুধু আপনার কথা শুনবে, ব্যাকগ্রাউন্ডের কথা ইগনোর করবে)"
                            } else {
                                "Off - আশেপাশের সব কথা ও শব্দ শুনবে"
                            },
                            checked = abilityState.ignoreBackgroundAudio,
                            onCheckedChange = { enabled ->
                                AgentAbilitySwitchesBus.setIgnoreBackgroundAudio(context, enabled)
                                onResultAnnouncement(
                                    if (enabled) "Noise cancellation & background audio ignore ON" else "Ignore background audio OFF"
                                )
                            },
                            testTag = "switch_ignore_background_audio"
                        )

                        AbilityRowDivider()

                        // 5. Show tap indicator (3D Cyber Cyan)
                        AbilitySwitchRowItem(
                            icon = Icons.Default.TouchApp,
                            iconBadgeBg = TitaniumCyan,
                            title = "Show tap indicator",
                            subtitle = if (abilityState.showTapIndicator) {
                                "On - shows a red bullseye where ARCHER taps"
                            } else {
                                "Off - taps are invisible"
                            },
                            checked = abilityState.showTapIndicator,
                            onCheckedChange = { enabled ->
                                AgentAbilitySwitchesBus.setShowTapIndicator(context, enabled)
                                onResultAnnouncement(
                                    if (enabled) "Show tap indicator ON (red bullseye active)" else "Show tap indicator OFF"
                                )
                            },
                            testTag = "switch_show_tap_indicator"
                        )

                        AbilityRowDivider()

                        // 6. Send crash reports (3D Hyper Magenta)
                        AbilitySwitchRowItem(
                            icon = Icons.Default.BugReport,
                            iconBadgeBg = ElectricMagenta,
                            title = "Send crash reports",
                            subtitle = if (abilityState.sendCrashReports) {
                                "On - automatically sends crash and failure reports (${abilityState.crashReportsCount} logged)"
                            } else {
                                "Off - crash and failure reporting is disabled"
                            },
                            checked = abilityState.sendCrashReports,
                            onCheckedChange = { enabled ->
                                AgentAbilitySwitchesBus.setSendCrashReports(context, enabled)
                                onResultAnnouncement(
                                    if (enabled) "Send crash reports ON" else "Send crash reports OFF"
                                )
                            },
                            testTag = "switch_send_crash_reports"
                        )

                        AbilityRowDivider()

                        // 7. Floating avatar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAvatarCharacterPicker = !showAvatarCharacterPicker
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("row_floating_avatar"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = RoyalViolet)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(RoyalViolet, Color(0xFF4A148C))
                                            )
                                        )
                                        .border(1.1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Floating avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Floating avatar",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "স্ক্রিনের উপর ভাসমান ক্যারেক্টার",
                                        color = Color(0xFFB4CBE4),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = if (abilityState.floatingAvatar == FloatingAvatarCharacter.OFF) {
                                    Color(0xFF172842)
                                } else {
                                    SapphireBlue
                                },
                                border = BorderStroke(
                                    1.1.dp,
                                    if (abilityState.floatingAvatar == FloatingAvatarCharacter.OFF) {
                                        SapphireBlue.copy(alpha = 0.65f)
                                    } else {
                                        Color.White
                                    }
                                ),
                                modifier = Modifier
                                    .clickable {
                                        val next = AgentAbilitySwitchesBus.cycleFloatingAvatar(context)
                                        showAvatarCharacterPicker = true
                                        onResultAnnouncement("Floating avatar: ${next.displayName}")
                                    }
                                    .testTag("floating_avatar_value_pill")
                            ) {
                                Text(
                                    text = abilityState.floatingAvatar.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Character Picker Row when Floating Avatar is tapped
                        AnimatedVisibility(visible = showAvatarCharacterPicker) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF071120))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FloatingAvatarCharacter.entries.forEach { character ->
                                        val selected = abilityState.floatingAvatar == character
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) RoseNeon else Color(0xFF102038),
                                            border = BorderStroke(
                                                1.1.dp,
                                                if (selected) Color.White else SapphireBlue.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    AgentAbilitySwitchesBus.setFloatingAvatar(context, character)
                                                    onResultAnnouncement("Floating avatar set to ${character.displayName}")
                                                }
                                                .testTag("pick_avatar_${character.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(text = character.badgeEmoji, fontSize = 14.sp)
                                                Text(
                                                    text = character.displayName,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dynamic Task Output & Compact On-Demand Bar (Only expands when a task is running or selected)
            ArcherAiStudioFeaturesCard(
                geminiApiKey = geminiApiKey,
                onResultAnnouncement = onResultAnnouncement
            )
        }
    }
}

/**
 * Dynamic Task-Driven Studio Output & Compact On-Demand Tool Bar:
 * - Stays ultra-compact when idle (no giant Maps, Bioscience, or Music screens taking up space).
 * - Automatically expands ONLY when a task is actively running (`isWorkingOnStudioTask`) or
 *   when a generated Image, Maps/Web Search result, or Music track is ready.
 * - Optional 1-tap compact chips let the user open a single tool on demand and close it anytime.
 */
@Composable
fun ArcherAiStudioFeaturesCard(
    geminiApiKey: String,
    modifier: Modifier = Modifier,
    onResultAnnouncement: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        AgentAbilitySwitchesBus.ensureInitialized(context)
    }
    val abilityState by AgentAbilitySwitchesBus.state.collectAsState()

    var activeQuickTool by rememberSaveable { mutableStateOf("NONE") } // "NONE", "IMAGE", "MAPS", "MUSIC"
    var imagePromptDraft by rememberSaveable { mutableStateOf("") }
    var searchQueryDraft by rememberSaveable { mutableStateOf("") }
    var musicPromptDraft by rememberSaveable { mutableStateOf("") }
    var statusBannerText by rememberSaveable { mutableStateOf("") }

    val hasImageOutput = abilityState.lastGeneratedImagePath.isNotBlank()
    val hasSearchOutput = abilityState.lastBackgroundSearchSummary.isNotBlank()
    val hasMusicOutput = abilityState.lastGeneratedMusicPath.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("archer_ai_studio_features_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact 1-line on-demand tool selector chips (only expands if tapped)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple("IMAGE", "🎨 ছবি তৈরি", RoseNeon),
                Triple("MAPS", "📍 ম্যাপস ও সার্চ", SapphireBlue),
                Triple("MUSIC", "🎵 মিউজিক", RoyalViolet)
            ).forEach { (toolKey, label, accent) ->
                val isSelected = activeQuickTool == toolKey
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accent.copy(alpha = 0.28f) else Color(0xFF0A1628),
                    border = BorderStroke(
                        1.1.dp,
                        if (isSelected) Color.White else accent.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (toolKey == "IMAGE") {
                                AgentAbilitySwitchesBus.openDedicatedImageStudioPage(imagePromptDraft)
                            } else {
                                activeQuickTool = if (isSelected) "NONE" else toolKey
                            }
                        }
                ) {
                    Text(
                        text = if (isSelected) "$label ✕" else label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Live working status banner (only shown when actively working or when a status message is active)
        if (abilityState.isWorkingOnStudioTask || statusBannerText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0A1628),
                border = BorderStroke(
                    1.2.dp,
                    if (statusBannerText.startsWith("ABILITY_SWITCH_OFF")) RoseNeon else MintTurquoise
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (abilityState.isWorkingOnStudioTask) {
                            "⏳ এআই কাজ করছে... অনুগ্রহ করে অপেক্ষা করুন"
                        } else {
                            statusBannerText
                        },
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!abilityState.isWorkingOnStudioTask && statusBannerText.isNotBlank()) {
                        Text(
                            text = "✕",
                            color = RoseNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { statusBannerText = "" }
                        )
                    }
                }
            }
        }

        // 1. IMAGE TOOL / OUTPUT — Only visible when user tapped "🎨 ছবি তৈরি" OR when an image was generated
        AnimatedVisibility(visible = activeQuickTool == "IMAGE" || hasImageOutput) {
            PremiumSectionCard(
                title = "🎨 ছবি তৈরি ও এডিট (Gemini Banana 2.1)",
                subtitle = "যখন কাজ দরকার শুধু তখনই ওপেন থাকে (Download/archer/)",
                icon = Icons.Default.AutoFixHigh,
                accentColor = RoseNeon,
                badgeText = if (abilityState.makeImagesWithGemini) "READY" else "OFF"
            ) {
                if (activeQuickTool == "IMAGE") {
                    OutlinedTextField(
                        value = imagePromptDraft,
                        onValueChange = { imagePromptDraft = it },
                        placeholder = {
                            Text(
                                text = "যেমন: A futuristic 3D neon sports car...",
                                color = Color(0xFF8AAEC9),
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        colors = darkStudioTextFieldColors(RoseNeon),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_gemini_image_prompt")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumActionButton(
                            text = if (abilityState.isWorkingOnStudioTask) "তৈরি হচ্ছে..." else "🎨 ছবি তৈরি",
                            onClick = {
                                val q = imagePromptDraft.trim().ifBlank { "Futuristic 3D neon cyber cityscape at night" }
                                scope.launch {
                                    val res = AgentAbilitySwitchesBus.createImageWithGemini(
                                        context = context,
                                        apiKey = geminiApiKey,
                                        prompt = q,
                                        editExistingImage = false
                                    )
                                    statusBannerText = res
                                    onResultAnnouncement(res)
                                }
                            },
                            primaryColor = RoseNeon,
                            secondaryColor = SapphireBlue,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_create_image_banana"
                        )

                        PremiumActionButton(
                            text = "✨ এডিট করুন",
                            onClick = {
                                val q = imagePromptDraft.trim().ifBlank { "Add glowing neon red and blue 3D lighting" }
                                scope.launch {
                                    val res = AgentAbilitySwitchesBus.createImageWithGemini(
                                        context = context,
                                        apiKey = geminiApiKey,
                                        prompt = q,
                                        editExistingImage = true
                                    )
                                    statusBannerText = res
                                    onResultAnnouncement(res)
                                }
                            },
                            primaryColor = SapphireBlue,
                            secondaryColor = RoyalViolet,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_edit_image_banana"
                        )
                    }
                }

                if (hasImageOutput) {
                    val imgFile = File(abilityState.lastGeneratedImagePath)
                    val previewBitmap = remember(abilityState.lastGeneratedImagePath) {
                        if (imgFile.exists()) BitmapFactory.decodeFile(imgFile.absolutePath) else null
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (abilityState.isImageSavedToArcher) {
                            "✅ Saved: ${abilityState.savedArcherImagePath.substringAfterLast('/')}"
                        } else {
                            "❓ এই কাজটা হয়ে গেছে — ছবিটা কি 'archer' ফোল্ডারে সেভ করব?"
                        },
                        color = if (abilityState.isImageSavedToArcher) MintTurquoise else Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (previewBitmap != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Generated or edited Gemini image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.4.dp, RoseNeon, RoundedCornerShape(14.dp))
                                .clickable {
                                    AgentAbilitySwitchesBus.openDedicatedImageStudioPage()
                                }
                        )
                    }
                    if (abilityState.pendingImageSaveConfirmation && !abilityState.isImageSavedToArcher) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumActionButton(
                                text = "💾 হ্যাঁ, সেভ করুন",
                                onClick = {
                                    val msg = AgentAbilitySwitchesBus.confirmSavePendingImageToArcherFolder(context)
                                    statusBannerText = msg
                                    onResultAnnouncement(msg)
                                },
                                primaryColor = MintTurquoise,
                                secondaryColor = SapphireBlue,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumActionButton(
                                text = "🗑️ না, বাদ দিন",
                                onClick = {
                                    AgentAbilitySwitchesBus.discardPendingImage()
                                    statusBannerText = "ছবিটি কোনো ফোল্ডারে সেভ করা হয়নি।"
                                },
                                primaryColor = RoseNeon,
                                secondaryColor = RoyalViolet,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. MAPS / SEARCH TOOL — Only visible when user tapped "📍 ম্যাপস ও সার্চ" OR when a search result is active
        AnimatedVisibility(visible = activeQuickTool == "MAPS" || hasSearchOutput) {
            PremiumSectionCard(
                title = "📍 ব্যাকগ্রাউন্ড ওয়েব ও ম্যাপস সার্চ",
                subtitle = "ব্রাউজার না খুলেই লোকেশন, দূরত্ব ও তথ্য",
                icon = Icons.Default.LocationOn,
                accentColor = SapphireBlue,
                badgeText = "LIVE MAPS"
            ) {
                if (activeQuickTool == "MAPS") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQueryDraft,
                            onValueChange = { searchQueryDraft = it },
                            placeholder = {
                                Text(
                                    text = "লোকেশন বা তথ্য খুঁজুন...",
                                    color = Color(0xFF8AAEC9),
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            colors = darkStudioTextFieldColors(SapphireBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_background_web_search")
                        )
                        PremiumActionButton(
                            text = "সার্চ",
                            onClick = {
                                val q = searchQueryDraft.trim().ifBlank { "Top hospitals in Dhaka" }
                                scope.launch {
                                    val res = AgentAbilitySwitchesBus.searchWebOrMapsInBackground(
                                        context = context,
                                        apiKey = geminiApiKey,
                                        query = q,
                                        useMapsGrounding = true
                                    )
                                    statusBannerText = res
                                    onResultAnnouncement(res)
                                }
                            },
                            primaryColor = SapphireBlue,
                            secondaryColor = TitaniumCyan,
                            testTag = "btn_run_background_web_search"
                        )
                    }
                }

                if (hasSearchOutput) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF071120),
                        border = BorderStroke(1.dp, SapphireBlue.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = abilityState.lastBackgroundSearchSummary,
                            color = Color(0xFFE5F2FF),
                            fontSize = 11.2.sp,
                            lineHeight = 15.5.sp,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                }
            }
        }

        // 3. MUSIC TOOL — Only visible when user tapped "🎵 মিউজিক" OR when a music track is ready
        AnimatedVisibility(visible = activeQuickTool == "MUSIC" || hasMusicOutput) {
            PremiumSectionCard(
                title = "🎵 এআই মিউজিক জেনারেটর (Lyria 3)",
                subtitle = "অরিজিনাল মিউজিক ট্র্যাক তৈরি ও প্লে",
                icon = Icons.Default.MusicNote,
                accentColor = RoyalViolet,
                badgeText = "LYRIA 3"
            ) {
                if (activeQuickTool == "MUSIC") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = musicPromptDraft,
                            onValueChange = { musicPromptDraft = it },
                            placeholder = {
                                Text(
                                    text = "যেমন: Cyberpunk synthwave beat...",
                                    color = Color(0xFF8AAEC9),
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            colors = darkStudioTextFieldColors(RoyalViolet),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_lyria_music_prompt")
                        )
                        PremiumActionButton(
                            text = "মিউজিক তৈরি",
                            onClick = {
                                val q = musicPromptDraft.trim().ifBlank { "Futuristic ambient synthwave melody" }
                                scope.launch {
                                    val res = AgentAbilitySwitchesBus.generateMusicTrackWithLyria(
                                        context = context,
                                        apiKey = geminiApiKey,
                                        prompt = q,
                                        fullLengthSong = false
                                    )
                                    statusBannerText = res
                                    onResultAnnouncement(res)
                                }
                            },
                            primaryColor = RoyalViolet,
                            secondaryColor = RoseNeon,
                            testTag = "btn_generate_lyria_music"
                        )
                    }
                }

                if (hasMusicOutput) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎧 ${abilityState.lastGeneratedMusicPath.substringAfterLast('/')}",
                            color = MintTurquoise,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                AgentAbilitySwitchesBus.togglePlayLastGeneratedMusic()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (abilityState.isPlayingGeneratedMusic) RoseNeon else MintTurquoise,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_play_stop_generated_music")
                        ) {
                            Icon(
                                imageVector = if (abilityState.isPlayingGeneratedMusic) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Play or Stop Generated Music",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (abilityState.isPlayingGeneratedMusic) "Stop" else "Play",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (abilityState.pendingMusicSaveConfirmation && !abilityState.isMusicSavedToArcher) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "❓ মিউজিক কাজটা হয়ে গেছে — এটা কি 'archer' ফোল্ডারে সেভ করব?",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumActionButton(
                                text = "💾 হ্যাঁ, সেভ করুন",
                                onClick = {
                                    val msg = AgentAbilitySwitchesBus.confirmSavePendingMusicToArcherFolder(context)
                                    statusBannerText = msg
                                    onResultAnnouncement(msg)
                                },
                                primaryColor = MintTurquoise,
                                secondaryColor = SapphireBlue,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumActionButton(
                                text = "🗑️ না, বাদ দিন",
                                onClick = {
                                    AgentAbilitySwitchesBus.discardPendingMusic()
                                    statusBannerText = "মিউজিকটি কোনো ফোল্ডারে সেভ করা হয়নি।"
                                },
                                primaryColor = RoseNeon,
                                secondaryColor = RoyalViolet,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun darkStudioTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF071324),
    unfocusedContainerColor = Color(0xFF071324),
    focusedBorderColor = accent,
    unfocusedBorderColor = accent.copy(alpha = 0.45f),
    cursorColor = accent
)

@Composable
private fun AbilitySwitchRowItem(
    icon: ImageVector,
    iconBadgeBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    iconTint: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // Compact 3D Sculpted Icon Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = iconBadgeBg)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                iconBadgeBg,
                                iconBadgeBg.copy(alpha = 0.65f),
                                Color(0xFF091326)
                            )
                        )
                    )
                    .border(
                        1.1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.85f), iconBadgeBg)
                        ),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFB4CBE4),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconBadgeBg,
                checkedBorderColor = Color.White.copy(alpha = 0.6f),
                uncheckedThumbColor = Color(0xFF90A4AE),
                uncheckedTrackColor = Color(0xFF14243A),
                uncheckedBorderColor = Color(0xFF2B4C66)
            )
        )
    }
}

@Composable
private fun AbilityRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 12.dp),
        thickness = 0.7.dp,
        color = Color(0xFF1A3150)
    )
}

/**
 * Draggable in-app floating character widget when `Floating avatar` is enabled (`ARCHER`, `JARVIS`, `AURA`, `NOVA`).
 */
@Composable
fun InAppFloatingCharacterBubble(
    isSpeaking: Boolean,
    onTapBubble: () -> Unit,
    modifier: Modifier = Modifier
) {
    val abilityState by AgentAbilitySwitchesBus.state.collectAsState()
    val character = abilityState.floatingAvatar
    if (character == FloatingAvatarCharacter.OFF) return

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val pulseTransition = rememberInfiniteTransition(label = "avatarPulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking) 1.14f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .scale(ringScale)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xEE081624))
            .border(2.dp, Color(character.accentHex), RoundedCornerShape(24.dp))
            .clickable { onTapBubble() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("in_app_floating_avatar_bubble")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = character.badgeEmoji, fontSize = 20.sp)
            Column {
                Text(
                    text = character.displayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isSpeaking) "Speaking..." else "Listening",
                    color = Color(character.accentHex),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
