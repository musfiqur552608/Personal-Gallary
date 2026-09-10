package com.freedu.personalgallary.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.prefs.SettingsRepository
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Living wallpaper: slow crossfading memories behind your home screen. */
class MemoryWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = MemoryEngine()

    inner class MemoryEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val io = CoroutineScope(Dispatchers.IO)
        private var items: List<MediaItem> = emptyList()
        private var index = 0
        private var current: Bitmap? = null
        private var alpha = 0f
        private var running = false

        private val loop = object : Runnable {
            override fun run() {
                if (!running) return
                advance()
                handler.postDelayed(this, 24_000)
            }
        }

        private val fader = object : Runnable {
            override fun run() {
                if (!running) return
                if (alpha < 1f) {
                    alpha = (alpha + 0.06f).coerceAtMost(1f)
                    draw()
                    handler.postDelayed(this, 60)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            io.launch {
                try {
                    val settings = SettingsRepository(this@MemoryWallpaper)
                    val excluded = try { settings.excludedAlbums.first() } catch (_: Exception) { emptySet() }
                    items = MediaStoreRepository(this@MemoryWallpaper)
                        .scanAll(excluded).items.filter { !it.isVideo }.take(40)
                } catch (_: Exception) {
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            handler.removeCallbacks(loop)
            handler.removeCallbacks(fader)
            if (visible) {
                advance()
                handler.postDelayed(loop, 24_000)
            }
        }

        override fun onDestroy() {
            running = false
            handler.removeCallbacks(loop)
            handler.removeCallbacks(fader)
            try {
                current?.recycle()
            } catch (_: Exception) {
            }
            super.onDestroy()
        }

        private fun advance() {
            if (items.isEmpty()) {
                draw()
                return
            }
            index = (index + 1) % items.size
            val uri = items[index].uri
            io.launch {
                try {
                    val bmp = ImageEditUtils.decodeSampled(this@MemoryWallpaper, uri, 1080)
                    val old = current
                    current = bmp
                    alpha = 0f
                    try {
                        old?.recycle()
                    } catch (_: Exception) {
                    }
                    handler.post(fader)
                } catch (_: Exception) {
                }
            }
        }

        private fun draw() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas == null) return
                canvas.drawColor(0xFF0B0B0F.toInt())
                current?.let { bmp ->
                    if (!bmp.isRecycled) {
                        val w = canvas.width.toFloat()
                        val h = canvas.height.toFloat()
                        val scale = maxOf(w / bmp.width, h / bmp.height)
                        val dw = bmp.width * scale
                        val dh = bmp.height * scale
                        val paint = Paint().apply { this.alpha = (alpha * 255).toInt() }
                        canvas.drawBitmap(
                            bmp, null,
                            RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f),
                            paint
                        )
                        // gentle dim for icon legibility
                        canvas.drawColor(0x55000000)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                }
            }
        }
    }
}
