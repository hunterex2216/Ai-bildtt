package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

/**
 * ON/OFF switch for the on-screen touch effect ("Show tap indicator").
 * Synced with `AgentAbilitySwitchesBus` and controllable by voice via `app_control` or `set_ability_switch`.
 */
object TouchEffectBus {
    private const val PREFS = "omni_touch_fx"
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun load(ctx: Context) {
        val saved = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("on", false)
        _enabled.value = saved
        AgentAbilitySwitchesBus.syncTapIndicatorFromTouchBus(saved)
    }

    fun set(ctx: Context, on: Boolean) {
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("on", on).apply()
        setEnabled(on)
        AgentAbilitySwitchesBus.syncTapIndicatorFromTouchBus(on)
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        if (!on) TouchEffectOverlay.hide()
    }
}

/**
 * Floating character avatar overlay bus ("Pick a character that floats over your screen").
 * Uses `TYPE_ACCESSIBILITY_OVERLAY` when Accessibility service is active so it floats over all apps
 * without needing extra permissions, and also exposes state for in-app floating character rendering.
 */
object FloatingAvatarOverlayBus {
    private val main = Handler(Looper.getMainLooper())
    private val _character = MutableStateFlow(FloatingAvatarCharacter.OFF)
    val character: StateFlow<FloatingAvatarCharacter> = _character.asStateFlow()

    private val _isSpeakingPulse = MutableStateFlow(false)
    val isSpeakingPulse: StateFlow<Boolean> = _isSpeakingPulse.asStateFlow()

    private var avatarView: FloatingAvatarBubbleView? = null
    private var wm: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    var onAvatarTappedCallback: (() -> Unit)? = null

    fun setCharacter(char: FloatingAvatarCharacter) {
        _character.value = char
        main.post {
            if (char == FloatingAvatarCharacter.OFF) {
                hideOverlayWindow()
            } else {
                ensureOrUpdateOverlayWindow(char)
            }
        }
    }

    fun updateSpeakingPulse(speaking: Boolean) {
        if (_isSpeakingPulse.value != speaking) {
            _isSpeakingPulse.value = speaking
            main.post { avatarView?.setSpeaking(speaking) }
        }
    }

    fun refreshIfAccessibilityConnected() {
        val current = _character.value
        if (current != FloatingAvatarCharacter.OFF) {
            main.post { ensureOrUpdateOverlayWindow(current) }
        }
    }

    private fun ensureOrUpdateOverlayWindow(char: FloatingAvatarCharacter) {
        val svc = AccessibilityAutomationBus.serviceInstance ?: return
        try {
            val manager = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val existing = avatarView
            if (existing != null) {
                existing.setCharacter(char)
                return
            }
            val density = svc.resources.displayMetrics.density
            val sizePx = (66 * density).toInt()
            val lp = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = (16 * density).toInt()
                y = (160 * density).toInt()
            }
            val view = FloatingAvatarBubbleView(
                ctx = svc,
                initialChar = char,
                onDragBy = { dx, dy ->
                    try {
                        lp.x = (lp.x - dx).coerceAtLeast(0)
                        lp.y = (lp.y + dy).coerceAtLeast(40)
                        manager.updateViewLayout(avatarView, lp)
                    } catch (_: Exception) {}
                },
                onTapped = {
                    onAvatarTappedCallback?.invoke()
                }
            )
            manager.addView(view, lp)
            wm = manager
            layoutParams = lp
            avatarView = view
        } catch (_: Exception) {
        }
    }

    fun hideOverlayWindow() {
        main.post {
            try {
                avatarView?.let { wm?.removeView(it) }
            } catch (_: Exception) {}
            avatarView = null
            layoutParams = null
        }
    }
}

