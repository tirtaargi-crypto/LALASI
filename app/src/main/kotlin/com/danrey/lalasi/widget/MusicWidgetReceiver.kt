// MusicWidgetReceiver.kt

package com.danrey.lalasi.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.danrey.lalasi.playback.MusicService

class MusicWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {

            }
        }

    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {

            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_NEXT, ACTION_PREVIOUS -> {

                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {

                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.danrey.lalasi.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.danrey.lalasi.widget.LIKE"
        const val ACTION_NEXT = "com.danrey.lalasi.widget.NEXT"
        const val ACTION_PREVIOUS = "com.danrey.lalasi.widget.PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.danrey.lalasi.widget.UPDATE_WIDGET"
    }
}
