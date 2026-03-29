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
    private var digitGap = -40f
    private var font = R.font.sf_pro

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
            container.setPadding(120, 120, 120, 120)

            val tv = createDigit()
            tv.text = "0"
            applyGradient(tv, i)
            tv.rotation = randomTilt()

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER

            container.addView(tv, params)
        }
    }

    private fun createDigit(): TextView {
        return TextView(this).apply {
            textSize = 160f
            setTextColor(Color.WHITE)
            typeface = ResourcesCompat.getFont(context, font)

            setPadding(80, 80, 80, 80)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
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

                for (i in full.indices) {
                    updateSlot(slots[i], full[i].toString(), i)
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
        val spacing = 160f + digitGap

        val offsets = if (showSeconds)
            floatArrayOf(-3f, -2f, -0.5f, 0.5f, 2f, 3f)
        else
            floatArrayOf(-2f, -1f, 1f, 2f)

        slots.forEachIndexed { i, container ->
            if (!showSeconds && i >= 4) return@forEachIndexed
            container.translationX = offsets[i] * spacing
        }

        colon1.translationX = -spacing * 0.5f
        colon2.translationX = spacing * 1.5f
    }

    // ---------- UPDATE ----------

    private fun updateSlot(container: FrameLayout, newText: String, index: Int) {
        val old = container.getChildAt(0) as TextView
        if (old.text == newText) return

        val newView = createDigit()
        newView.text = newText

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
            .setDuration(420)
            .start()

        newView.translationY = screenH
        newView.alpha = 0f
        newView.rotation = -tilt

        newView.animate()
            .translationY(0f)
            .rotation(tilt)
            .alpha(1f)
            .setInterpolator(PathInterpolator(0.2f, 1.3f, 0.2f, 1f))
            .setDuration(420)
            .withEndAction {
                container.removeView(old)

                newView.animate()
                    .rotation(tilt + randomTilt() * 0.3f)
                    .setDuration(500)
                    .setInterpolator(PathInterpolator(0.3f, 1.4f, 0.3f, 1f))
                    .start()
            }
            .start()
    }

    // ---------- STYLE ----------

    private fun applyGradient(v: TextView, index: Int) {
        val w = v.paint.measureText(v.text.toString()) + 300f

        val colors = when (index) {
            0, 1 -> intArrayOf(
                Color.parseColor("#AA6FE7FF"),
                Color.parseColor("#CC4FD8FF")
            )
            2, 3 -> intArrayOf(
                Color.parseColor("#AA7BFFB5"),
                Color.parseColor("#CC4BFF9A")
            )
            else -> intArrayOf(
                Color.parseColor("#AAE0A3FF"),
                Color.parseColor("#CCB46CFF")
            )
        }

        v.paint.shader = LinearGradient(0f, 0f, w, 0f, colors, null, Shader.TileMode.CLAMP)
        v.setShadowLayer(60f, 0f, 0f, colors[0])
    }

    private fun randomTilt(): Float {
        return Random.nextFloat() * 6f - 3f
    }

    // ---------- SETTINGS ----------

    private fun showSettings() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val seek = SeekBar(this).apply {
            max = 50
            progress = digitGap.toInt() + 25
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
                digitGap = seek.progress - 25f
                showSeconds = toggle.isChecked
            }
            .show()
    }
}