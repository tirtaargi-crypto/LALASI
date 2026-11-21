// IslandOverlayController.kt
//
// The Dynamic Island outside the app. While a song is loaded and LALASI is not on screen, this puts the
// island in a system overlay window at the top of the display, so it stays over other apps like the
// phone's. It is drawn through accessibility access (LockLyricsAccessibilityService, no extra permission) when that is on, and through
// "display over other apps" otherwise; with neither, nothing is drawn here and the in-app island still works. Inside LALASI the in-app island is drawn instead, so the two never overlap.

package com.danrey.lalasi.playback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.danrey.lalasi.constants.DynamicIslandCameraGapAuto
import com.danrey.lalasi.constants.DynamicIslandCameraGapKey
import com.danrey.lalasi.constants.DynamicIslandCameraKey
import com.danrey.lalasi.constants.DynamicIslandCardLyricsKey
import com.danrey.lalasi.constants.DynamicIslandShape
import com.danrey.lalasi.constants.DynamicIslandShapeKey
import com.danrey.lalasi.constants.DynamicIslandEnabledKey
import com.danrey.lalasi.constants.DynamicIslandOffsetDefault
import com.danrey.lalasi.constants.DynamicIslandOffsetKey
import com.danrey.lalasi.constants.DynamicIslandPillLyricsKey
import com.danrey.lalasi.constants.DynamicIslandSwipeKey
import com.danrey.lalasi.constants.DynamicIslandWordLitKey
import com.danrey.lalasi.ui.component.islandCameraGapDp
import com.danrey.lalasi.ui.component.islandClosedWindowHeight
import com.danrey.lalasi.ui.component.islandExpandedWindowHeight
import com.danrey.lalasi.ui.component.islandStatusBarPx
import com.danrey.lalasi.ui.component.islandWindowTopPx
import com.danrey.lalasi.ui.component.islandClosedWindowWidth
import com.danrey.lalasi.ui.component.IslandBelowBarGap
import com.danrey.lalasi.ui.component.IslandOptions
import com.danrey.lalasi.constants.DynamicIslandStyle
import com.danrey.lalasi.constants.DynamicIslandStyleKey
import com.danrey.lalasi.db.MusicDatabase
import com.danrey.lalasi.db.entities.LyricsEntity
import com.danrey.lalasi.lyrics.LyricsHelper
import com.danrey.lalasi.models.MediaMetadata
import com.danrey.lalasi.ui.component.DynamicIslandPill
import com.danrey.lalasi.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Whether LALASI's window is a floating / split window right now. The activity sets it; the island follows it. */
object IslandWindow {
    val multiWindow = MutableStateFlow(false)
}

