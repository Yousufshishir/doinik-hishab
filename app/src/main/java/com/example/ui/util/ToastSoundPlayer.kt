package com.example.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build

/**
 * Handles playing subtle, standard Android notification sound feedback
 * when premium toast notifications and system alerts are emitted.
 */
object ToastSoundPlayer {
    fun playNotificationSound(context: Context) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
                return
            }
        } catch (e: Exception) {
            // System ringtone unavailable, fallback below
        }

        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 160)
        } catch (ignored: Exception) {
        }
    }
}
