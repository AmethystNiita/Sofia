package com.amethystniita.sofia

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingControlService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var tvProgress: TextView? = null
    private var iconView: ImageView? = null

    companion object {
        private var instance: FloatingControlService? = null

        fun updateProgress(current: Int, total: Int) {
            Handler(Looper.getMainLooper()).post {
                instance?.let { service ->
                    val isPaused = SofiaAccessibilityService.isCleaningPaused

                    val statusText = if (isPaused) {
                        "$current/$total"
                    } else {
                        "$current/$total"
                    }
                    service.tvProgress?.text = statusText

                    if (isPaused) {
                        val matrix = ColorMatrix().apply { setSaturation(0f) }
                        service.iconView?.colorFilter = ColorMatrixColorFilter(matrix)
                    } else {
                        service.iconView?.colorFilter = null
                    }

                    service.floatingView?.let { view ->
                        service.windowParams?.let { params ->
                            service.windowManager.updateViewLayout(view, params)
                        }
                    }
                }
            }
        }

        fun stopSelfService(context: Context) {
            context.stopService(Intent(context, FloatingControlService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
        windowParams = params

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        iconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            background = null
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        tvProgress = TextView(this).apply {
            text = "0/${SofiaAccessibilityService.packageList.size}"
            setTextColor(Color.WHITE)
            setTextSize(11f)
            gravity = Gravity.CENTER
            setPadding(16, 6, 16, 6)
            setBackgroundResource(android.R.drawable.toast_frame)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 4
            }
        }

        container.addView(iconView)
        container.addView(tvProgress)
        floatingView = container

        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            private val handler = Handler(Looper.getMainLooper())
            private var isLongPress = false
            private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

            private val longPressRunnable = Runnable {
                isLongPress = true
                SofiaAccessibilityService.isRunning = false
                SofiaAccessibilityService.isCleaningPaused = false

                val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                if (intent != null) {
                    startActivity(intent)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(applicationContext, "Cache cleaning stopped.", Toast.LENGTH_SHORT).show()
                    stopSelf()
                }, 100)
            }

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isLongPress = false
                        handler.postDelayed(longPressRunnable, longPressTimeout)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX > 10 || diffY > 10) {
                            handler.removeCallbacks(longPressRunnable)
                        }
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        handler.removeCallbacks(longPressRunnable)
                        if (!isLongPress) {
                            val diffX = Math.abs(event.rawX - initialTouchX)
                            val diffY = Math.abs(event.rawY - initialTouchY)

                            if (diffX < 10 && diffY < 10) {
                                SofiaAccessibilityService.isCleaningPaused = !SofiaAccessibilityService.isCleaningPaused
                                val total = SofiaAccessibilityService.packageList.size
                                val current = SofiaAccessibilityService.currentPackageIndex
                                updateProgress(current, total)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }
    }
}