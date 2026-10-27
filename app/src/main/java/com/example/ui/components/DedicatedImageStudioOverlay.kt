package com.example.ui.components

import android.accounts.AccountManager
import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.service.PremiumImageModelTier
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dedicated Full-Screen Image Generation Studio Page:
 * - Automatically opens whenever image generation is triggered from the main screen (voice or text)
 *   or from the UI.
 * - Supports Nano Banana Pro (`gemini-3-pro-image-preview`), Imagen 4.0 Ultra (`imagen-4.0-ultra-generate-001`),
 *   Imagen 4.0 Pro (`imagen-4.0-generate-001`), and Nano Banana 2.1 (`gemini-2.5-flash-image`) with Auto-Routing.
 * - Provides both Paid/Premium API Key setup AND Main Gmail Account Sign-In so users with premium access
 *   can unlock all highest-tier models.
 * - Stages generated images in preview first and asks for explicit confirmation before saving to `archer`.
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

    var showPremiumAccessSheet by remember { mutableStateOf(false) }
    var premiumKeyInput by remember(state.premiumImageApiKey, apiKey) {
        mutableStateOf(state.premiumImageApiKey.ifBlank { apiKey })
    }
    var gmailInput by remember(state.connectedGmailAccount) {
        mutableStateOf(state.connectedGmailAccount)
    }

    // System Google Account Picker for 1-Tap Main Gmail Sign-In
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim().orEmpty()
            if (pickedEmail.isNotBlank()) {
                gmailInput = pickedEmail
                val msg = AgentAbilitySwitchesBus.connectMainGmailForPremium(context, pickedEmail)
                onStatusSpoken(msg)
            }
        }
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
    val accentGold = Color(0xFFFFD54F)

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
        // Header bar
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
                        text = "NANO BANANA PRO • IMAGEN 4 ULTRA STUDIO",
                        color = accentGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "প্রিমিয়াম রিয়েল ছবি তৈরির স্টুডিও",
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

        Spacer(modifier = Modifier.height(10.dp))

        // Premium Model Selector & Gmail / API Key Status Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0C1426))
                .border(1.dp, accentGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚡ অ্যাক্টিভ প্রিমিয়াম মডেল: ${state.selectedImageModelTier.badgeLabel}",
                        color = accentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = state.selectedImageModelTier.subtitleBn,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.isGmailPremiumLinked || state.premiumImageApiKey.isNotBlank() || apiKey.isNotBlank())
                                accentGreen.copy(alpha = 0.16f)
                            else
                                accentGold.copy(alpha = 0.18f)
                        )
                        .border(
                            1.dp,
                            if (state.isGmailPremiumLinked || state.premiumImageApiKey.isNotBlank() || apiKey.isNotBlank())
                                accentGreen
                            else
                                accentGold,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { showPremiumAccessSheet = !showPremiumAccessSheet }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_toggle_premium_gmail_api_panel")
                ) {
                    Text(
                        text = when {
                            state.isGmailPremiumLinked -> "✔ Gmail: ${state.connectedGmailAccount.substringBefore("@")}"
                            state.premiumImageApiKey.isNotBlank() || apiKey.isNotBlank() -> "🔑 PRO KEY ACTIVE"
                            else -> "🔑 Gmail / API লগইন"
                        },
                        color = if (state.isGmailPremiumLinked || state.premiumImageApiKey.isNotBlank() || apiKey.isNotBlank())
                            accentGreen
                        else
                            accentGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Scrollable Model Tier Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumImageModelTier.entries.forEach { tier ->
                    val isSelected = state.selectedImageModelTier == tier
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) accentPink.copy(alpha = 0.25f) else Color(0xFF141E33)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentPink else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                AgentAbilitySwitchesBus.setSelectedImageModelTier(context, tier)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("pill_image_model_${tier.id}")
                    ) {
                        Text(
                            text = tier.badgeLabel,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }

            // Expandable Main Gmail Sign-In + Paid API Key Panel
            if (showPremiumAccessSheet) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF070C18))
                        .border(1.dp, accentCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(11.dp)
                        .testTag("panel_premium_gmail_and_api_key")
                ) {
                    Text(
                        text = "🌟 প্রিমিয়াম মডেল (Nano Banana Pro ও Imagen 4 Ultra) কানেকশন:",
                        color = accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "• আপনার প্রিমিয়াম API Key থাকলে নিচে বসান — সাথে সাথে Nano Banana Pro (gemini-3-pro-image-preview) ও Imagen 4.0 Ultra অটো-রাউটিং হবে।\n• অথবা আপনার মেইন জিমেইল (Gmail ID) লগইন করে প্রিমিয়াম সুবিধা চালু করুন।",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: Main Gmail Account Login
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Main Gmail",
                            tint = accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "১. মেইন জিমেইল আইডি (Main Gmail Sign-In):",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = gmailInput,
                        onValueChange = { gmailInput = it },
                        placeholder = {
                            Text(
                                "আপনার প্রিমিয়াম জিমেইল (যেমন: yourname@gmail.com)",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentGreen,
                            unfocusedBorderColor = accentGreen.copy(alpha = 0.35f),
                            cursorColor = accentGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_main_gmail_account")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentGreen.copy(alpha = 0.22f))
                                .border(1.dp, accentGreen, RoundedCornerShape(8.dp))
                                .clickable {
                                    // If user hasn't typed an email yet, try launching system Google Account picker first
                                    if (gmailInput.isBlank()) {
                                        runCatching {
                                            val pickIntent = AccountManager.newChooseAccountIntent(
                                                null,
                                                null,
                                                arrayOf("com.google"),
                                                null,
                                                null,
                                                null,
                                                null
                                            )
                                            googleAccountPickerLauncher.launch(pickIntent)
                                        }.onFailure {
                                            val msg = "জিমেইল বক্সে আপনার মেইন Gmail আইডি লিখে ট্যাপ করুন।"
                                            onStatusSpoken(msg)
                                        }
                                    } else {
                                        val msg = AgentAbilitySwitchesBus.connectMainGmailForPremium(context, gmailInput)
                                        onStatusSpoken(msg)
                                    }
                                }
                                .padding(vertical = 9.dp)
                                .testTag("btn_connect_main_gmail"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (gmailInput.isBlank()) "📲 ফোনের মেইন Gmail সিলেক্ট করুন" else "✔ এই Gmail দিয়ে লগইন করুন",
                                color = accentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (state.isGmailPremiumLinked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2B1218))
                                    .border(1.dp, Color(0xFFFF4D6A), RoundedCornerShape(8.dp))
                                    .clickable {
                                        gmailInput = ""
                                        AgentAbilitySwitchesBus.disconnectMainGmail(context)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                                    .testTag("btn_disconnect_main_gmail"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "লগআউট",
                                    color = Color(0xFFFF8A9E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Paid / Premium Gemini API Key
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = "Premium API Key",
                            tint = accentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "২. প্রিমিয়াম Gemini API Key (Nano Banana Pro & Imagen 4):",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = premiumKeyInput,
                        onValueChange = { premiumKeyInput = it },
                        placeholder = {
                            Text(
                                "AIzaSy... আপনার প্রিমিয়াম API Key বসান",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = accentGold.copy(alpha = 0.35f),
                            cursorColor = accentGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_premium_image_api_key")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentGold.copy(alpha = 0.22f))
                            .border(1.dp, accentGold, RoundedCornerShape(8.dp))
                            .clickable {
                                val msg = AgentAbilitySwitchesBus.savePremiumApiKey(context, premiumKeyInput)
                                onStatusSpoken(msg)
                            }
                            .padding(vertical = 9.dp)
                            .testTag("btn_save_premium_image_api_key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔑 প্রিমিয়াম API Key সেভ ও অ্যাক্টিভ করুন",
                            color = accentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt Input & Action Controls on the Dedicated Page
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
                                    apiKey = state.premiumImageApiKey.ifBlank { apiKey },
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
                        text = if (state.isGeneratingImage) "⏳ প্রিমিয়াম মডেলে তৈরি হচ্ছে..." else "🎨 রিয়েল ছবি তৈরি করুন (${state.selectedImageModelTier.badgeLabel})",
                        color = Color.White,
                        fontSize = 11.sp,
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
                                        apiKey = state.premiumImageApiKey.ifBlank { apiKey },
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

        // Main Image Viewport / Live Generation Status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
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
                            text = "${state.selectedImageModelTier.badgeLabel} দিয়ে রিয়েল ছবি তৈরি হচ্ছে...",
                            color = Color.White,
                            fontSize = 14.sp,
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = decodedBitmap,
                            contentDescription = state.activeImagePrompt.ifBlank { "Generated Real AI Image" },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (state.lastUsedImageModelName.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xCC060913))
                                    .border(1.dp, accentGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚡ ${state.lastUsedImageModelName}",
                                    color = accentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                                "আপনি যেকোনো ছবির কথা বললে বা উপরে লিখে ট্যাপ করলে Nano Banana Pro ও Imagen 4 Ultra দিয়ে সাথে সাথে রিয়েল ছবি তৈরি হবে।"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Mandatory User Confirmation Before Saving to Folder!
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