private class FloatingAvatarBubbleView(
    ctx: Context,
    initialChar: FloatingAvatarCharacter,
    private val onDragBy: (Int, Int) -> Unit,
    private val onTapped: () -> Unit
) : View(ctx) {
    private var currentChar: FloatingAvatarCharacter = initialChar
    private var speaking: Boolean = false
    private val d = ctx.resources.displayMetrics.density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E6161B26")
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.8f * d
        color = initialChar.accentHex
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9.5f * d
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 21f * d
        textAlign = Paint.Align.CENTER
    }
    private val pillRect = RectF()
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    fun setCharacter(c: FloatingAvatarCharacter) {
        currentChar = c
        ringPaint.color = c.accentHex
        invalidate()
    }

    fun setSpeaking(isSpeaking: Boolean) {
        speaking = isSpeaking
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f) - 4f * d
        c.drawCircle(cx, cy, r, bgPaint)
        ringPaint.strokeWidth = if (speaking) 4.2f * d else 2.6f * d
        c.drawCircle(cx, cy, r, ringPaint)
        c.drawText(currentChar.badgeEmoji, cx, cy + 3f * d, emojiPaint)

        pillRect.set(cx - 24f * d, height - 15f * d, cx + 24f * d, height - 2f * d)
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = currentChar.accentHex
        }
        c.drawRoundRect(pillRect, 6f * d, 6f * d, tagPaint)
        c.drawText(currentChar.displayName, cx, height - 5.2f * d, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX).toInt()
                val dy = (event.rawY - lastRawY).toInt()
                if (abs(dx) > 1 || abs(dy) > 1) {
                    onDragBy(dx, dy)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val totalMove = abs(event.rawX - downRawX) + abs(event.rawY - downRawY)
                if (totalMove < 12f * d) {
                    onTapped()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

/**
 * Draws a glowing ripple at every tap and a trail for every swipe the agent performs, on top of ANY app.
 * Uses an accessibility overlay window (no extra permission) and never intercepts touches.
 */
object TouchEffectOverlay {
    private val main = Handler(Looper.getMainLooper())
    private var view: RippleView? = null
    private var wm: WindowManager? = null

    fun tap(x: Float, y: Float) = post { ensure()?.add(Fx(x, y, x, y, false)) }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float) = post { ensure()?.add(Fx(x1, y1, x2, y2, true)) }

    private fun post(r: () -> Unit) {
        if (!TouchEffectBus.enabled.value) return
        main.post { try { r() } catch (_: Exception) {} }
    }

    private fun ensure(): RippleView? {
        view?.let { return it }
        val svc = AccessibilityAutomationBus.serviceInstance ?: return null
        val manager = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val v = RippleView(svc)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        manager.addView(v, lp)
        wm = manager
        view = v
        return v
    }

    fun hide() {
        main.post {
            try { view?.let { wm?.removeView(it) } } catch (_: Exception) {}
            view = null
        }
    }
}

private class Fx(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val swipe: Boolean) {
    val start: Long = SystemClock.uptimeMillis()
    val dur: Long = if (swipe) 750L else 650L
}

private class RippleView(ctx: Context) : View(ctx) {
    private val fxs = CopyOnWriteArrayList<Fx>()
    private val d = ctx.resources.displayMetrics.density
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f * d; color = Color.parseColor("#00FF9C")
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#00E5FF") }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 6f * d; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#00E5FF")
    }

    fun add(fx: Fx) { fxs.add(fx); postInvalidateOnAnimation() }

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        for (fx in fxs) {
            val t = (now - fx.start).toFloat() / fx.dur
            if (t >= 1f) { fxs.remove(fx); continue }
            val fade = 1f - t
            if (!fx.swipe) {
                ring.alpha = (230 * fade).toInt()
                c.drawCircle(fx.x1, fx.y1, (14f + 46f * t) * d, ring)
                fill.alpha = (150 * fade).toInt()
                c.drawCircle(fx.x1, fx.y1, 11f * d * (1f - 0.4f * t), fill)
            } else {
                val p = (1f - (1f - t.coerceAtMost(0.6f) / 0.6f) * (1f - t.coerceAtMost(0.6f) / 0.6f))
                val cx = fx.x1 + (fx.x2 - fx.x1) * p
                val cy = fx.y1 + (fx.y2 - fx.y1) * p
                line.alpha = (200 * fade).toInt()
                c.drawLine(fx.x1, fx.y1, cx, cy, line)
                fill.alpha = (230 * fade).toInt()
                c.drawCircle(cx, cy, 13f * d, fill)
                ring.alpha = (200 * fade).toInt()
                c.drawCircle(cx, cy, 24f * d, ring)
            }
        }
        if (fxs.isNotEmpty()) postInvalidateOnAnimation()
    }
}
