package com.example.cozymojistandby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var slots: List<FrameLayout>
    private lateinit var colon1: TextView
    private lateinit var colon2: TextView
    private lateinit var setupOverlay: FrameLayout

    private var showSeconds = false
    private var digitGap = -35f
    private var fontSize = 360f
    private var fontId = R.font.sf_pro
    private var glassyEffect = false

    private val exitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ChargingReceiver.ACTION_EXIT) {
                finish()
            }
        }
    }

    companion object {
        private const val SWAP_DURATION = 820L
        private const val JIGGLE_DURATION = 600L
        private const val POST_SWAP_SETTLE_DURATION = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContentView(R.layout.activity_main)

        val root = findViewById<ViewGroup>(R.id.root)
        root.clipChildren = false
        root.clipToPadding = false

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        slots = listOf(
            findViewById(R.id.h1),
            findViewById(R.id.h2),
            findViewById(R.id.m1),
            findViewById(R.id.m2),
            findViewById(R.id.s1),
            findViewById(R.id.s2)
        )

        colon1 = findViewById(R.id.colon1)
        colon2 = findViewById(R.id.colon2)

        colon1.alpha = 0.9f
        colon2.alpha = 0.9f
        colon1.setTextColor(Color.WHITE)
        colon2.setTextColor(Color.WHITE)

        initSlots()
        setupOverlayUI()

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })

        root.setOnLongClickListener {
            showSettings()
            true
        }

        val filter = IntentFilter(ChargingReceiver.ACTION_EXIT)
        ContextCompat.registerReceiver(
            this,
            exitReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        startClock()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(exitReceiver)
    }

    override fun onResume() {
        super.onResume()
        updateSetupVisibility()
    }

    private fun setupOverlayUI() {
        val root = findViewById<FrameLayout>(R.id.root)
        
        setupOverlay = FrameLayout(this).apply {
            setBackgroundColor("#E6000000".toColorInt())
            visibility = View.GONE
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = (32 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            
            background = GradientDrawable().apply {
                setColor("#2C2C2E".toColorInt())
                cornerRadius = 64f
            }
            elevation = 20f
        }

        val title = TextView(this).apply {
            text = "Smooth Transition"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val desc = TextView(this).apply {
            text = "To enable Standby mode automatically when charging, we need permission to display over other apps."
            textSize = 15f
            setTextColor("#A1A1AA".toColorInt())
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
            setPadding(0, 0, 0, 48)
        }

        val btnGrant = Button(this, null, 0, com.google.android.material.R.style.Widget_Material3_Button_TonalButton).apply {
            text = "GRANT PERMISSION"
            setTextColor(Color.BLACK)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.1f
            
            background = GradientDrawable().apply {
                setColor(palette.c2[0])
                cornerRadius = 100f
            }
            
            val py = (16 * resources.displayMetrics.density).toInt()
            val px = (48 * resources.displayMetrics.density).toInt()
            setPadding(px, py, px, py)
            
            setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }
        }

        val btnLater = TextView(this).apply {
            text = "I'll setup later"
            textSize = 14f
            setTextColor("#71717A".toColorInt())
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                setupOverlay.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction { setupOverlay.visibility = View.GONE }
                    .start()
            }
        }

        card.addView(title)
        card.addView(desc)
        card.addView(btnGrant)
        card.addView(btnLater)
        
        val cardParams = FrameLayout.LayoutParams(
            (400 * resources.displayMetrics.density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        setupOverlay.addView(card, cardParams)
        root.addView(setupOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun updateSetupVisibility() {
        if (Settings.canDrawOverlays(this)) {
            setupOverlay.visibility = View.GONE
        } else if (setupOverlay.visibility == View.GONE) {
            setupOverlay.alpha = 0f
            setupOverlay.visibility = View.VISIBLE
            setupOverlay.animate().alpha(1f).setDuration(600).start()
        }
    }

    private fun initSlots() {
        slots.forEachIndexed { i, container ->
            container.clipChildren = false
            container.clipToPadding = false

            val tv = createDigit()
            tv.text = "0"
            
            val w = tv.paint.measureText("0").toInt()
            val fm = tv.paint.fontMetricsInt
            val h = fm.bottom - fm.top
            tv.layoutParams = FrameLayout.LayoutParams(w, h).apply {
                gravity = Gravity.CENTER
            }
            tv.pivotX = w / 2f
            tv.pivotY = h / 2f

            applyGradient(tv, i)
            tv.rotation = randomTilt()

            container.addView(tv)
        }
    }

    private fun createDigit(): TextView {
        return TextView(this).apply {
            textSize = fontSize
            setTextColor(Color.WHITE)
            typeface = ResourcesCompat.getFont(context, fontId)
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
            applyGlassyLook(this)
        }
    }

    private fun applyGlassyLook(v: View) {
        if (glassyEffect) {
            v.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            })
        } else {
            v.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private fun startClock() {
        handler.post(object : Runnable {
            override fun run() {
                val c = Calendar.getInstance()
                val h = String.format(Locale.US, "%02d", c.get(Calendar.HOUR_OF_DAY))
                val m = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE))
                val s = String.format(Locale.US, "%02d", c.get(Calendar.SECOND))

                val full = if (showSeconds) h + m + s else h + m
                val changedIndices = mutableListOf<Int>()

                for (i in full.indices) {
                    if (i < slots.size) {
                        val currentTv = slots[i].getChildAt(0) as TextView
                        if (currentTv.text != full[i].toString()) {
                            changedIndices.add(i)
                        }
                    }
                }

                if (changedIndices.isNotEmpty()) {
                    changedIndices.forEach { i ->
                        updateSlot(slots[i], full[i].toString(), i)
                    }
                    jiggleOthers(changedIndices)
                }

                slots[4].isVisible = showSeconds
                slots[5].isVisible = showSeconds
                colon2.isVisible = showSeconds

                layoutAll()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun layoutAll() {
        val density = resources.displayMetrics.density
        val gap = digitGap * density

        val refTv = slots[0].getChildAt(0) as TextView
        val fixedDigitWidth = refTv.paint.measureText("0")
        val colonW = colon1.paint.measureText(":")

        val items = mutableListOf<View>()
        val itemWidths = mutableListOf<Float>()

        items.add(slots[0]); itemWidths.add(fixedDigitWidth)
        items.add(slots[1]); itemWidths.add(fixedDigitWidth)
        items.add(colon1); itemWidths.add(colonW)
        items.add(slots[2]); itemWidths.add(fixedDigitWidth)
        items.add(slots[3]); itemWidths.add(fixedDigitWidth)

        if (showSeconds) {
            items.add(colon2); itemWidths.add(colonW)
            items.add(slots[4]); itemWidths.add(fixedDigitWidth)
            items.add(slots[5]); itemWidths.add(fixedDigitWidth)
        }

        val totalW = itemWidths.sum() + (itemWidths.size - 1) * gap - gap
        var currentX = -totalW / 2

        for (i in items.indices) {
            val view = items[i]
            val w = itemWidths[i]
            if (i == 2 || i == 5) currentX -= gap/2
            view.translationX = currentX + w / 2
            if (i == 2 || i == 5) currentX -= gap/2
            currentX += w + gap
        }

        val yOff = -25f * density
        colon1.translationY = yOff
        colon2.translationY = yOff
    }

    private fun updateSlot(container: FrameLayout, newText: String, index: Int) {
        val old = container.getChildAt(0) as TextView
        val newView = createDigit()
        newView.text = newText
        
        val w = old.paint.measureText("0").toInt()
        val fm = old.paint.fontMetricsInt
        val h = fm.bottom - fm.top
        
        newView.layoutParams = FrameLayout.LayoutParams(w, h).apply {
            gravity = Gravity.CENTER
        }
        newView.pivotX = w / 2f
        newView.pivotY = h / 2f

        applyGradient(old, index)
        applyGradient(newView, index)

        val tilt = randomTilt()
        container.addView(newView, 0)

        val screenH = resources.displayMetrics.heightPixels.toFloat()

        old.animate()
            .translationY(-screenH)
            .rotation(tilt)
            .alpha(0f)
            .setInterpolator(OvershootInterpolator(0.7f))
            .setDuration(SWAP_DURATION)
            .start()

        newView.translationY = screenH
        newView.alpha = 0f
        newView.rotation = -tilt

        newView.animate()
            .translationY(0f)
            .rotation(tilt)
            .alpha(1f)
            .setInterpolator(PathInterpolator(0.2f, 1.3f, 0.2f, 1f))
            .setDuration(SWAP_DURATION)
            .withEndAction {
                container.removeView(old)
                newView.animate()
                    .rotation(tilt + (Random.nextFloat() * 2f - 1f))
                    .setDuration(POST_SWAP_SETTLE_DURATION)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            }
            .start()
    }

    private fun jiggleOthers(exceptIndices: List<Int>) {
        slots.forEachIndexed { i, container ->
            if (!exceptIndices.contains(i) && container.isVisible) {
                val view = container.getChildAt(0) as? TextView ?: return@forEachIndexed
                val newTilt = randomTilt()
                view.pivotX = view.width / 2f
                view.pivotY = view.height / 2f
                view.animate()
                    .rotation(newTilt)
                    .setDuration(JIGGLE_DURATION)
                    .setInterpolator(OvershootInterpolator(3f))
                    .start()
            }
        }
    }

    // set palette once
    private val palette = palettes.entries.random().value
    private fun applyGradient(v: TextView, index: Int) {
        val h = v.textSize
        val colors = when (index) {
            0, 4 -> palette.c0
            1, 5 -> palette.c1
            2 -> palette.c2
            3 -> palette.c3
            else -> intArrayOf(Color.WHITE, Color.WHITE)
        }
        v.paint.shader = LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
        v.setShadowLayer(0f, 0f, 0f, 0)
    }

    private fun randomTilt(): Float {
        return Random.nextFloat() * 16f - 8f
    }

    private fun showSettings() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (24 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            background = GradientDrawable().apply {
                setColor("#1C1C1E".toColorInt())
            }
        }

        val title = TextView(this).apply {
            text = "Personalize"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(title)

        fun createSlider(label: String, min: Float, maxVal: Float, current: Float, step: Float, onProgress: (Float) -> Unit) {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
            }
            val header = TextView(this).apply {
                text = label
                setTextColor("#A1A1AA".toColorInt())
                textSize = 14f
                setPadding(0, 0, 0, 8)
            }
            
            val slider = Slider(this).apply {
                valueFrom = min
                valueTo = maxVal
                value = current.coerceIn(min, maxVal)
                stepSize = step
                
                val accentColor = palette.c1[0]
                thumbTintList = ColorStateList.valueOf(Color.WHITE)
                trackActiveTintList = ColorStateList.valueOf(accentColor)
                
                addOnChangeListener { _, value, _ ->
                    onProgress(value)
                }
            }
            
            container.addView(header)
            container.addView(slider)
            rootLayout.addView(container)
        }

        createSlider("Digit Spacing", -120f, 80f, digitGap, 5f) { digitGap = it }
        createSlider("Clock Size", 100f, 500f, fontSize, 10f) { fontSize = it }

        fun createToggle(label: String, initial: Boolean, onToggle: (Boolean) -> Unit) {
            val row = RelativeLayout(this).apply {
                setPadding(0, 24, 0, 24)
            }
            val text = TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 17f
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { addRule(RelativeLayout.ALIGN_PARENT_LEFT) }
            }
            val sw = MaterialSwitch(this).apply {
                isChecked = initial
                
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { addRule(RelativeLayout.ALIGN_PARENT_RIGHT) }
                
                setOnCheckedChangeListener { _, checked -> onToggle(checked) }
            }
            row.addView(text)
            row.addView(sw)
            rootLayout.addView(row)
        }

        createToggle("Show Seconds", showSeconds) { showSeconds = it }
        createToggle("Glassy Vibrancy", glassyEffect) { glassyEffect = it }

        MaterialAlertDialogBuilder(this)
            .setView(rootLayout)
            .setPositiveButton("Done") { _, _ -> updateAllDigits() }
            .show().apply {
                window?.setBackgroundDrawable(GradientDrawable().apply {
                    setColor("#1C1C1E".toColorInt())
                    cornerRadius = 48f
                })
            }
    }

    private fun updateAllDigits() {
        slots.forEachIndexed { index, container ->
            for (i in 0 until container.childCount) {
                val tv = container.getChildAt(i) as? TextView ?: continue
                tv.textSize = fontSize
                
                val w = tv.paint.measureText("0").toInt()
                val fm = tv.paint.fontMetricsInt
                val h = fm.bottom - fm.top
                
                tv.layoutParams = FrameLayout.LayoutParams(w, h).apply {
                    gravity = Gravity.CENTER
                }
                tv.pivotX = w / 2f
                tv.pivotY = h / 2f
                
                applyGlassyLook(tv)
                applyGradient(tv, index)
            }
        }
        
        val colonSize = fontSize * 0.65f
        colon1.textSize = colonSize
        colon2.textSize = colonSize
        layoutAll()
    }
}