class IslandOverlayController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val database: MusicDatabase,
    private val lyricsHelper: LyricsHelper,
    private val metadata: StateFlow<MediaMetadata?>,
    private val isPlaying: () -> Boolean,
    private val positionMs: () -> Long,
    private val durationMs: () -> Long,
    private val onPrevious: () -> Unit,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onSeek: (Long) -> Unit,
) {
    private val appWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    // the manager the island window was really added with: the accessibility service's, or the app's
    private var windowManager: WindowManager = appWindowManager
    private val onScreen = MutableStateFlow(false)
    private val style = MutableStateFlow(DynamicIslandStyle.DEFAULT)
    private val options = MutableStateFlow(IslandOptions())
    private val expanded = MutableStateFlow(false)
    // what the pill itself shows: it follows [expanded] only once the window is the right size (see sizeJob)
    private val pillOpen = MutableStateFlow(false)
    private val offsetDp = MutableStateFlow(DynamicIslandOffsetDefault)
    // island or notch, and whether the lyric flows around the front camera (and how wide that gap is; -1 = read it from the phone)
    private val shape = MutableStateFlow(DynamicIslandShape.ISLAND)
    private val cameraOn = MutableStateFlow(true)
    private val cameraManualGap = MutableStateFlow(DynamicIslandCameraGapAuto)
    private var layoutJob: Job? = null
    private var view: FrameLayout? = null
    private var owner: OverlayOwner? = null
    private var job: Job? = null
    private var sizeJob: Job? = null
    private var offsetJob: Job? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var lyricsJob: Job? = null

    private val processObserver = LifecycleEventObserver { _, _ ->
        onScreen.value = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    fun start() {
        if (job != null) return
        val process = ProcessLifecycleOwner.get().lifecycle
        process.addObserver(processObserver)
        onScreen.value = process.currentState.isAtLeast(Lifecycle.State.STARTED)

        val settings: Flow<Triple<Boolean, DynamicIslandStyle, IslandOptions>> = context.dataStore.data.map { prefs: Preferences ->
            val on = prefs[DynamicIslandEnabledKey] ?: false
            val look = prefs[DynamicIslandStyleKey]
                ?.let { name -> runCatching { DynamicIslandStyle.valueOf(name) }.getOrNull() }
                ?: DynamicIslandStyle.DEFAULT
            Triple(
                on,
                look,
                IslandOptions(
                    pillLyrics = prefs[DynamicIslandPillLyricsKey] ?: true,
                    cardLyrics = prefs[DynamicIslandCardLyricsKey] ?: true,
                    wordLit = prefs[DynamicIslandWordLitKey] ?: true,
                    swipe = prefs[DynamicIslandSwipeKey] ?: true,
                ),
            )
        }.distinctUntilChanged()

        // moving the island in Settings moves the window right away
        offsetJob = scope.launch {
            context.dataStore.data.map { it[DynamicIslandOffsetKey] ?: DynamicIslandOffsetDefault }.distinctUntilChanged().collect { dp ->
                offsetDp.value = dp
                val v = view
                val lp = windowParams
                if (v != null && lp != null) {
                    lp.y = windowTop()
                    runCatching { windowManager.updateViewLayout(v, lp) }
                }
            }
        }
        // the shape and the camera gap change the window itself, so the window is laid out again when they change
        layoutJob = scope.launch {
            context.dataStore.data.map { prefs: Preferences ->
                val sh = prefs[DynamicIslandShapeKey]
                    ?.let { name -> runCatching { DynamicIslandShape.valueOf(name) }.getOrNull() }
                    ?: DynamicIslandShape.ISLAND
                Triple(sh, prefs[DynamicIslandCameraKey] ?: true, prefs[DynamicIslandCameraGapKey] ?: DynamicIslandCameraGapAuto)
            }.distinctUntilChanged().collect { (sh, camera, gap) ->
                shape.value = sh
                cameraOn.value = camera
                cameraManualGap.value = gap
                // a closed window re-sized now cannot be seen moving; an open one keeps its size and only moves
                applyWindow(open = pillOpen.value)
            }
        }
        job = scope.launch {
            combine(settings, metadata.map { it != null }.distinctUntilChanged(), onScreen, IslandWindow.multiWindow, LockLyricsBus.connected) { (enabled, look, opts), hasSong, inApp, floating, _ ->
                style.value = look
                options.value = opts
                // in a floating / split window the island belongs outside the app, over the whole screen, not inside the small window
                enabled && hasSong && (!inApp || floating) && canDraw()
            }.distinctUntilChanged().collect { want -> if (want) show() else hide() }
        }
    }

    fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        job?.cancel()
        offsetJob?.cancel()
        offsetJob = null
        layoutJob?.cancel()
        layoutJob = null
        lyricsJob?.cancel()
        job = null
        lyricsJob = null
        hide()
    }

    // top of the window: island = bottom of the status bar plus the offset chosen in Settings, notch = the top edge of the screen plus
    // the offset; kept on the screen (the open card too). The same function draws the preview in Settings.
    private fun windowTop(): Int = islandWindowTopPx(context, shape.value, offsetDp.value)

    private fun statusBarDp(): Dp = (islandStatusBarPx(context) / context.resources.displayMetrics.density).dp

    // accessibility access draws it with no further permission; "display over other apps" is the fallback
    private fun canDraw() =
        LockLyricsBus.serviceWindow() != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    @SuppressLint("InflateParams")
    private fun show() {
        if (view != null) return
        val lifecycleOwner = OverlayOwner()
        // drawn through the accessibility service when it is on (its own window type, no overlay permission), else as an app overlay
        val access = LockLyricsBus.serviceWindow()
        val viewContext: Context = access?.first ?: context
        val manager: WindowManager = access?.second ?: appWindowManager
        expanded.value = false
        // a touch anywhere outside the window arrives as ACTION_OUTSIDE (FLAG_WATCH_OUTSIDE_TOUCH): it minimises the open card
        // ComposeView is final, so a plain FrameLayout holds it and watches for the outside touch
        val composeView = ComposeView(viewContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { OverlayContent() }
        }
        val root = object : FrameLayout(viewContext) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    expanded.value = false
                    return false
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            addView(composeView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val density = context.resources.displayMetrics.density
        // one fixed size while minimised (never wrap-content): the window does not follow the pill, so it can never flick sideways
        val params = WindowManager.LayoutParams(
            closedWidthPx(),
            (islandClosedWindowHeight(shape.value, statusBarDp()).value * density).toInt(),
            if (access != null) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            // right under the status bar, which takes every touch inside it
            y = windowTop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        runCatching { manager.addView(root, params) }
            .onSuccess {
                windowManager = manager
                view = root
                windowParams = params
                // The window has two fixed sizes, closed and open, and only ever switches while the pill is not moving:
                //  - opening: the window grows first, a few ms later the pill starts to stretch into it (so it is never clipped);
                //  - minimising: the pill springs back inside the big window, and only when it has fully settled does the window
                //    shrink, around the very same centre, so the swap cannot be seen.
                sizeJob?.cancel()
                pillOpen.value = false
                sizeJob = scope.launch {
                    expanded.collectLatest { open ->
                        if (open) {
                            applyWindow(open = true)
                            delay(70)
                            pillOpen.value = true
                        } else {
                            pillOpen.value = false
                            delay(900) // the close finishes first; then only the height shrinks (the width never changes)
                            applyWindow(open = false)
                        }
                    }
                }
                owner = lifecycleOwner
                // the song on screen when the island appears may have no lyrics yet
                lyricsJob?.cancel()
                lyricsJob = scope.launch {
                    val song = metadata.value ?: return@launch
                    if (database.lyrics(song.id).first() == null) {
                        val found = lyricsHelper.getLyrics(song)
                        if (found.provider != LyricsHelper.NoConnectionProvider) {
                            database.query { upsert(LyricsEntity(id = song.id, lyrics = found.lyrics, provider = found.provider)) }
                        }
                    }
                    metadata.distinctUntilChangedBy { it?.id }.collectLatest { next ->
                        if (next == null || view == null) return@collectLatest
                        if (database.lyrics(next.id).first() != null) return@collectLatest
                        val found = lyricsHelper.getLyrics(next)
                        if (found.provider != LyricsHelper.NoConnectionProvider) {
                            database.query { upsert(LyricsEntity(id = next.id, lyrics = found.lyrics, provider = found.provider)) }
                        }
                    }
                }
            }
            .onFailure { lifecycleOwner.destroy() }
    }

    private fun closedWidthPx(): Int {
        val d = context.resources.displayMetrics
        return (islandClosedWindowWidth((d.widthPixels / d.density).dp).value * d.density).toInt()
    }

    private fun applyWindow(open: Boolean) {
        val v = view ?: return
        val lp = windowParams ?: return
        val d = context.resources.displayMetrics
        if (open) {
            // the width stays the same open and closed: only the height changes, so the window never shifts sideways
            lp.width = closedWidthPx()
            lp.height = (islandExpandedWindowHeight(shape.value, statusBarDp()).value * d.density).toInt()
        } else {
            lp.width = closedWidthPx()
            lp.height = (islandClosedWindowHeight(shape.value, statusBarDp()).value * d.density).toInt()
        }
        lp.y = windowTop()
        runCatching { windowManager.updateViewLayout(v, lp) }
    }

    // a tap on the artwork of the open card goes to the app
    private fun openApp() {
        expanded.value = false
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { context.startActivity(launch) }
    }

    private fun hide() {
        sizeJob?.cancel()
        sizeJob = null
        windowParams = null
        expanded.value = false
        pillOpen.value = false
        val v = view ?: return
        view = null
        runCatching { windowManager.removeView(v) }
        owner?.destroy()
        owner = null
    }

    @Composable
    private fun OverlayContent() {
        val song by metadata.collectAsState()
        val look by style.collectAsState()
        val opts by options.collectAsState()
        val current = song ?: return
        val lyrics by remember(current.id) { database.lyrics(current.id) }.collectAsState(initial = null)
        val offset by remember(current.id) { database.song(current.id).map { it?.song?.lyricsOffset ?: 0 } }.collectAsState(initial = 0)
        var playing by remember { mutableStateOf(isPlaying()) }
        val open by pillOpen.collectAsState()
        // follows the pressed state (the play intent) every frame or so, so the icon flips the moment you tap
        LaunchedEffect(Unit) {
            while (true) {
                playing = isPlaying()
                delay(40)
            }
        }
        val screen = context.resources.displayMetrics.let { it.widthPixels / it.density }.dp
        val shapeNow by shape.collectAsState()
        val cameraNow by cameraOn.collectAsState()
        val manualGap by cameraManualGap.collectAsState()
        // 0 dp = camera mode off; otherwise the width of the camera that the line flows around
        val gap = remember(cameraNow, manualGap) { if (cameraNow) islandCameraGapDp(context, manualGap) else 0.dp }
        val bar = remember { statusBarDp() }
        // the pill always sits at the top centre of a window that is centred on the screen: nothing here depends on the window's size
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        DynamicIslandPill(
            metadata = current,
            isPlaying = playing,
            lyricsRaw = lyrics?.lyrics,
            offsetMs = offset,
            positionNow = positionMs,
            durationMs = durationMs,
            onPrevious = onPrevious,
            onPlayPause = { playing = !playing; onPlayPause() },
            onNext = onNext,
            onSeek = onSeek,
            style = look,
            options = opts,
            shapeStyle = shapeNow,
            statusBar = bar,
            cameraGap = gap,
            fullWidth = screen,
            expandedState = open,
            onExpandedChange = { expanded.value = it },
            onOpenApp = ::openApp,
        )
        }
    }

    // a window outside any activity has no lifecycle of its own; Compose needs one on the view
    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val viewModelStore = ViewModelStore()
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

        init {
            saved.performAttach()
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}
