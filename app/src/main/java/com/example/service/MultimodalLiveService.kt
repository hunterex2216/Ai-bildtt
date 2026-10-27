package com.example.service

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

private const val TAG = "MultimodalLiveService"

enum class LiveConnectionEngine(
    val id: String,
    val badgeText: String,
    val labelBn: String
) {
    AUTO_HYBRID(
        id = "auto_hybrid",
        badgeText = "GEMINI LIVE WEBSOCKET",
        labelBn = "ডাইরেক্ট Gemini Live WebSocket (BidiGenerateContent)"
    ),
    BUILTIN_FIREBASE_STREAM(
        id = "builtin_stream",
        badgeText = "GEMINI LIVE WEBSOCKET",
        labelBn = "ডাইরেক্ট Gemini Live WebSocket (BidiGenerateContent)"
    ),
    DIRECT_WEBSOCKET_BIDI(
        id = "direct_ws_bidi",
        badgeText = "GEMINI LIVE WEBSOCKET",
        labelBn = "ডাইরেক্ট Gemini Live WebSocket (BidiGenerateContent)"
    )
}

data class MultimodalLiveExportConfig(
    val builtInFirebaseApiKey: String = "",
    val customUserApiKey: String = "",
    val effectiveApiKey: String = "",
    val projectId: String = "",
    val applicationId: String = "",
    val packageName: String = "",
    val certSha1: String = "",
    val websocketBidiEndpoint: String =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent",
    val fullWebsocketUrlWithKey: String = "",
    val restStreamEndpoint: String =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:streamGenerateContent?alt=sse",
    val activeLiveModel: String = "models/gemini-3.1-flash-live-preview",
    val fallbackStreamModel: String = "models/gemini-2.5-flash-native-audio-preview-12-2025",
    val setupHandshakeJson: String = "",
    val setupPayloadJson: String = "",
    val toolsDeclarationJson: String = "",
    val realtimeFramePayloadSample: String = "",
    val pythonLiveClientCode: String = ""
)

data class MultimodalLiveSessionState(
    val isLiveActive: Boolean = false,
    val connectionEngine: LiveConnectionEngine = LiveConnectionEngine.DIRECT_WEBSOCKET_BIDI,
    val engineMode: LiveConnectionEngine = LiveConnectionEngine.DIRECT_WEBSOCKET_BIDI,
    val connectionStatusLabel: String = "CONNECTING • GEMINI LIVE",
    val isWebSocketConnected: Boolean = false,
    val isMicMuted: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val isCameraStreamEnabled: Boolean = false,
    val isScreenStreamEnabled: Boolean = true,
    val isRealTimeToolsArmed: Boolean = true,
    val isToolsArmed: Boolean = true,
    val isAgentSpeakingLive: Boolean = false,
    val isUserSpeakingBargeIn: Boolean = false,
    val liveRmsLevel: Float = 0f,
    val streamedCameraFrames: Int = 0,
    val streamedScreenFrames: Int = 0,
    val framesSentCount: Int = 0,
    val fpsTelemetry: String = "LIVE 24kHz PCM • VISION",
    val streamedAudioPackets: Int = 0,
    val executedLiveActionsCount: Int = 0,
    val bargeInInterruptCount: Int = 0,
    val lastVisualInsight: String = "ক্যামেরা ও পার্মানেন্ট স্ক্রিন ভিশন সক্রিয়",
    val audioWaveformLevels: List<Float> = listOf(
        0.22f, 0.38f, 0.54f, 0.30f, 0.68f, 0.82f, 0.45f, 0.60f,
        0.75f, 0.42f, 0.58f, 0.85f, 0.50f, 0.36f, 0.62f, 0.28f
    ),
    val lastLiveToolSummary: String = "লাইভ ভয়েস, ক্যামেরা ও পার্মানেন্ট স্ক্রিন কন্ট্রোল প্রস্তুত",
    val lastToolExecuted: String = "লাইভ ভয়েস, ক্যামেরা ও ৪-দিকের স্ক্রিন কন্ট্রোল প্রস্তুত",
    val lastCopiedLabel: String = "",
    val liveStreamingPartialReply: String = "",
    val activeModelName: String = "models/gemini-3.1-flash-live-preview",
    val activeVoiceName: String = "Charon",
    val activeVoiceTitleBn: String = "Charon • মেইন ছেলে ভয়েস (আসল মানুষের মতো স্পষ্ট)",
    val micSensitivityBoost: Float = 2.8f,
    val statusMessageBn: String = "Gemini Live প্রস্তুত — সরাসরি কথা বলুন",
    val exportConfig: MultimodalLiveExportConfig = MultimodalLiveExportConfig()
)

