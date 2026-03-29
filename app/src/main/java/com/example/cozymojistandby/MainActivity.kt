package com.example.cozymojistandby

import android.app.AlertDialog
import android.graphics.*
import android.os.*
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var slots: List<FrameLayout>
    private lateinit var colon1: TextView
    private lateinit var colon2: TextView

    private var showSeconds = false
    private var digitGap = -30f // Default overlap
    private var font = R.font.sf_pro

    companion object {
        private const val SWAP_DURATION = 820L
        private const val JIGGLE_DURATION = 600L
        private const val POST_SWAP_SETTLE_DURATION = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<ViewGroup>(R.id.root)
        root.clipChildren = false
        root.clipToPadding = false

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

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

        // Make colons fully opaque
        colon1.alpha = 0.9f
        colon2.alpha = 0.9f
        colon1.setTextColor(Color.WHITE)
        colon2.setTextColor(Color.WHITE)

        initSlots()

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })

        root.setOnLongClickListener {
            showSettings()
            true
        }

        startClock()
    }

    // ---------- INIT ----------

    private fun initSlots() {
        slots.forEachIndexed { i, container ->
            container.clipChildren = false
            container.clipToPadding = false

            val tv = createDigit()
            tv.text = "0"
            
            // Set fixed size and pivot during initialization
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
            textSize = 260f
            setTextColor(Color.WHITE)
            typeface = ResourcesCompat.getFont(context, font)
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
        }
    }

    // ---------- CLOCK ----------

    private fun startClock() {
        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                val c = Calendar.getInstance()

                val h = String.format("%02d", c.get(Calendar.HOUR_OF_DAY))
                val m = String.format("%02d", c.get(Calendar.MINUTE))
                val s = String.format("%02d", c.get(Calendar.SECOND))

                val full = if (showSeconds) h + m + s else h + m
                val changedIndices = mutableListOf<Int>()

                // Identify which slots are actually changing
                for (i in full.indices) {
                    if (i < slots.size) {
                        val currentTv = slots[i].getChildAt(0) as TextView
                        if (currentTv.text != full[i].toString()) {
                            changedIndices.add(i)
                        }
                    }
                }

                // Update and jiggle only if there are changes
                if (changedIndices.isNotEmpty()) {
                    changedIndices.forEach { i ->
                        updateSlot(slots[i], full[i].toString(), i)
                    }
                    jiggleOthers(changedIndices)
                }

                slots[4].visibility = if (showSeconds) View.VISIBLE else View.GONE
                slots[5].visibility = if (showSeconds) View.VISIBLE else View.GONE
                colon2.visibility = if (showSeconds) View.VISIBLE else View.GONE

                layoutAll()

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(runnable)
    }

    // ---------- POSITION ----------

    private fun layoutAll() {
        val density = resources.displayMetrics.density
        val gap = digitGap * density

        val refTv = slots[0].getChildAt(0) as TextView
        val fixedDigitWidth = refTv.paint.measureText("0")
        
        val colonW = colon1.paint.measureText(":")

        val items = mutableListOf<View>()
        val itemWidths = mutableListOf<Float>()

        // H1 H2 : M1 M2
        items.add(slots[0]); itemWidths.add(fixedDigitWidth)
        items.add(slots[1]); itemWidths.add(fixedDigitWidth)
        items.add(colon1); itemWidths.add(colonW)
        items.add(slots[2]); itemWidths.add(fixedDigitWidth)
        items.add(slots[3]); itemWidths.add(fixedDigitWidth)

        if (showSeconds) {
            // : S1 S2
            items.add(colon2); itemWidths.add(colonW)
            items.add(slots[4]); itemWidths.add(fixedDigitWidth)
            items.add(slots[5]); itemWidths.add(fixedDigitWidth)
        }

        val totalW = itemWidths.sum() + (itemWidths.size - 1) * gap - 4 * gap
        var currentX = -totalW / 2

        for (i in items.indices) {
            val view = items[i]
            val w = itemWidths[i]

            if (i == 2 || i == 5) {
                currentX -= gap/2
            }
            view.translationX = currentX + w / 2

            if (i == 2 || i == 5) {
                currentX -= gap/2
            }
            currentX += w + gap
        }

        val yOff = -25f * density
        colon1.translationY = yOff
        colon2.translationY = yOff
    }

    // ---------- UPDATE ----------

    private fun updateSlot(container: FrameLayout, newText: String, index: Int) {
        val old = container.getChildAt(0) as TextView
        
        val newView = createDigit()
        newView.text = newText
        
        // Force fixed width and height to ensure pivot is perfectly centered
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
            // Jiggle only stable digits that aren't currently transitioning
            if (!exceptIndices.contains(i) && container.visibility == View.VISIBLE) {
                val view = container.getChildAt(0) as? TextView ?: return@forEachIndexed
                
                val newTilt = randomTilt()
                
                // Explicitly ensure pivot is centered before animating
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

    // ---------- STYLE ----------

    private fun applyGradient(v: TextView, index: Int) {
        val h = v.textSize
        val alpha = "#e5"
        val colors = when (index) {
            0, 4 -> intArrayOf(Color.parseColor(alpha + "0261da"), Color.parseColor(alpha + "0063db"))
            1, 5 -> intArrayOf(Color.parseColor(alpha + "61d392"), Color.parseColor(alpha + "65da98"))
            2 -> intArrayOf(Color.parseColor(alpha + "38b0fc"), Color.parseColor(alpha + "37b6fb"))
            3 -> intArrayOf(Color.parseColor(alpha + "2a8564"), Color.parseColor(alpha + "288b65"))
            else -> intArrayOf(Color.WHITE, Color.WHITE)
        }
        v.paint.shader = LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
        v.setShadowLayer(0f, 0f, 0f, 0)
    }

    private fun randomTilt(): Float {
        return Random.nextFloat() * 16f - 8f
    }

    // ---------- SETTINGS ----------

    private fun showSettings() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val seek = SeekBar(this).apply {
            max = 100
            progress = (digitGap + 80).toInt()
        }
        val toggle = Switch(this).apply {
            text = "Show Seconds"
            isChecked = showSeconds
        }
        layout.addView(seek)
        layout.addView(toggle)
        AlertDialog.Builder(this)
            .setView(layout)
            .setTitle("Settings")
            .setPositiveButton("OK") { _, _ ->
                digitGap = seek.progress.toFloat() - 80f
                showSeconds = toggle.isChecked
            }
            .show()
    }
}
