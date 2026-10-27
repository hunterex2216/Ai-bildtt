package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AgentAbilitySwitchesBus
import com.example.service.CodingSessionBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full-screen Argon 4 page: code is typed live, then asks the user for confirmation before saving to `archer/`.
 */
@Composable
fun LiveCodingOverlay() {
    val session by CodingSessionBus.session.collectAsState()
    val s = session ?: return

    val green = Color(0xFF00FF9C)
    val total = s.code.length
    var shown by remember(s.id) { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    val typingDone = shown >= total

    LaunchedEffect(s.id) {
        val step = (total / 350).coerceAtLeast(4)
        var frame = 0
        while (shown < total) {
            shown = (shown + step).coerceAtMost(total)
            frame++
            if (frame % 4 == 0) scroll.scrollTo(scroll.maxValue)
            delay(16)
        }
        scroll.scrollTo(scroll.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030A07))
            .safeDrawingPadding()
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ARGON 4 • লাইভ কোডিং", color = green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(s.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(
                "✕",
                color = Color(0xFFFF4D6A),
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable { CodingSessionBus.dismiss() }
                    .padding(8.dp)
            )
        }
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(s.fileName, green)
            if (s.language.isNotBlank()) Chip(s.language, Color(0xFF00E5FF))
            Chip("${s.code.take(shown).count { it == '\n' } + 1} লাইন", Color(0xFF8FA89B))
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else shown.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = green,
            trackColor = Color(0xFF12281D)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, green.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(Color(0xFF06120C))
        ) {
            Text(
                text = s.code.take(shown) + if (!typingDone) "▌" else "",
                color = Color(0xFFB8FFE0),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .verticalScroll(scroll)
                    .padding(10.dp)
            )
        }
        if (typingDone) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(green.copy(alpha = 0.12f))
                    .border(1.dp, green, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (!s.isSaved) {
                    Text(
                        "❓ এই কাজটা হয়ে গেছে — এটা কি 'archer' ফোল্ডারে সেভ করব?",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "আপনি ভয়েসে 'হ্যাঁ সেভ করো' বলুন অথবা নিচের বাটনে ট্যাপ করুন।",
                        color = Color(0xFFB8FFE0),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(green)
                                .clickable { CodingSessionBus.requestConfirmSave() }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "💾 হ্যাঁ, সেভ করুন",
                                color = Color(0xFF03140B),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2B1218))
                                .border(1.dp, Color(0xFFFF4D6A), RoundedCornerShape(10.dp))
                                .clickable { CodingSessionBus.requestDiscard() }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🗑️ না, বাদ দিন",
                                color = Color(0xFFFF8A9E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Text(
                        "✔ আপনার অনুমতিতে 'archer' ফোল্ডারে সেভ হয়েছে",
                        color = green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (s.savedPath.isNotBlank()) {
                        Text(s.savedPath, color = Color(0xFFB8FFE0), fontSize = 11.sp)
                    }
                    Text(
                        "বন্ধ করুন",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { CodingSessionBus.dismiss() }
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Full-Screen Image Generation Studio Page:
 * Automatically opens whenever image generation is triggered from the main screen (voice or text)
 * or from the UI. Shows live generation progress, displays the real prompt-matched image,
 * and asks the user for explicit confirmation before saving to the `archer` folder.
 */
@Composable
fun DedicatedImageStudioOverlay(
    apiKey: String = "",
    onStatusSpoken: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by AgentAbilitySwitchesBus.state.collectAsState()

    if (!state.showDedicatedImageStudioPage) return

    BackHandler {
        AgentAbilitySwitchesBus.closeDedicatedImageStudioPage()
    }

    var promptInput by remember(state.activeImagePrompt) {
        mutableStateOf(state.activeImagePrompt)
    }

    val decodedBitmap = remember(state.lastGeneratedImagePath) {
        val path = state.lastGeneratedImagePath
        if (path.isNotBlank() && File(path).exists()) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
    }

    val accentPink = Color(0xFFF43F5E)
    val accentCyan = Color(0xFF00E5FF)
    val accentGreen = Color(0xFF00FF9C)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060913),
                        Color(0xFF0B1022),
                        Color(0xFF05070E)
                    )
                )
            )
            .safeDrawingPadding()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
            .testTag("dedicated_image_studio_page")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentPink.copy(alpha = 0.18f))
                        .border(1.dp, accentPink.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Image Studio",
                        tint = accentPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "ARCHER REAL IMAGE STUDIO",
                        color = accentPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "রিয়েল এআই ছবি তৈরির আলাদা পেইজ",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFF1E1324),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { AgentAbilitySwitchesBus.closeDedicatedImageStudioPage() }
                    .testTag("btn_close_dedicated_image_page")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Image Studio",
                        tint = Color(0xFFFF6B8B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0E1629))
                .border(1.dp, accentPink.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "আপনার ছবির বর্ণনা (বাংলা বা ইংরেজিতে লিখুন বা মেইন স্ক্রিন থেকে ভয়েসে বলুন):",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = {
                    Text(
                        "যেমন: বৃষ্টির দিনে চায়ের দোকানে বসে চা খাওয়ার রিয়েল ছবি...",
                        color = Color.White.copy(alpha = 0.42f),
                        fontSize = 12.sp
                    )
                },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentPink,
                    unfocusedBorderColor = accentPink.copy(alpha = 0.35f),
                    cursorColor = accentPink
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_dedicated_image_prompt")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF43F5E), Color(0xFFD946EF))
                            )
                        )
                        .clickable(enabled = !state.isGeneratingImage) {
                            val targetPrompt = promptInput.trim().ifBlank {
                                state.activeImagePrompt.ifBlank { "Realistic scenic river in Bangladesh at sunset" }
                            }
                            scope.launch {
                                val res = AgentAbilitySwitchesBus.createImageWithGemini(
                                    context = context,
                                    apiKey = apiKey,
                                    prompt = targetPrompt,
                                    editExistingImage = false
                                )
                                onStatusSpoken(res)
                            }
                        }
                        .padding(vertical = 11.dp)
                        .testTag("btn_dedicated_generate_real_image"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isGeneratingImage) "⏳ তৈরি হচ্ছে..." else "🎨 রিয়েল ছবি তৈরি করুন",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (decodedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .weight(0.85f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF18243C))
                            .border(1.dp, accentCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !state.isGeneratingImage) {
                                val targetPrompt = promptInput.trim().ifBlank { "Enhance lighting and details" }
                                scope.launch {
                                    val res = AgentAbilitySwitchesBus.createImageWithGemini(
                                        context = context,
                                        apiKey = apiKey,
                                        prompt = targetPrompt,
                                        editExistingImage = true
                                    )
                                    onStatusSpoken(res)
                                }
                            }
                            .padding(vertical = 11.dp)
                            .testTag("btn_dedicated_edit_real_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Image",
                                tint = accentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ছবি এডিট করুন",
                                color = accentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF080D1A))
                .border(
                    width = 1.5.dp,
                    color = if (state.isGeneratingImage) accentCyan else accentPink.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isGeneratingImage -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accentPink,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "রিয়েল এআই ছবি তৈরি হচ্ছে...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"${state.activeImagePrompt}\"",
                            color = accentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            color = accentPink,
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                decodedBitmap != null -> {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = state.activeImagePrompt.ifBlank { "Generated Real AI Image" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🎨",
                            fontSize = 38.sp
                        )
                        Text(
                            text = state.imageProgressMessageBn.ifBlank {
                                "আপনি যেকোনো ছবির কথা বললে বা উপরে লিখে ট্যাপ করলে এখানে সাথে সাথে রিয়েল ছবি তৈরি হবে।"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (state.pendingImageSaveConfirmation && !state.isImageSavedToArcher && decodedBitmap != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF071F18))
                    .border(1.5.dp, accentGreen, RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .testTag("banner_confirm_save_image")
            ) {
                Text(
                    text = "❓ এই কাজটা হয়ে গেছে — ছবিটা কি 'archer' ফোল্ডারে সেভ করব?",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "আপনার অনুমতি ছাড়া কোনো ফোল্ডারে অটো-সেভ করা হয়নি। ভয়েসে 'হ্যাঁ সেভ করো' বলুন অথবা নিচের বাটনে ট্যাপ করুন।",
                    color = Color(0xFFB8FFE0),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentGreen)
                            .clickable {
                                val msg = AgentAbilitySwitchesBus.confirmSavePendingImageToArcherFolder(context)
                                onStatusSpoken(msg)
                            }
                            .padding(vertical = 12.dp)
                            .testTag("btn_confirm_save_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💾 হ্যাঁ, ফোল্ডারে সেভ করুন",
                            color = Color(0xFF03140B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2B1218))
                            .border(1.dp, Color(0xFFFF4D6A), RoundedCornerShape(10.dp))
                            .clickable {
                                AgentAbilitySwitchesBus.discardPendingImage()
                                onStatusSpoken("জি স্যার, ছবিটি কোনো ফোল্ডারে সেভ করা হয়নি—বাদ দেওয়া হয়েছে।")
                            }
                            .padding(vertical = 12.dp)
                            .testTag("btn_discard_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗑️ না, বাদ দিন",
                            color = Color(0xFFFF8A9E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (state.isImageSavedToArcher && state.savedArcherImagePath.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentGreen.copy(alpha = 0.14f))
                    .border(1.dp, accentGreen, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "✔ আপনার অনুমতিতে 'archer' ফোল্ডারে সেভ হয়েছে!",
                    color = accentGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.savedArcherImagePath,
                    color = Color(0xFFB8FFE0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