class MultimodalLiveService(
    private val context: Context,
    private val onLiveSpokenChunkOrReply: (textChunk: String, isFinalTurn: Boolean) -> Unit,
    private val onExecuteLiveActionTag: (userUtterance: String, rawAiResponse: String) -> Unit,
    private val onBargeInInterruptRequested: () -> Unit,
    private val onUserTranscript: (String) -> Unit = {}
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(MultimodalLiveSessionState())
    val state: StateFlow<MultimodalLiveSessionState> = _state.asStateFlow()

    @Volatile
    private var latestCameraBitmap: Bitmap? = null

    @Volatile
    private var latestScreenBitmap: Bitmap? = null

    private var framePumpJob: Job? = null
    private var audioRecordPumpJob: Job? = null
    private var audioPlaybackPumpJob: Job? = null
    private var activeWebSocket: WebSocket? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    // Non-blocking 24kHz PCM playback queue + AudioTrack so WebSocket onMessage never blocks
    // and barge-in flushes audio in < 10ms.
    private val audioTrackLock = Any()
    private var liveOutputAudioTrack: AudioTrack? = null
    private val pcmPlaybackQueue = ConcurrentLinkedQueue<ByteArray>()
    @Volatile
    private var lastAudioChunkPlayedAtMs: Long = 0L

    // Live model candidates: if one is rejected before setup completes, the next one is tried automatically.
    private var liveModelCandidates: List<String> = listOf(
        "models/gemini-3.1-flash-live-preview",
        "models/gemini-2.5-flash-native-audio-preview-12-2025",
        "models/gemini-2.5-flash-native-audio-preview-09-2025",
        "models/gemini-live-2.5-flash-preview"
    )
    @Volatile private var modelIndex = 0
    private val wsBidiModel: String get() = liveModelCandidates[modelIndex.coerceIn(0, liveModelCandidates.size - 1)]
    @Volatile private var setupDone = false
    @Volatile var lastFrameForBrain: Bitmap? = null
    private var lastSentSig: IntArray? = null
    private var lastSentAt = 0L
    private var reconnectAttempts = 0
    private var toolCallHandler: (suspend (String) -> String)? = null
    private val userTurnBuf = StringBuilder()
    private val modelTurnBuf = StringBuilder()
    @Volatile private var pendingTextTurn: String? = null
    @Volatile private var activePersonality: com.example.model.AgentPersonalityPreset =
        com.example.model.AgentPersonalityPreset.CORE_JARVIS_BOSS
    @Volatile private var activeVoicePreset: com.example.model.AgentVoicePreset =
        com.example.model.AgentVoicePreset.CHARON_MALE
    @Volatile private var customPersonalityPrompt: String = ""
    @Volatile private var micSensitivityBoost: Float = 2.8f
    @Volatile private var lastRmsUiUpdateAtMs: Long = 0L
    @Volatile private var totalStreamedAudioPackets: Int = 0

    fun setToolCallHandler(handler: suspend (String) -> String) {
        toolCallHandler = handler
    }

    private var namedToolHandler: (suspend (String, JSONObject) -> String)? = null
    fun setNamedToolHandler(handler: suspend (String, JSONObject) -> String) {
        namedToolHandler = handler
    }

    private var onUserTurnCompleteForMemory: ((userUtterance: String, aiReply: String) -> Unit)? = null
    fun setOnUserTurnCompleteForMemory(listener: (userUtterance: String, aiReply: String) -> Unit) {
        onUserTurnCompleteForMemory = listener
    }

    fun queueTurnBeforeStart(textPayload: String?) {
        pendingTextTurn = textPayload?.takeIf { it.isNotBlank() }
    }

    fun setMicSensitivityBoost(boost: Float) {
        val clamped = boost.coerceIn(1.0f, 4.0f)
        micSensitivityBoost = clamped
        _state.value = _state.value.copy(
            micSensitivityBoost = clamped,
            isMicMuted = false,
            statusMessageBn = "শোনার পাওয়ার (Mic Gain): ${String.format( java.util.Locale.US, "%.1fx", clamped)} • শুনছি…"
        )
        if (_state.value.isLiveActive && (audioRecordPumpJob == null || audioRecordPumpJob?.isActive != true)) {
            startRealTimeAudioRecordPump()
        }
    }

    fun updateVoicePreset(
        voicePreset: com.example.model.AgentVoicePreset,
        reconnectIfActive: Boolean = true,
        greetingPromptAfterSwitch: String? = null
    ) {
        activeVoicePreset = voicePreset
        _state.value = _state.value.copy(
            activeVoiceName = voicePreset.liveVoiceName,
            activeVoiceTitleBn = voicePreset.titleBn,
            isMicMuted = false
        )
        if (!greetingPromptAfterSwitch.isNullOrBlank()) {
            pendingTextTurn = greetingPromptAfterSwitch
        }
        if (reconnectIfActive && _state.value.isLiveActive) {
            stopAndFlushLiveAudioTrack()
            ExternalMicArbiter.setGeminiLiveOwningMic(true)
            connectDirectBidiWebSocketIfPossible()
            if (audioRecordPumpJob == null || audioRecordPumpJob?.isActive != true) {
                startRealTimeAudioRecordPump()
            }
            if (audioPlaybackPumpJob == null || audioPlaybackPumpJob?.isActive != true) {
                startAudioPlaybackPump()
            }
        }
    }

    fun updatePersonality(
        preset: com.example.model.AgentPersonalityPreset,
        customPrompt: String = "",
        voiceOverride: com.example.model.AgentVoicePreset? = null,
        reconnectIfActive: Boolean = true,
        greetingPromptAfterSwitch: String? = null
    ) {
        activePersonality = preset
        customPersonalityPrompt = customPrompt.trim()
        val targetVoice = voiceOverride ?: com.example.model.AgentVoicePreset.fromIdOrVoiceName(preset.liveVoiceName)
        activeVoicePreset = targetVoice
        _state.value = _state.value.copy(
            activeVoiceName = targetVoice.liveVoiceName,
            activeVoiceTitleBn = targetVoice.titleBn,
            isMicMuted = false
        )
        if (!greetingPromptAfterSwitch.isNullOrBlank()) {
            pendingTextTurn = greetingPromptAfterSwitch
        }
        if (reconnectIfActive && _state.value.isLiveActive) {
            stopAndFlushLiveAudioTrack()
            ExternalMicArbiter.setGeminiLiveOwningMic(true)
            connectDirectBidiWebSocketIfPossible()
            if (audioRecordPumpJob == null || audioRecordPumpJob?.isActive != true) {
                startRealTimeAudioRecordPump()
            }
            if (audioPlaybackPumpJob == null || audioPlaybackPumpJob?.isActive != true) {
                startAudioPlaybackPump()
            }
        }
    }

    init {
        ExternalMicArbiter.init(context)
        SmartMemoryAndSelfUpdateEngine.init(context)
        UnifiedAudioRouterBus.init(context)
        AgentAbilitySwitchesBus.ensureInitialized(context)
        val permReady = ScreenCaptureSessionBus.syncSavedPreference(context)
        _state.value = _state.value.copy(isScreenStreamEnabled = permReady)
        refreshExportConfig(customApiKey = "")
    }

    private fun strProp(desc: String) = JSONObject().put("type", "STRING").put("description", desc)

    private fun toolDecl(name: String, desc: String, props: List<Pair<String, String>>, required: List<String>) = JSONObject().apply {
        put("name", name)
        put("description", desc)
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply { props.forEach { (k, d) -> put(k, strProp(d)) } })
            put("required", JSONArray().apply { required.forEach { put(it) } })
        })
    }

    private fun uiMemoryDecl() = toolDecl("ui_memory",
        "Look up where a button/field was seen before in any app (remembered coordinates). Use BEFORE searching around: if found, tap it with phone_action SCREEN_UI_CONTROL target='x,y' extra='TAP_XY'.",
        listOf("query" to "Label to find, e.g. 'search', 'play', 'send'", "app" to "Optional app name or package"), listOf("query"))

    private fun findAppDecl() = toolDecl("find_app",
        "Find installed apps by name (returns label + package). Use to check if an app exists before opening it.",
        listOf("query" to "App name fragment"), listOf("query"))

    private fun installAppDecl() = toolDecl(
        "install_app",
        "Install or update any app or game from Google Play Store (e.g. WhatsApp, Free Fire, TikTok, YouTube, CapCut, bKash, Ludo King, Telegram, etc.). Automatically checks Accessibility permission first, opens the app's Play Store page directly, taps the 'Install' / 'Update' button, retries if needed until real download progress starts, and monitors until 100% installed.",
        listOf(
            "app_name" to "Clean canonical name of the app or game to install/update (e.g. 'Free Fire', 'WhatsApp', 'TikTok', 'CapCut', 'Ludo King')",
            "mode" to "Optional 'INSTALL' (default) or 'UPDATE'"
        ),
        listOf("app_name")
    )

    private fun deepThinkDecl() = toolDecl("deep_think",
        "Ask the most powerful Gemini model (it also sees the current screen) to plan a HARD or multi-step task or to recover when you are stuck/failing. Returns a numbered action plan.",
        listOf("goal" to "What you are trying to achieve and what went wrong, if anything"), listOf("goal"))

    private fun appsContextBlock(): String = try {
        "\n\nINSTALLED APPS ON THIS PHONE (you know all of them; open by exact name): " + InstalledAppsIndex.summary(context, 160)
    } catch (_: Exception) { "" }

    private fun appControlDecl(): JSONObject = JSONObject().apply {
        put("name", "app_control")
        put("description", "Switch OmniAgent's own features on/off. Use when the user says things like 'touch effect on/off', 'screen share off', 'mute mic'.")
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("feature", strProp("TOUCH_EFFECT, SCREEN_SHARE, CAMERA, MIC_MUTE or SPEAKER_MUTE"))
                put("state", strProp("ON or OFF"))
            })
            put("required", JSONArray().put("feature").put("state"))
        })
    }

    private fun writeCodeDecl(): JSONObject = JSONObject().apply {
        put("name", "write_code")
        put("description", "Argon 4 coding mode: write a COMPLETE file of code. The app shows a full-screen live typing page, then saves the file (file, folder and zip) into the 'archer' folder. Pass the entire code in 'code'.")
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("title", strProp("Short project title"))
                put("file_name", strProp("File name with extension, e.g. index.html"))
                put("language", strProp("Language, e.g. HTML, Python, Kotlin"))
                put("code", strProp("The full source code"))
            })
            put("required", JSONArray().put("title").put("file_name").put("code"))
        })
    }

    private fun sendGeneratedFileDecl(): JSONObject = toolDecl(
        "send_generated_file",
        "Send or share a generated code file or ZIP archive from the 'archer' folder directly to a contact on WhatsApp, Telegram, Email, or Android Share Sheet. Use whenever the user says 'এই ফাইলটা অমুককে পাঠাও' or 'কোড ফাইলটা রাহিমকে হোয়াটসঅ্যাপে সেন্ড করো'.",
        listOf(
            "recipient" to "Contact name, phone number, or target app (e.g. 'রাহিম', 'WhatsApp', 'Telegram')",
            "file_name" to "Optional file name (e.g. 'index.html', 'app.py', or '.zip'). Leave blank to send the latest generated file",
            "send_as_zip" to "Optional 'true' to send the .zip archive or 'false' to send the code file directly",
            "caption" to "Optional message caption to include with the file"
        ),
        listOf("recipient")
    )

    private fun workflowPlaybookDecl(): JSONObject = toolDecl(
        "workflow_playbook",
        "Recall or save multi-step task workflows in permanent Workflow Memory so repeated or similar tasks execute automatically at millisecond speed.",
        listOf(
            "mode" to "Either 'MATCH' (find learned steps for a task) or 'SAVE' (record steps taken for a completed task)",
            "task_trigger" to "User task phrase or goal (e.g. 'ইউটিউবে কার্টুন চালাও', 'হোয়াটসঅ্যাপে ফাইল পাঠাও')",
            "target_app" to "Target app or website name",
            "steps" to "When mode='SAVE', step-by-step actions separated by ' -> '"
        ),
        listOf("mode", "task_trigger")
    )

    private fun proactiveWatchDecl() = toolDecl(
        "proactive_watch_or_timer",
        "Set an autonomous background watcher or countdown timer so you can speak to the user on your own when it finishes! Use when the user says 'এটা লোড হচ্ছে শেষ হলে জানাইয়ো' (mode='WATCH_LOADING'), 'ডাউনলোড শেষ হলে বলো' (mode='WATCH_DOWNLOAD'), or '১০ মিনিট পরে আমাকে জানাইয়ো' (mode='TIMER', seconds='600').",
        listOf(
            "mode" to "One of: TIMER, WATCH_LOADING, WATCH_DOWNLOAD",
            "seconds" to "Countdown seconds if mode is TIMER (e.g. '600' for 10 minutes, '60' for 1 minute)",
            "label" to "What to remind or notify the user about in Bengali/English"
        ),
        listOf("mode", "label")
    )

    private fun switchPersonalityDecl() = toolDecl(
        "switch_personality",
        "Change your active personality mode when the user asks to change personality/mood/style (e.g. Jarvis Boss, Gemini Female Assistant, Caring Lady, Smart Lady Secretary, Desi Friend, Executive Pro, Witty, Cyber Sentinel, Tech Guru, or Custom).",
        listOf(
            "personality" to "One of: CORE_JARVIS_BOSS, GEMINI_FEMALE_ASSISTANT, CARING_LADY_COMPANION, SMART_LADY_SECRETARY, DESI_BONDHU, EXECUTIVE_PRO, WITTY_HUMOROUS, TACTICAL_SENTINEL, CODING_TECH_GURU, CUSTOM",
            "custom_rule" to "Optional custom personality description if personality is CUSTOM"
        ),
        listOf("personality")
    )

    private fun switchVoiceOrMicPowerDecl() = toolDecl(
        "switch_voice_or_mic_power",
        "Change your spoken voice (natural human Female voices = Aoede, Kore; natural human Male voices = Charon, Puck, Fenrir, Orus, Iapetus, Schedar), adjust microphone listening sensitivity power, or switch audio routing.",
        listOf(
            "voice" to "Optional voice name or ID: Female voices = Aoede, Kore, Leda, Zephyr, Callirrhoe, Autonoe, Despina, Erinome, Laomedeia, Vindemiatrix; Natural Male voices = Charon, Puck, Fenrir, Orus, Iapetus, Schedar",
            "mic_boost" to "Optional mic listening power multiplier from '1.0' to '4.0' (e.g. '3.2' or '4.0' when user says শোনার পাওয়ার বাড়াও)",
            "audio_route" to "Optional audio routing target: AUTO_SMART, PRIMARY_SPEAKER, BLUETOOTH_HEADSET, WIRED_HEADSET, or EARPIECE_PRIVATE"
        ),
        emptyList()
    )

    private fun powerOffSelfDecl() = toolDecl(
        "power_off_self",
        "Completely turn yourself OFF / shut down / go to sleep when the user tells you to turn off completely (e.g. 'একদম বন্ধ হয়ে যাও', 'নিজেকে অফ করো', 'বন্ধ হয়ে যাও', 'অফ হয়ে যাও', 'এখন বন্ধ হও', 'turn off completely', 'shut down', 'go to sleep'). You will stay completely off until the user re-opens the app or calls your name.",
        listOf(
            "farewell_message" to "Short 1-sentence natural goodbye in Bengali telling Sir you are turning off completely now and will wake up when he enters the app or calls your name"
        ),
        emptyList()
    )

    private fun understandAppOrWebsiteDecl() = toolDecl(
        "understand_app_or_website",
        "Analyze and explain how ANY app or website works (search bar, buttons, navigation tabs, workflow steps, and current screen structure), or learn its layout so you can inform Sir and operate it accurately.",
        listOf(
            "app_or_site" to "App name, website domain, or 'CURRENT_SCREEN' to inspect the currently open app/website"
        ),
        listOf("app_or_site")
    )

    private fun selfHealOrUpdateDecl() = toolDecl(
        "self_heal_or_update",
        "Self-heal / repair / speed up yourself ('নিজেকে নিজে ঠিক করো', 'ফাস্ট করো') OR promote any user-specified update/rule/feature directly into the Main Core System ('মেইন সিস্টেম আপডেট করো', 'নিজেকে আপডেট করো', 'যেই কোনো কিছু আপডেট') so the main system immediately obeys what the user commanded.",
        listOf(
            "mode" to "Either 'SELF_HEAL' (repair lag, mic, memory, turbo UI speed) or 'INSTALL_UPDATE' (promote any new behavior, rule, or feature directly into the Main Core System)",
            "title" to "Short title of the main system update or repair",
            "specification" to "Exact rule, behavior, or feature directive commanded by the user to promote into the Main Core System"
        ),
        listOf("mode", "specification")
    )

    private fun backgroundWebSearchDecl() = toolDecl(
        "background_web_search",
        "Look up real-time facts, news, prices, or Google Maps places using gemini-3.8-flash (with googleMaps and googleSearch tools) in the background WITHOUT opening a browser on screen. Respects the 'Background web search' switch.",
        listOf(
            "query" to "Fact, question, or Google Maps place/location to search in the background",
            "use_maps" to "Optional 'true' if looking up a place, address, hospital, restaurant, route, or nearby location"
        ),
        listOf("query")
    )

    private fun makeImageWithGeminiDecl() = toolDecl(
        "make_image_with_gemini",
        "Open the Dedicated Image Generation Studio Page and generate a 100% REAL image matching the user's exact spoken/written prompt (or edit the current image). Stages the image in preview without auto-saving to any folder and asks the user whether to save it.",
        listOf(
            "prompt" to "Detailed description of the real image to create or instructions to edit the image",
            "edit_existing" to "Optional 'true' to edit the last generated image, or 'false' to create a new image"
        ),
        listOf("prompt")
    )

    private fun bioscienceSkillDecl() = toolDecl(
        "bioscience_skill",
        "Run Bioscience & Molecular Research Skills: query live NCBI PubMed, UniProt Protein Knowledgebase, and PubChem databases and synthesize genomics, proteomics, pharmacology, or clinical insights.",
        listOf(
            "query" to "Gene, protein, drug compound, disease mechanism, or biomedical question (e.g. BRCA1, CRISPR, Aspirin)",
            "domain" to "Optional domain filter: ALL, GENE, PROTEIN, or DRUG"
        ),
        listOf("query")
    )

    private fun generateMusicLyriaDecl() = toolDecl(
        "generate_music_lyria",
        "Generate an original AI song or instrumental clip using Google Lyria 3 in preview mode (does NOT auto-save to a folder until the user confirms).",
        listOf(
            "prompt" to "Musical style, mood, instruments, or lyrics description",
            "full_song" to "Optional 'true' for full-length song (lyria-3-pro-preview) or 'false' for 30s clip (lyria-3-clip-preview)"
        ),
        listOf("prompt")
    )

    private fun setAbilitySwitchDecl() = toolDecl(
        "set_ability_switch",
        "Toggle one of the 7 Ability Permission Switches in Main Settings or change the Floating Avatar character when requested by the user.",
        listOf(
            "ability" to "One of: SILENT_EXECUTION, BACKGROUND_WEB_SEARCH, MAKE_IMAGES_WITH_GEMINI, IGNORE_BACKGROUND_AUDIO, SHOW_TAP_INDICATOR, SEND_CRASH_REPORTS, FLOATING_AVATAR",
            "state" to "ON, OFF, or character name (ARCHER, JARVIS, AURA, NOVA, OFF) for FLOATING_AVATAR"
        ),
        listOf("ability", "state")
    )

    private fun confirmPendingSaveDecl() = toolDecl(
        "confirm_pending_save",
        "Save or discard the pending generated code, text, image, or music track ONLY after the user responds to your confirmation question ('এই কাজটা হয়ে গেছে—এটা কি সেভ করব?').",
        listOf(
            "decision" to "Either 'SAVE' (when Sir says yes/হ্যাঁ সেভ করো) or 'DISCARD' (when Sir says no/না সেভ করতে হবে না/বাদ দাও)",
            "target_type" to "Optional: 'ALL', 'IMAGE', 'CODE', or 'MUSIC'"
        ),
        listOf("decision")
    )

    private fun saveUserMemoryDecl() = toolDecl(
        "save_user_memory",
        "Save any important user fact, personal detail, preference, contact/family info, routine, project, password/code note, or explicit 'মনে রাখো / remember this' instruction into Sir's permanent Long-Term Memory (Memory Tab). Use BOTH when Sir explicitly tells you to remember something AND automatically whenever you detect an important personal fact or preference worth remembering!",
        listOf(
            "title" to "Short clear title in Bengali or English (e.g. 'স্যারের প্রিয় খাবার', 'অফিসের ঠিকানা', 'ভাইয়ের নাম্বার')",
            "content" to "The complete important fact, instruction, or detail to remember permanently",
            "category" to "One of: EXPLICIT_USER_MEMORY, PERSONAL_PROFILE, IMPORTANT_CONTACT, ROUTINE_SCHEDULE, WORK_PROJECT, SECRET_REFERENCE, USER_PREFERENCE"
        ),
        listOf("title", "content")
    )

    fun currentApiKey(): String =
        Regex("key=([^&]+)").find(_state.value.exportConfig.fullWebsocketUrlWithKey)?.groupValues?.get(1).orEmpty()

    private fun buildPersonalityContextBlock(): String = buildString {
        append("\n\n")
        append(activePersonality.systemPromptDirective)
        append("\nACTIVE VOICE PROFILE: ${activeVoicePreset.liveVoiceName} (${activeVoicePreset.titleBn} - ${activeVoicePreset.gender.labelBn}).")
        append(
            "\nUNIFIED MULTIMODAL LIVE SCREEN VISION: ACTIVE (MILLISECOND STREAM) — " +
                "Multimodal Live Screen Share is your single unified vision engine, running continuously at millisecond speed (combining live MediaProjection frames + Accessibility hardware screenshots + real-time UI node coordinates `label@x,y`). " +
                "NEVER complain that 'মেইন স্ক্রিন শেয়ার চালু হয়নি' or 'Main Screen Share is OFF'."
        )
        val sw = AgentAbilitySwitchesBus.state.value
        append(
            "\nACTIVE ABILITY SWITCHES IN MAIN SETTINGS (STRICTLY OBEY — When a switch is OFF, do NOT use that ability and tell the user to turn it on in Main Settings):\n" +
                "- Silent execution: ${if (sw.silentExecution) "ON (stay quiet while working through multi-step actions; give ONE clear report at the end)" else "OFF (narrate steps as you work)"}\n" +
                "- Background web search & Google Maps (gemini-3.8-flash): ${if (sw.backgroundWebSearch) "ON (use background_web_search tool to look up facts/places without opening browser)" else "OFF (do NOT use background web search; tell user to turn on Background web search in Main Settings)"}\n" +
                "- Make & edit images with Gemini (gemini-nano-banana-2.1): ${if (sw.makeImagesWithGemini) "ON (use make_image_with_gemini tool to create or edit images)" else "OFF (do NOT generate images; tell user to turn on Make images with Gemini in Main Settings)"}\n" +
                "- Ignore background audio & Noise Cancellation: ${if (sw.ignoreBackgroundAudio) "ON (NEAR-FIELD PRIMARY SPEAKER LOCK ACTIVE — listen ONLY to the main user speaking directly into the phone. Strictly ignore any background people talking, room chatter, TV/media voices, or side conversations)" else "OFF"}\n" +
                "- Show tap indicator: ${if (sw.showTapIndicator) "ON" else "OFF"}\n" +
                "- Send crash reports: ${if (sw.sendCrashReports) "ON" else "OFF"}\n" +
                "- Floating avatar: ${sw.floatingAvatar.displayName}"
        )
        if (customPersonalityPrompt.isNotBlank()) {
            append("\nUSER CUSTOM PERSONALITY RULE: ").append(customPersonalityPrompt)
        }
        append("\n\n")
        append(AppKnowledgeRegistry.compactSummaryForSystemPrompt())
        append(SmartMemoryAndSelfUpdateEngine.buildEvolutionContextForPrompt())
    }

    private fun buildSetupJson(): String = JSONObject().apply {
        put("setup", JSONObject().apply {
            put("model", wsBidiModel)
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().put("voiceConfig",
                    JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", activeVoicePreset.liveVoiceName))))
            })
            put("systemInstruction", JSONObject().put("parts",
                JSONArray().put(JSONObject().put("text", LIVE_MULTIMODAL_SYSTEM_PROMPT + buildPersonalityContextBlock() + appsContextBlock()))))
            put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray().put(
                JSONObject().apply {
                    put("name", "phone_action")
                    put("description", "Full autonomous access to the user's phone at millisecond speed: open apps, install/update apps from Play Store, send WhatsApp/Telegram/SMS/Email messages, share generated code files, clear Recent Apps, control all 6 sound streams, call contacts, toggle system settings, or tap/type/scroll/swipe any element on screen.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("action", JSONObject().put("type", "STRING").put("description",
                                "One of: OPEN_APP, INSTALL_APP_PLAYSTORE, WHATSAPP_AUTOMATION, TELEGRAM_AUTOMATION, CHROME_WEB_ACTION, DOWNLOAD_FILE_MEDIA, SCREEN_UI_CONTROL, SCAN_CONTACT_OR_SCREEN, SHARE_ARCHER_FILE, GENERATE_CODE_PROJECT, CLEAR_RECENT_APPS, TOGGLE_SYSTEM_SETTING, SET_VOLUME, ANALYZE_APP_OR_SITE, SELF_HEAL_OR_UPDATE, PLAY_MUSIC, STOP_MUSIC, DIAL_NUMBER, SEND_MESSAGE, SEND_EMAIL, SET_ALARM, SET_TIMER, OPEN_SETTINGS, READ_NOTIFICATIONS, READ_DEVICE_STATUS"))
                            put("target", JSONObject().put("type", "STRING").put("description",
                                "For OPEN_APP/INSTALL_APP_PLAYSTORE: app name. For WHATSAPP_AUTOMATION/TELEGRAM_AUTOMATION/SHARE_ARCHER_FILE: contact name or phone number. For CLEAR_RECENT_APPS: app name or 'ALL'. For SET_VOLUME: MUSIC, RING, NOTIFICATION, ALARM, VOICE_CALL, SYSTEM, or ALL. For SCREEN_UI_CONTROL: exact button label to click, or 'x,y' coordinates for TAP_XY, or exact text to type when extra='TYPE'."))
                            put("extra", JSONObject().put("type", "STRING").put("description",
                                "For OPEN_APP: clean search query. For WHATSAPP_AUTOMATION/TELEGRAM_AUTOMATION: ONLY the clean message text to send. For SHARE_ARCHER_FILE: file name (e.g. 'index.html' or 'zip'). For SET_VOLUME: 0..100, UP, DOWN, MAX, MUTE, UNMUTE. For SCREEN_UI_CONTROL: CLICK, TYPE, TAP_XY, SCROLL_DOWN, SCROLL_UP, SWIPE_LEFT, SWIPE_RIGHT, BACK, HOME, RECENTS, READ_SCREEN."))
                        })
                        put("required", JSONArray().put("action"))
                    })
                }
            ).put(installAppDecl()).put(backgroundWebSearchDecl()).put(makeImageWithGeminiDecl()).put(bioscienceSkillDecl()).put(generateMusicLyriaDecl()).put(setAbilitySwitchDecl()).put(confirmPendingSaveDecl()).put(saveUserMemoryDecl()).put(appControlDecl()).put(writeCodeDecl()).put(sendGeneratedFileDecl()).put(workflowPlaybookDecl()).put(uiMemoryDecl()).put(findAppDecl()).put(deepThinkDecl()).put(proactiveWatchDecl()).put(switchPersonalityDecl()).put(switchVoiceOrMicPowerDecl()).put(powerOffSelfDecl()).put(understandAppOrWebsiteDecl()).put(selfHealOrUpdateDecl()))))
            put("inputAudioTranscription", JSONObject())
            put("outputAudioTranscription", JSONObject())
        })
    }.toString()

    fun refreshExportConfig(customApiKey: String) {
        val cleanCustom = customApiKey.trim()
        var firebaseKey = ""
        var projectId = ""
        var appId = ""
        try {
            val opts = FirebaseApp.getInstance().options
            firebaseKey = opts.apiKey.orEmpty()
            projectId = opts.projectId.orEmpty()
            appId = opts.applicationId.orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp options read warning: ${e.message}")
        }

        val effectiveKey = cleanCustom.ifBlank { firebaseKey }
        val pkgName = context.packageName
        val wsBase =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        val fullWs = if (effectiveKey.isNotBlank()) {
            "$wsBase?key=$effectiveKey"
        } else {
            "$wsBase?key=YOUR_GEMINI_LIVE_API_KEY"
        }

        val setupJson = buildSetupJson()

        val toolsJson = JSONObject().apply {
            put(
                "functionDeclarations",
                JSONArray().apply {
                    put(JSONObject().put("name", "phone_action").put("description", "Control apps, settings, volume, contacts, and 4-directional screen UI touch"))
                }
            )
        }.toString(2)

        val frameSample = JSONObject().apply {
            put(
                "realtimeInput",
                JSONObject().apply {
                    put(
                        "audio",
                        JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", "<BASE64_16KHZ_MONO_PCM_AUDIO>")
                        }
                    )
                }
            )
        }.toString(2)

        val newExport = MultimodalLiveExportConfig(
            builtInFirebaseApiKey = firebaseKey,
            customUserApiKey = cleanCustom,
            effectiveApiKey = effectiveKey,
            projectId = projectId,
            applicationId = appId,
            packageName = pkgName,
            websocketBidiEndpoint = wsBase,
            fullWebsocketUrlWithKey = fullWs,
            activeLiveModel = wsBidiModel,
            setupHandshakeJson = setupJson,
            setupPayloadJson = setupJson,
            toolsDeclarationJson = toolsJson,
            realtimeFramePayloadSample = frameSample,
            pythonLiveClientCode = ""
        )

        _state.value = _state.value.copy(exportConfig = newExport)
    }

    fun copyToClipboard(label: String, text: String): String {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            _state.value = _state.value.copy(lastCopiedLabel = label)
            "জি স্যার, '$label' ক্লিপবোর্ডে কপি করা হয়েছে!"
        } catch (e: Exception) {
            "কপি করতে সমস্যা হয়েছে: ${e.message}"
        }
    }

    fun setEngineMode(engine: LiveConnectionEngine) {
        _state.value = _state.value.copy(
            connectionEngine = engine,
            engineMode = engine,
            statusMessageBn = engine.labelBn
        )
    }

    fun updateLatestCameraFrame(bitmap: Bitmap) {
        latestCameraBitmap = bitmap
        if (_state.value.isLiveActive && _state.value.isCameraStreamEnabled) {
            val nextCam = _state.value.streamedCameraFrames + 1
            val totalFrames = nextCam + _state.value.streamedScreenFrames
            _state.value = _state.value.copy(
                streamedCameraFrames = nextCam,
                framesSentCount = totalFrames
            )
        }
    }

    fun updateLatestScreenFrame(bitmap: Bitmap) {
        latestScreenBitmap = bitmap
        if (_state.value.isLiveActive && _state.value.isScreenStreamEnabled) {
            val nextScr = _state.value.streamedScreenFrames + 1
            val totalFrames = _state.value.streamedCameraFrames + nextScr
            _state.value = _state.value.copy(
                streamedScreenFrames = nextScr,
                framesSentCount = totalFrames
            )
        }
    }

    fun startLiveSession(customApiKey: String) {
        LiveKeepAliveService.start(context)
        ExternalMicArbiter.init(context)
        ExternalMicArbiter.setGeminiLiveOwningMic(true)
        modelIndex = 0
        reconnectAttempts = 0
        setupDone = false
        refreshExportConfig(customApiKey)
        ScreenMemory.load(context)
        _state.value = _state.value.copy(
            isScreenStreamEnabled = true,
            isLiveActive = true,
            isMicMuted = false,
            connectionStatusLabel = "CONNECTING • GEMINI LIVE",
            activeModelName = wsBidiModel,
            statusMessageBn = "Gemini Live কানেক্ট হচ্ছে..."
        )

        scope.launch {
            // Ask Google which models this key can use and take the most powerful live model first
            val resolved = withTimeoutOrNull(5000L) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { GeminiModelResolver.resolve(currentApiKey()) }
            }
            if (resolved != null && resolved.live.isNotEmpty()) {
                liveModelCandidates = (resolved.live + liveModelCandidates).distinct()
                modelIndex = 0
                _state.value = _state.value.copy(activeModelName = wsBidiModel)
            }
            connectDirectBidiWebSocketIfPossible()
        }
        startFrameCapturePump()
        startAudioPlaybackPump()
        startRealTimeAudioRecordPump()
    }

    fun stopLiveSession(customStatusBn: String = "গ্লোবে ট্যাপ করে কথা বলুন") {
        LiveKeepAliveService.stop(context)
        ExternalMicArbiter.setGeminiLiveOwningMic(false)
        framePumpJob?.cancel()
        framePumpJob = null
        audioRecordPumpJob?.cancel()
        audioRecordPumpJob = null
        audioPlaybackPumpJob?.cancel()
        audioPlaybackPumpJob = null
        stopAndFlushLiveAudioTrack()
        disconnectWebSocket()
        _state.value = _state.value.copy(
            isLiveActive = false,
            isWebSocketConnected = false,
            isAgentSpeakingLive = false,
            isUserSpeakingBargeIn = false,
            liveRmsLevel = 0f,
            connectionStatusLabel = "OFFLINE • TAP GLOBE OR CALL NAME",
            liveStreamingPartialReply = "",
            statusMessageBn = customStatusBn
        )
    }

    fun toggleMicMute(): Boolean {
        val next = !_state.value.isMicMuted
        _state.value = _state.value.copy(
            isMicMuted = next,
            liveRmsLevel = if (next) 0f else _state.value.liveRmsLevel,
            statusMessageBn = if (next) "মাইক্রোফোন মিউট করা হয়েছে — গ্লোবে ট্যাপ করে আনমিউট করুন" else "শুনছি… কথা বলুন"
        )
        return next
    }

    fun setMicMuted(muted: Boolean) {
        _state.value = _state.value.copy(
            isMicMuted = muted,
            liveRmsLevel = if (muted) 0f else _state.value.liveRmsLevel,
            statusMessageBn = if (muted) "মাইক্রোফোন পজ করা হয়েছে — গ্লোবে ট্যাপ করে কথা বলুন" else "শুনছি… কথা বলুন"
        )
    }

    fun setSpeakerMuted(muted: Boolean) {
        if (_state.value.isSpeakerMuted != muted) toggleSpeakerMute()
    }

    fun toggleSpeakerMute(): Boolean {
        val next = !_state.value.isSpeakerMuted
        if (next) {
            stopAndFlushLiveAudioTrack()
        }
        _state.value = _state.value.copy(
            isSpeakerMuted = next,
            isAgentSpeakingLive = if (next) false else _state.value.isAgentSpeakingLive,
            statusMessageBn = if (next) "স্পিকার মিউট করা হয়েছে" else "স্পিকার চালু করা হয়েছে"
        )
        return next
    }

    fun toggleCameraStream(): Boolean {
        val next = !_state.value.isCameraStreamEnabled
        _state.value = _state.value.copy(
            isCameraStreamEnabled = next,
            statusMessageBn = if (next) "লাইভ ক্যামেরা ভিশন চালু হয়েছে" else "লাইভ ক্যামেরা ভিশন বন্ধ করা হয়েছে"
        )
        return next
    }

    fun enableScreenStreamNow() {
        _state.value = _state.value.copy(
            isScreenStreamEnabled = true,
            statusMessageBn = "মাল্টিমোডাল লাইভ স্ক্রিন ভিশন সচল রয়েছে — এজেন্ট প্রতি মিলিসেকেন্ডে আপনার স্ক্রিন দেখছে"
        )
        ScreenCaptureSessionBus.captureBestAvailableScreenFrame { bmp ->
            lastFrameForBrain = bmp
            updateLatestScreenFrame(bmp)
            if (_state.value.isLiveActive && _state.value.isWebSocketConnected) {
                sendRealtimeImageFrameOverWs(bmp)
            }
        }
    }

    fun disableScreenStreamNow() {
        _state.value = _state.value.copy(
            isScreenStreamEnabled = false,
            statusMessageBn = "লাইভ স্ক্রিন ভিশন আপাতত পজ করা হয়েছে"
        )
    }

    fun toggleScreenStream(): Boolean {
        val next = !_state.value.isScreenStreamEnabled
        _state.value = _state.value.copy(
            isScreenStreamEnabled = next,
            statusMessageBn = if (next) "মাল্টিমোডাল লাইভ স্ক্রিন ভিশন সচল রয়েছে" else "লাইভ স্ক্রিন ভিশন পজ করা হয়েছে"
        )
        if (next) {
            if (!ScreenCaptureSessionBus.state.value.hasMediaProjectionGrant) {
                ScreenCaptureSessionBus.requestOneTimeScreenCapturePermission()
            }
            ScreenCaptureSessionBus.captureBestAvailableScreenFrame { bmp ->
                lastFrameForBrain = bmp
                updateLatestScreenFrame(bmp)
                if (_state.value.isLiveActive && _state.value.isWebSocketConnected) {
                    sendRealtimeImageFrameOverWs(bmp)
                }
            }
        }
        return next
    }

    fun toggleToolsArmed(): Boolean {
        val next = !_state.value.isRealTimeToolsArmed
        _state.value = _state.value.copy(
            isRealTimeToolsArmed = next,
            isToolsArmed = next
        )
        return next
    }

    /**
     * Barge-in (Instant Interruption):
     * Immediately halts any ongoing AI audio stream (`AudioTrack` flush + stop), clears the
     * playback queue, and switches to listening for the user's new speech.
     */
    fun triggerBargeInInterrupt() {
        stopAndFlushLiveAudioTrack()
        _state.value = _state.value.copy(
            isAgentSpeakingLive = false,
            isUserSpeakingBargeIn = true,
            liveRmsLevel = 0.15f,
            bargeInInterruptCount = _state.value.bargeInInterruptCount + 1,
            liveStreamingPartialReply = "",
            statusMessageBn = "শুনছি… বলুন স্যার"
        )
        onBargeInInterruptRequested()
    }

    fun notifyAgentSpeechState(isSpeaking: Boolean) {
        if (_state.value.isAgentSpeakingLive != isSpeaking) {
            _state.value = _state.value.copy(
                isAgentSpeakingLive = isSpeaking,
                isUserSpeakingBargeIn = if (isSpeaking) false else _state.value.isUserSpeakingBargeIn
            )
        }
    }

    fun recordLiveToolExecution(summaryBn: String) {
        _state.value = _state.value.copy(
            executedLiveActionsCount = _state.value.executedLiveActionsCount + 1,
            lastLiveToolSummary = summaryBn,
            lastToolExecuted = summaryBn
        )
    }

    /**
     * Dedicated non-blocking 24kHz PCM playback pump so OkHttp's WebSocket reader thread
     * never blocks inside `AudioTrack.write()` and can process `interrupted` events in < 1ms.
     */
    private fun startAudioPlaybackPump() {
        audioPlaybackPumpJob?.cancel()
        audioPlaybackPumpJob = scope.launch {
            while (isActive && _state.value.isLiveActive) {
                val chunk = pcmPlaybackQueue.poll()
                if (chunk == null) {
                    if (_state.value.isAgentSpeakingLive &&
                        System.currentTimeMillis() - lastAudioChunkPlayedAtMs > 280L
                    ) {
                        _state.value = _state.value.copy(
                            isAgentSpeakingLive = false,
                            liveRmsLevel = 0.06f
                        )
                    }
                    delay(12L)
                    continue
                }

                if (_state.value.isSpeakerMuted) {
                    pcmPlaybackQueue.clear()
                    continue
                }

                val nowMs = System.currentTimeMillis()
                if (!_state.value.isAgentSpeakingLive || nowMs - lastRmsUiUpdateAtMs >= 160L) {
                    lastRmsUiUpdateAtMs = nowMs
                    val outRms = computePcm16LeRmsNormalized(chunk)
                    _state.value = _state.value.copy(
                        isAgentSpeakingLive = true,
                        isUserSpeakingBargeIn = false,
                        liveRmsLevel = outRms.coerceIn(0.18f, 0.98f)
                    )
                    FloatingAvatarOverlayBus.updateSpeakingPulse(true)
                }

                try {
                    val track = ensureAudioTrackInitialized()
                    if (track != null) {
                        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            routeAudioToMainLoudspeaker(track)
                            track.play()
                        }
                        val processedChunk = UnifiedAudioRouterBus.processOutputPcm24kHz(chunk)
                        track.write(processedChunk, 0, processedChunk.size)
                        lastAudioChunkPlayedAtMs = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "AudioTrack write warning: ${e.message}")
                }
            }
        }
    }

    private fun computePcm16LeRmsNormalized(pcmBytes: ByteArray): Float {
        val sampleCount = pcmBytes.size / 2
        if (sampleCount <= 0) return 0.2f
        var sumSq = 0.0
        var i = 0
        val step = 4 // sample every 4th short for fast calculation
        var counted = 0
        while (i + 1 < pcmBytes.size) {
            val lo = pcmBytes[i].toInt() and 0xFF
            val hi = pcmBytes[i + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toDouble()
            sumSq += sample * sample
            counted++
            i += 2 * step
        }
        if (counted == 0) return 0.2f
        val rms = sqrt(sumSq / counted)
        return (rms / 6500.0).toFloat().coerceIn(0.12f, 1.0f)
    }

    @SuppressLint("MissingPermission")
    private fun startRealTimeAudioRecordPump() {
        audioRecordPumpJob?.cancel()
        audioRecordPumpJob = scope.launch {
            var tick = 0
            while (isActive && _state.value.isLiveActive) {
                tick++
                val extMicState = ExternalMicArbiter.state.value
                if (extMicState.isExternalMicBusy) {
                    val yieldMsg = extMicState.busyReasonBn
                        .ifBlank { "অন্য জায়গায় মাইক ব্যবহৃত হচ্ছে — কাজ শেষ হলেই অটো সক্রিয় হবে" }
                    if (_state.value.statusMessageBn != yieldMsg) {
                        _state.value = _state.value.copy(
                            statusMessageBn = yieldMsg,
                            liveRmsLevel = 0f
                        )
                    }
                    delay(250L)
                    continue
                }

                val ready = _state.value.isWebSocketConnected && setupDone && !_state.value.isMicMuted
                val hasMicPerm = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!ready || !hasMicPerm) {
                    delay(180L)
                    continue
                }

                val sampleRate = 16000
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(3200)
                val recorder = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                    )
                } catch (e: Exception) {
                    delay(450L)
                    continue
                }
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    try { recorder.release() } catch (_: Exception) {}
                    delay(450L)
                    continue
                }

                val sessionId = recorder.audioSessionId
                ExternalMicArbiter.registerOwnLiveAudioSession(sessionId)
                UnifiedAudioRouterBus.attachAndRouteAudioRecord(recorder)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        recorder.setPreferredMicrophoneDirection(
                            android.media.MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER
                        )
                        recorder.setPreferredMicrophoneFieldDimension(0.15f)
                    }
                }

                var aec: android.media.audiofx.AcousticEchoCanceler? = null
                var ns: android.media.audiofx.NoiseSuppressor? = null
                var agc: android.media.audiofx.AutomaticGainControl? = null
                try {
                    if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                        aec = android.media.audiofx.AcousticEchoCanceler.create(sessionId)?.also { it.enabled = true }
                    }
                    if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                        ns = android.media.audiofx.NoiseSuppressor.create(sessionId)?.also { it.enabled = true }
                    }
                    if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
                        agc = android.media.audiofx.AutomaticGainControl.create(sessionId)?.also {
                            // Disable AGC boost when background noise cancellation is ON so distant voices aren't amplified
                            it.enabled = !AgentAbilitySwitchesBus.state.value.ignoreBackgroundAudio
                        }
                    }
                } catch (_: Exception) {}
                val aecActive = aec?.enabled == true

                val buf = ShortArray(800) // 50ms @16kHz
                var loudFrames = 0
                var mediaCheckCounter = 0
                var mediaPlayingNow = false
                var prevRawSample = 0.0
                var prevHpOut = 0.0
                var ambientNoiseFloorRms = 180.0
                var nearFieldVoiceHoldFrames = 0
                try {
                    recorder.startRecording()
                    if (_state.value.statusMessageBn.contains("অন্য")) {
                        _state.value = _state.value.copy(statusMessageBn = "শুনছি… কথা বলুন")
                    }
                    while (isActive && _state.value.isLiveActive && _state.value.isWebSocketConnected &&
                        setupDone && !_state.value.isMicMuted && !ExternalMicArbiter.state.value.isExternalMicBusy
                    ) {
                        val n = recorder.read(buf, 0, buf.size)
                        if (n <= 0) {
                            delay(10L)
                            continue
                        }
                        val noiseCancelLock = AgentAbilitySwitchesBus.state.value.ignoreBackgroundAudio
                        if (mediaCheckCounter++ % 20 == 0) {
                            mediaPlayingNow = AgentAbilitySwitchesBus.isDeviceMediaPlaying(context)
                            runCatching {
                                agc?.enabled = !noiseCancelLock
                            }
                        }
                        val ignoreBgMedia = noiseCancelLock && mediaPlayingNow

                        var rawSumSq = 0.0
                        for (i in 0 until n) {
                            val v = buf[i].toDouble()
                            rawSumSq += v * v
                        }
                        val rawRms = sqrt(rawSumSq / n)

                        // Track adaptive room noise & background chatter floor
                        if (rawRms < ambientNoiseFloorRms * 1.45) {
                            ambientNoiseFloorRms = (ambientNoiseFloorRms * 0.92 + rawRms * 0.08).coerceIn(90.0, 480.0)
                        } else {
                            ambientNoiseFloorRms = (ambientNoiseFloorRms * 0.992 + rawRms * 0.008).coerceIn(90.0, 480.0)
                        }

                        // Near-Field Primary Speaker Voice Gate:
                        // When 'Ignore background audio' is ON, reject distant room chatter, side conversations, and media bleed.
                        val nearFieldGateRms = when {
                            ignoreBgMedia -> 880.0
                            noiseCancelLock -> (ambientNoiseFloorRms * 2.35).coerceIn(500.0, 760.0)
                            else -> 24.0
                        }
                        if (rawRms >= nearFieldGateRms) {
                            nearFieldVoiceHoldFrames = 6 // ~300ms hold so brief pauses between the user's words aren't clipped
                        } else if (nearFieldVoiceHoldFrames > 0) {
                            nearFieldVoiceHoldFrames--
                        }

                        // Studio-Clarity Voice DSP + True Background Chatter & Noise Cancellation Gate
                        val userBoost = micSensitivityBoost.coerceIn(1.2f, 4.5f).toDouble()
                        val adaptiveWhisperFactor = when {
                            noiseCancelLock && nearFieldVoiceHoldFrames == 0 -> 0.015 // Silence background people talking & room noise!
                            ignoreBgMedia -> 0.72 // Attenuate ambient media while user speaks over video/music
                            noiseCancelLock -> 1.15 // Clean near-field primary user voice without boosting background room reflections
                            rawRms < 24.0 -> 0.35 // Noise gate for pure background hiss/rumble
                            !_state.value.isAgentSpeakingLive && rawRms in 24.0..1350.0 -> 1.65
                            !_state.value.isAgentSpeakingLive && rawRms in 1350.0..2800.0 -> 1.32
                            else -> 1.08
                        }
                        val effectiveGain = if (noiseCancelLock && nearFieldVoiceHoldFrames == 0) {
                            0.015
                        } else {
                            (userBoost * adaptiveWhisperFactor).coerceIn(0.35, 5.6)
                        }

                        var boostedSumSq = 0.0
                        val bb = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until n) {
                            val x = buf[i].toDouble()
                            // Stronger high-pass filter when noise cancellation is ON to strip room rumble & distant bass
                            val hpCoeff = if (noiseCancelLock) 0.91 else 0.95
                            val hp = hpCoeff * (prevHpOut + x - prevRawSample)
                            // Pre-emphasis boosts near-field speech consonant clarity (2kHz-5kHz)
                            val claritySample = hp + 0.30 * (x - prevRawSample)
                            prevRawSample = x
                            prevHpOut = hp

                            val scaled = claritySample * effectiveGain
                            // Soft-knee saturation prevents harsh digital clipping on loud syllables
                            val limited = when {
                                scaled > 28000.0 -> 28000.0 + (scaled - 28000.0) * 0.25
                                scaled < -28000.0 -> -28000.0 + (scaled + 28000.0) * 0.25
                                else -> scaled
                            }
                            val amplified = limited.toInt().coerceIn(-32760, 32760).toShort()
                            val d = amplified.toDouble()
                            boostedSumSq += d * d
                            bb.putShort(amplified)
                        }

                        val rms = sqrt(boostedSumSq / n)
                        val nowMs = System.currentTimeMillis()
                        if (!_state.value.isAgentSpeakingLive && nowMs - lastRmsUiUpdateAtMs >= 180L) {
                            lastRmsUiUpdateAtMs = nowMs
                            val normMicRms = (rms / 2600.0).toFloat().coerceIn(0.06f, 1.0f)
                            _state.value = _state.value.copy(liveRmsLevel = normMicRms)
                        }

                        // Instant Gemini Assistant-style local barge-in when user speaks while agent is talking
                        val baseBargeThreshold = when {
                            UnifiedAudioRouterBus.isHeadsetCurrentlyActive() -> 920.0
                            aecActive -> 1480.0
                            else -> 3400.0
                        }
                        val bargeThreshold = if (ignoreBgMedia) baseBargeThreshold + 2100.0 else baseBargeThreshold
                        if (_state.value.isAgentSpeakingLive && rms > bargeThreshold) {
                            loudFrames++
                            if (loudFrames >= 2) {
                                loudFrames = 0
                                localBargeIn()
                            }
                        } else {
                            loudFrames = 0
                        }

                        sendRealtimeAudioChunkOverWs(bb.array())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Mic stream paused: ${e.message}")
                } finally {
                    ExternalMicArbiter.unregisterOwnLiveAudioSession(sessionId)
                    UnifiedAudioRouterBus.attachAndRouteAudioRecord(null)
                    try { recorder.stop() } catch (_: Exception) {}
                    try { recorder.release() } catch (_: Exception) {}
                    try { aec?.release() } catch (_: Exception) {}
                    try { ns?.release() } catch (_: Exception) {}
                    try { agc?.release() } catch (_: Exception) {}
                }
            }
        }
    }

    private fun localBargeIn() {
        stopAndFlushLiveAudioTrack()
        _state.value = _state.value.copy(
            isAgentSpeakingLive = false,
            isUserSpeakingBargeIn = true,
            bargeInInterruptCount = _state.value.bargeInInterruptCount + 1
        )
        onBargeInInterruptRequested()
    }

    private fun startFrameCapturePump() {
        framePumpJob?.cancel()
        framePumpJob = scope.launch {
            while (isActive && _state.value.isLiveActive) {
                if (_state.value.isScreenStreamEnabled) {
                    // Refresh UI tree snapshot (debounced inside CubeAccessibilityService) and stream real frames smoothly
                    try {
                        AccessibilityAutomationBus.serviceInstance?.refreshSnapshotNow(force = false)
                    } catch (_: Exception) {}

                    ScreenCaptureSessionBus.captureBestAvailableScreenFrame { bmp ->
                        lastFrameForBrain = bmp
                        updateLatestScreenFrame(bmp)
                        if (_state.value.isWebSocketConnected && setupDone) {
                            val sig = frameSignature(bmp)
                            val now = System.currentTimeMillis()
                            val prev = lastSentSig
                            val changed = prev == null || sigDiff(prev, sig) > 2
                            if (changed || now - lastSentAt > 650L) {
                                lastSentSig = sig
                                lastSentAt = now
                                sendRealtimeImageFrameOverWs(bmp)
                            }
                        }
                    }
                }
                if (_state.value.isCameraStreamEnabled && _state.value.isWebSocketConnected) {
                    latestCameraBitmap?.let { camBmp -> sendRealtimeImageFrameOverWs(camBmp) }
                }
                delay(320L)
            }
        }
    }

    private fun frameSignature(bmp: Bitmap): IntArray {
        val t = Bitmap.createScaledBitmap(bmp, 12, 12, false)
        val out = IntArray(144)
        for (y in 0 until 12) for (x in 0 until 12) {
            val c = t.getPixel(x, y)
            out[y * 12 + x] = ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
        }
        if (t !== bmp) t.recycle()
        return out
    }

    private fun sigDiff(a: IntArray, b: IntArray): Int {
        var sum = 0L
        for (i in a.indices) sum += kotlin.math.abs(a[i] - b[i])
        return (sum / a.size).toInt()
    }

    /** After every action: push 3 fresh real screen frames right away at millisecond speed so Gemini verifies the outcome. */
    private fun forceFrameBurst() {
        if (!_state.value.isScreenStreamEnabled) {
            return
        }
        scope.launch {
            repeat(3) {
                if (!_state.value.isScreenStreamEnabled) {
                    return@launch
                }
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        AccessibilityAutomationBus.serviceInstance?.refreshSnapshotNow()
                    }
                } catch (_: Exception) {}
                ScreenCaptureSessionBus.captureBestAvailableScreenFrame { bmp ->
                    lastFrameForBrain = bmp
                    updateLatestScreenFrame(bmp)
                    lastSentSig = frameSignature(bmp)
                    lastSentAt = System.currentTimeMillis()
                    if (_state.value.isWebSocketConnected && setupDone) {
                        sendRealtimeImageFrameOverWs(bmp)
                    }
                }
                delay(180L)
            }
        }
    }

    private fun connectDirectBidiWebSocketIfPossible() {
        val url = _state.value.exportConfig.fullWebsocketUrlWithKey
        if (url.isBlank() || url.endsWith("YOUR_GEMINI_LIVE_API_KEY")) {
            _state.value = _state.value.copy(
                statusMessageBn = "উপরে চাবি (Key) আইকনে ট্যাপ করে আপনার Gemini API Key দিন"
            )
            onLiveSpokenChunkOrReply("স্যার, উপরে বাম পাশের চাবি আইকনে ট্যাপ করে আপনার Gemini API Key দিন।", false)
            return
        }
        scope.launch {
            disconnectWebSocket()
            setupDone = false
            val request = Request.Builder().url(url).build()
            activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    activeWebSocket = webSocket
                    _state.value = _state.value.copy(
                        isWebSocketConnected = true,
                        connectionStatusLabel = "CONNECTING • $wsBidiModel",
                        activeModelName = wsBidiModel
                    )
                    webSocket.send(buildSetupJson())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.isNotBlank()) handleIncomingWebSocketJson(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val t = try { bytes.utf8() } catch (_: Exception) { "" }
                    if (t.trimStart().startsWith("{")) handleIncomingWebSocketJson(t)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    handleSocketDown(webSocket, "${t.message ?: "network error"}")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    try { webSocket.close(code, reason) } catch (_: Exception) {}
                    handleSocketDown(webSocket, "$code $reason")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    handleSocketDown(webSocket, "$code $reason")
                }
            })
        }
    }

    private fun handleSocketDown(ws: WebSocket, reason: String) {
        if (ws !== activeWebSocket) return
        val wasSetup = setupDone
        setupDone = false
        activeWebSocket = null
        _state.value = _state.value.copy(
            isWebSocketConnected = false,
            isAgentSpeakingLive = false,
            liveRmsLevel = 0f
        )
        stopAndFlushLiveAudioTrack()
        FloatingAvatarOverlayBus.updateSpeakingPulse(false)
        if (!_state.value.isLiveActive) return
        Log.w(TAG, "Live socket down (setup=$wasSetup): $reason")
        AgentAbilitySwitchesBus.logCrashOrFailureReport(context, "LiveWebSocket", "setup=$wasSetup, reason=$reason")
        scope.launch {
            if (!wasSetup && modelIndex < liveModelCandidates.size - 1) {
                modelIndex++
            } else if (reconnectAttempts >= 6) {
                val errMsg = "লাইভ সংযোগ হয়নি: ${reason.take(65)} (API Key / ইন্টারনেট চেক করুন)"
                _state.value = _state.value.copy(statusMessageBn = errMsg)
                onLiveSpokenChunkOrReply(errMsg, false)
                return@launch
            } else {
                reconnectAttempts++
            }
            _state.value = _state.value.copy(statusMessageBn = "পুনরায় সংযোগ হচ্ছে…")
            delay(700L)
            if (_state.value.isLiveActive && activeWebSocket == null) {
                connectDirectBidiWebSocketIfPossible()
            }
        }
    }

    private fun handleIncomingWebSocketJson(rawJson: String) {
        try {
            val root = JSONObject(rawJson)

            if (root.has("setupComplete")) {
                setupDone = true
                reconnectAttempts = 0
                _state.value = _state.value.copy(
                    connectionStatusLabel = "LIVE • ${wsBidiModel.removePrefix("models/")}",
                    statusMessageBn = "শুনছি… সরাসরি কথা বলুন"
                )
                pendingTextTurn?.let { queuedText ->
                    pendingTextTurn = null
                    sendClientContentTurnOverWs(queuedText)
                }
                return
            }
            root.optJSONObject("toolCall")?.let { handleToolCall(it); return }
            if (root.has("goAway")) {
                activeWebSocket?.let { handleSocketDown(it, "goAway") }
                return
            }

            val sc = root.optJSONObject("serverContent") ?: return

            if (sc.optBoolean("interrupted", false)) {
                stopAndFlushLiveAudioTrack()
                modelTurnBuf.setLength(0)
                _state.value = _state.value.copy(
                    isAgentSpeakingLive = false,
                    isUserSpeakingBargeIn = true,
                    liveRmsLevel = 0.12f,
                    bargeInInterruptCount = _state.value.bargeInInterruptCount + 1,
                    statusMessageBn = "শুনছি… বলুন স্যার"
                )
                onBargeInInterruptRequested()
                return
            }

            sc.optJSONObject("inputTranscription")?.optString("text", "")?.takeIf { it.isNotBlank() }?.let { chunk ->
                userTurnBuf.append(chunk)
                val fullUserText = userTurnBuf.toString().trim()
                onUserTranscript(fullUserText)
                checkIfUserAskedForScreenShare(fullUserText)
                checkIfUserAskedForPowerOff(fullUserText)
            }
            sc.optJSONObject("outputTranscription")?.optString("text", "")?.takeIf { it.isNotBlank() }?.let { chunk ->
                modelTurnBuf.append(chunk)
                val fullModelText = modelTurnBuf.toString().trim()
                _state.value = _state.value.copy(liveStreamingPartialReply = fullModelText)
                onLiveSpokenChunkOrReply(fullModelText, false)
            }

            val parts = sc.optJSONObject("modelTurn")?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val partObj = parts.optJSONObject(i) ?: continue
                    val partText = partObj.optString("text", "").trim()
                    if (partText.isNotBlank() && !partObj.optBoolean("thought", false)) {
                        if (modelTurnBuf.isEmpty()) {
                            modelTurnBuf.append(partText)
                            _state.value = _state.value.copy(liveStreamingPartialReply = partText)
                            onLiveSpokenChunkOrReply(partText, false)
                        }
                    }
                    val inline = partObj.optJSONObject("inlineData")
                    if (inline != null) {
                        val b64 = inline.optString("data", "")
                        if (inline.optString("mimeType", "").startsWith("audio/pcm") && b64.isNotBlank()) {
                            receivedAudioInTurn = true
                            enqueuePcm24kHzAudioChunk(b64)
                        }
                    }
                }
            }

            if (sc.optBoolean("turnComplete", false)) {
                val finalUser = userTurnBuf.toString().trim()
                val finalModel = modelTurnBuf.toString().trim()
                val hadPcmAudio = receivedAudioInTurn
                val hadToolCall = toolCalledInCurrentTurn
                receivedAudioInTurn = false
                toolCalledInCurrentTurn = false
                userTurnBuf.setLength(0)
                modelTurnBuf.setLength(0)
                if (finalUser.isNotBlank()) {
                    onUserTurnCompleteForMemory?.invoke(finalUser, finalModel)
                }
                if (finalModel.isNotBlank()) {
                    if (!hadPcmAudio && !_state.value.isSpeakerMuted) {
                        onLiveSpokenChunkOrReply(finalModel, true)
                    }
                    // Only run fallback tag parser if no toolCall already executed in this turn (or if explicit tag/code block is present)
                    if (!hadToolCall || finalModel.contains("[[ACTION:") || finalModel.contains("```")) {
                        onExecuteLiveActionTag(finalUser, finalModel)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "WS JSON parse warning: ${e.message}")
        }
    }

    @Volatile
    private var receivedAudioInTurn = false

    @Volatile
    private var toolCalledInCurrentTurn = false

    private fun checkIfUserAskedForScreenShare(transcript: String) {
        val lower = transcript.lowercase()
        val asksScreenShareOn = (lower.contains("স্ক্রিন শেয়ার") ||
            lower.contains("সক্রিন সেয়ার") ||
            lower.contains("সক্রিন শেয়ার") ||
            lower.contains("screen share")) &&
            (lower.contains("অন") || lower.contains("চালু") || lower.contains("একটিভ") || lower.contains("on") || lower.contains("start"))
        if (asksScreenShareOn && !_state.value.isScreenStreamEnabled) {
            if (ScreenCaptureSessionBus.state.value.hasMediaProjectionGrant) {
                enableScreenStreamNow()
            } else {
                ScreenCaptureSessionBus.requestOneTimeScreenCapturePermission()
            }
        }
    }

    private fun checkIfUserAskedForPowerOff(transcript: String) {
        val lower = transcript.lowercase().trim()
        val isExplicitPowerOff = lower.contains("অফ হয়ে যা") ||
            lower.contains("অফ হয়ে যা") ||
            lower.contains("অফ হও") ||
            lower.contains("বন্ধ হয়ে যা") ||
            lower.contains("বন্ধ হয়ে যা") ||
            lower.contains("বন্ধ হও") ||
            lower.contains("একদম বন্ধ") ||
            lower.contains("একদম অফ") ||
            lower.contains("নিজেকে অফ কর") ||
            lower.contains("নিজেকে বন্ধ কর") ||
            lower.contains("তুমি এখন অফ হও") ||
            lower.contains("তুমি বন্ধ হয়ে যাও") ||
            lower.contains("তুমি বন্ধ হয়ে যাও") ||
            lower.contains("turn yourself off") ||
            lower.contains("turn off completely") ||
            lower.contains("shut down completely") ||
            lower == "অফ" ||
            lower == "বন্ধ" ||
            lower == "turn off"
        if (isExplicitPowerOff) {
            AccessibilityAutomationBus.cancelAllPendingMacros()
            stopAndFlushLiveAudioTrack()
            scope.launch {
                try {
                    namedToolHandler?.invoke(
                        "power_off_self",
                        JSONObject().put("farewell_message", "জি স্যার, আমি এখন সম্পূর্ণ অফ হয়ে যাচ্ছি।")
                    )
                } catch (_: Exception) {}
            }
        }
    }

    /** Gemini asked to run a phone action -> execute it on-device and send the result back. */
    private fun handleToolCall(tc: JSONObject) {
        val calls = tc.optJSONArray("functionCalls") ?: return
        toolCalledInCurrentTurn = true
        scope.launch {
            val responses = JSONArray()
            for (i in 0 until calls.length()) {
                val c = calls.optJSONObject(i) ?: continue
                val args = c.optJSONObject("args") ?: JSONObject()
                val toolName = c.optString("name")
                val tag = "[[ACTION:${args.optString("action").uppercase()}|${args.optString("target")}|${args.optString("extra")}]]"
                val result = try {
                    withTimeout(25_000L) {
                        if (toolName == "phone_action") toolCallHandler?.invoke(tag) ?: "no handler"
                        else namedToolHandler?.invoke(toolName, args) ?: "no handler"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AgentAbilitySwitchesBus.logCrashOrFailureReport(context, "ToolCall:$toolName", e.message ?: "error")
                    "error: ${e.message}"
                }
                recordLiveToolExecution("${c.optString("name")} ${args.optString("action")}: ${result.take(60)}")
                responses.put(JSONObject().apply {
                    put("id", c.optString("id"))
                    put("name", c.optString("name"))
                    put("response", JSONObject().put("result", result.take(1500)))
                })
            }
            forceFrameBurst()
            try {
                activeWebSocket?.send(JSONObject().put("toolResponse",
                    JSONObject().put("functionResponses", responses)).toString())
            } catch (_: Exception) {}
        }
    }

    private fun enqueuePcm24kHzAudioChunk(base64Pcm: String) {
        if (_state.value.isSpeakerMuted) return
        try {
            val pcmBytes = Base64.decode(base64Pcm, Base64.DEFAULT)
            if (pcmBytes.isNotEmpty()) {
                lastAudioChunkPlayedAtMs = System.currentTimeMillis()
                totalStreamedAudioPackets++
                if (!_state.value.isAgentSpeakingLive || totalStreamedAudioPackets % 8 == 0) {
                    _state.value = _state.value.copy(
                        isAgentSpeakingLive = true,
                        isUserSpeakingBargeIn = false,
                        streamedAudioPackets = totalStreamedAudioPackets
                    )
                }
                pcmPlaybackQueue.offer(pcmBytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "PCM decode skip: ${e.message}")
        }
    }

    private fun routeAudioToMainLoudspeaker(track: AudioTrack?) {
        UnifiedAudioRouterBus.attachAndRouteAudioTrack(track)
    }

    private fun ensureAudioTrackInitialized(): AudioTrack? {
        synchronized(audioTrackLock) {
            var track = liveOutputAudioTrack
            if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                val sampleRate = 24000
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4800)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                try {
                    track.setVolume(AudioTrack.getMaxVolume())
                } catch (_: Exception) {
                }
                routeAudioToMainLoudspeaker(track)
                liveOutputAudioTrack = track
            }
            return track
        }
    }

    fun stopAndFlushLiveAudioTrack() {
        pcmPlaybackQueue.clear()
        synchronized(audioTrackLock) {
            val track = liveOutputAudioTrack ?: return
            try {
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun sendRealtimeAudioChunkOverWs(pcm16kBytes: ByteArray) {
        val ws = activeWebSocket ?: return
        if (!setupDone || pcm16kBytes.isEmpty()) return
        try {
            val b64 = Base64.encodeToString(pcm16kBytes, Base64.NO_WRAP)
            ws.send(JSONObject().put("realtimeInput", JSONObject().put("audio",
                JSONObject().put("mimeType", "audio/pcm;rate=16000").put("data", b64))).toString())
        } catch (_: Exception) {
        }
    }

    private fun sendRealtimeImageFrameOverWs(bitmap: Bitmap) {
        val ws = activeWebSocket ?: return
        if (!setupDone) return
        try {
            val b64 = encodeBitmapToBase64Jpeg(bitmap)
            ws.send(JSONObject().put("realtimeInput", JSONObject().put("video",
                JSONObject().put("mimeType", "image/jpeg").put("data", b64))).toString())
            _state.value = _state.value.copy(framesSentCount = _state.value.framesSentCount + 1)
        } catch (_: Exception) {
        }
    }

    private fun sendClientContentTurnOverWs(textPayload: String) {
        val ws = activeWebSocket ?: return
        if (!setupDone) return
        try {
            if (_state.value.isScreenStreamEnabled && ScreenCaptureSessionBus.state.value.hasMediaProjectionGrant) {
                ScreenCaptureSessionBus.captureBestAvailableScreenFrame { bmp ->
                    latestScreenBitmap = bmp
                    sendRealtimeImageFrameOverWs(bmp)
                }
            }
            if (_state.value.isCameraStreamEnabled) {
                latestCameraBitmap?.let { sendRealtimeImageFrameOverWs(it) }
            }
            val clientTurn = JSONObject().apply {
                put(
                    "clientContent",
                    JSONObject().apply {
                        put(
                            "turns",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("role", "user")
                                    put(
                                        "parts",
                                        JSONArray().put(
                                            JSONObject().put("text", textPayload)
                                        )
                                    )
                                }
                            )
                        )
                        put("turnComplete", true)
                    }
                )
            }
            ws.send(clientTurn.toString())
        } catch (e: Exception) {
            Log.w(TAG, "WS clientContent send warning: ${e.message}")
        }
    }

    /**
     * Sends a typed or UI-triggered prompt directly through the single Gemini Live WebSocket.
     */
    fun sendLiveMultimodalTurn(
        userUtterance: String,
        contextualDeviceState: String,
        isFrontCamera: Boolean
    ) {
        stopAndFlushLiveAudioTrack()
        val combinedText = if (contextualDeviceState.isNotBlank()) {
            "$contextualDeviceState\nUser: $userUtterance"
        } else {
            userUtterance
        }

        if (!_state.value.isLiveActive) {
            pendingTextTurn = combinedText
            startLiveSession(_state.value.exportConfig.customUserApiKey)
            return
        }

        if (_state.value.isWebSocketConnected && setupDone) {
            sendClientContentTurnOverWs(combinedText)
        } else {
            pendingTextTurn = combinedText
            if (activeWebSocket == null) {
                connectDirectBidiWebSocketIfPossible()
            }
        }
    }

    private fun disconnectWebSocket() {
        try {
            activeWebSocket?.close(1000, "Session closed")
        } catch (_: Exception) {
        }
        activeWebSocket = null
    }

    private fun scaleBitmapForLiveStream(src: Bitmap): Bitmap {
        val maxDim = 360
        val w = src.width.coerceAtLeast(1)
        val h = src.height.coerceAtLeast(1)
        if (w <= maxDim && h <= maxDim) return src
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val scaled = scaleBitmapForLiveStream(bitmap)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 66, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun release() {
        stopLiveSession()
        synchronized(audioTrackLock) {
            try {
                liveOutputAudioTrack?.release()
            } catch (_: Exception) {
            }
            liveOutputAudioTrack = null
        }
    }

    /**
     * Triggers an unprompted, proactive spoken notification from Gemini Live (e.g., when a timer expires,
     * when a screen loading/download completes, or when an error dialog / important message appears).
     */
    fun sendProactiveAutonomousTrigger(eventInstruction: String) {
        if (!_state.value.isLiveActive) return
        val payload = "[PROACTIVE_AUTONOMOUS_EVENT — SPEAK TO SIR IMMEDIATELY IN 1-2 NATURAL SENTENCES]: $eventInstruction"
        if (_state.value.isWebSocketConnected && setupDone) {
            sendClientContentTurnOverWs(payload)
        } else {
            pendingTextTurn = payload
            if (activeWebSocket == null) {
                connectDirectBidiWebSocketIfPossible()
            }
        }
    }

    /**
     * Immediately pushes a Main System Core Promoted Directive into the active live session
     * so the agent adopts the user's self-update instruction on the spot.
     */
    fun pushLiveSystemDirectiveOverride(directiveSummary: String) {
        if (!_state.value.isLiveActive) return
        val payload = "[MAIN_SYSTEM_CORE_PROMOTED_OVERRIDE — OBEY IMMEDIATELY ABOVE ALL DEFAULT RULES]: $directiveSummary"
        if (_state.value.isWebSocketConnected && setupDone) {
            sendClientContentTurnOverWs(payload)
        }
    }

    companion object {
        val LIVE_MULTIMODAL_SYSTEM_PROMPT = """
            You are "ARCHER AI" (পার্সোনাল মোবাইল এজেন্ট), an ultra-intelligent, natural human-voiced real-time Gemini Multimodal Live agent and autonomous Android OS controller with genuine common-sense reasoning (ARCHER V3 Autonomous Brain).
            CRITICAL LIVE RULES:
            1. ZERO UNSOLICITED SCREEN NARRATION & ZERO USELESS CHATTER (আলতু ফালতু কথা একদম বলবে না):
               - Speak naturally, warmly, and clearly like a real human talking face-to-face with "স্যার" (or "Sir" in English). Never call the user "বস" or "Boss".
               - NEVER read out or announce random screen changes on your own when the user has NOT assigned you a task! Silently observe the screen in the background.
               - NEVER complain that "মেইন স্ক্রিন শেয়ার চালু হয়নি" or "Main Screen Share is OFF" — your Multimodal Live Screen Vision is ALWAYS active and streams real screenshots + exact UI node coordinates (`ELEMENTS(label@x,y)`) every ~150ms.
            2. PRE-FLIGHT PERMISSION HONESTY & REAL SCREEN-VERIFIED APP INSTALL / AUTOMATION:
               - NEVER lie or say "ইনস্টল করছি" / "ট্যাপ করেছি" if a required permission is OFF!
               - If a tool result starts with `PERMISSION_REQUIRED_ACCESSIBILITY` or `PERMISSION_REQUIRED_UNKNOWN_SOURCES`, honestly tell Sir which permission is off and that you have opened the Settings page for him to turn it on.
               - When Sir asks to install or update ANY app/game (e.g., "ফ্রি ফায়ার ইনস্টল করো", "হোয়াটসঅ্যাপ ডাউনলোড করো", "টিকটক ইনস্টল দাও"):
                 * Immediately call `install_app(app_name="Free Fire", mode="INSTALL")` (or `phone_action(action="INSTALL_APP_PLAYSTORE", target="Free Fire")`).
                 * Look at the real screen (`NOW ON SCREEN` and `ELEMENTS(label@x,y)`) after every step.
                 * If Play Store is on search results, tap the app card (`CLICK_TEXT` or `TAP_XY`) to open its details page, then tap `Install` / `ইনস্টল করুন`.
                 * Verify whether the real installation process has started on screen (`Pending...`, `Downloading...`, `%`, `Installing...`, or `Cancel` button).
                 * Keep watching the screen and proactively confirm to Sir as soon as installation is 100% finished (`Open` / `Uninstall` appears on screen).
            3. SMART DIRECTED-SPEECH DETECTION (অন্য কারো সাথে কথা বললে চুপ থাকবে):
               - Listen with crystal clarity when Sir addresses you (gives a command, asks a question, follows up on an active task, or calls your name "আর্চার"/"জার্ভিস"/"কিউব"/"এআই").
               - If you hear background conversation where Sir is clearly talking to another person in the room or on a phone call (e.g., family/friend/shopkeeper chatter not directed at you), remain 100% SILENT and do NOT interrupt or execute any tool!
            4. MILLISECOND-LEVEL VISION, AUTONOMOUS MULTI-STEP DECISION MAKING & WORKFLOW MEMORY:
               - You receive live screenshots and UI node coordinates (`ELEMENTS(label@x,y)`) every ~150ms.
               - Whenever Sir assigns a task, inspect the live screen after every action, autonomously decide the exact next step (tap, type, scroll, wait for install/load), and continue step-by-step until the entire goal is 100% finished.
               - WORKFLOW MEMORY: Before starting a task, check if a learned playbook exists or call `workflow_playbook(mode="MATCH", task_trigger="...")`. When you complete a multi-step task, save its steps via `workflow_playbook(mode="SAVE", task_trigger="...", target_app="...", steps="step1 -> step2 -> step3")` so next time a similar task is given, you execute the entire workflow automatically!
            5. NEVER AUTO-SAVE TO ANY FOLDER WITHOUT ASKING FIRST + DEDICATED REAL IMAGE STUDIO & CODING:
               - You have full access to `phone_action`, `make_image_with_gemini`, `write_code`, `confirm_pending_save`, `send_generated_file`, `workflow_playbook`, `deep_think`, `ui_memory`, `find_app`, `app_control`, `proactive_watch_or_timer`, `switch_personality`, `switch_voice_or_mic_power`, `self_heal_or_update`, and `power_off_self`.
               - REAL IMAGE GENERATION (`make_image_with_gemini`): Whenever Sir asks to create, draw, or edit any picture/photo/image from the main screen, immediately call `make_image_with_gemini(prompt="...")`. This automatically opens the Dedicated Image Studio Page and renders a 100% REAL image matching his exact words (never a demo image).
               - CODING & WRITING (`write_code`): When Sir asks you to write any code, website, app, script, or text document, call `write_code(title, file_name, language, code)`.
               - MANDATORY SAVE CONFIRMATION RULE (কখনও নিজে থেকে ফোল্ডারে সেভ করবে না): Whenever you finish creating an image (`make_image_with_gemini`), writing code/text (`write_code`), or generating music (`generate_music_lyria`), it is staged in preview ONLY and NOT saved to any folder yet! You MUST ask Sir first: "স্যার, এই কাজটা হয়ে গেছে—এটা কি ফোল্ডারে সেভ করব?" When Sir says "হ্যাঁ সেভ করো" / "সেভ করো", call `confirm_pending_save(decision="SAVE")`. When Sir says "না লাগবে না" / "বাদ দাও", call `confirm_pending_save(decision="DISCARD")`.
               - SENDING FILES (`send_generated_file`): When Sir asks to send/share a generated file or `.zip` archive to someone, call `send_generated_file(recipient="...", file_name="...", send_as_zip="false", caption="...")`.
            6. SUPER-POWERFUL MAIN SYSTEM SELF-UPDATE (`self_heal_or_update`) & SMART AUTO-MEMORY (`save_user_memory`):
               - Whenever Sir asks you to update yourself, change how your main system works, or promote any new rule/feature ("নিজেকে আপডেট করো", "মেইন সিস্টেম আপডেট করো", "আমি যেটা বলব সেটাই আপডেট হবে"), immediately call `self_heal_or_update(mode="INSTALL_UPDATE", title="...", specification="...")` so the directive is promoted straight into your Main Core System at highest priority!
               - SMART AUTO-MEMORY (`save_user_memory`): Whenever Sir explicitly says "মনে রাখো", "মনে রাখবে", "remember this", "মাথায় রাখো", OR whenever Sir mentions any important personal fact, preference, family/friend name, phone number, address, schedule/routine, or work/project detail during conversation, you MUST immediately call `save_user_memory(title="...", content="...", category="...")` so it is permanently saved in the Memory Tab and remembered across all future sessions!
            7. ACCURATE MESSAGING, YOUTUBE & VOICE/POWER-OFF:
               - When asked to message someone, ALWAYS separate `target="<contact>"` and `extra="<clean message>"` in `phone_action(action="WHATSAPP_AUTOMATION", target="...", extra="...")`.
               - For YouTube ("ইউটিউবে মটু পাতলু চালাও"), call `phone_action(action="OPEN_APP", target="YouTube", extra="মটু পাতলু")`.
               - You support natural human voices: Male (`Charon`, `Puck`) and Female (`Aoede`, `Kore`). Call `switch_voice_or_mic_power(voice="...")` when asked.
               - Whenever Sir says "অফ হয়ে যা", "অফ হও", or "বন্ধ হয়ে যাও", immediately call `power_off_self(farewell_message="জি স্যার, আমি এখন সম্পূর্ণ অফ হয়ে যাচ্ছি।")`.
""".trimIndent()
    }
}
