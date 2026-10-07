package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

@Immutable
data class ActiveAutomationState(
  val isRunning: Boolean = false,
  val taskId: String? = null,
  val taskTitle: String? = null,
  val voiceMessage: String? = null,
  val isDndActive: Boolean = false,
  val isSpeaking: Boolean = false,
  val isVoiceEnabled: Boolean = true,
  val notificationTitle: String? = null,
  val notificationBody: String? = null
)

class RoutineAutomationService : Service(), TextToSpeech.OnInitListener {

  companion object {
    const val CHANNEL_ID = "chronos_automation_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_AUTOMATION = "com.example.ACTION_STOP_AUTOMATION"

    private val _serviceState = MutableStateFlow(ActiveAutomationState())
    val serviceState: StateFlow<ActiveAutomationState> = _serviceState.asStateFlow()
  }

  private var tts: TextToSpeech? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var audioManager: AudioManager? = null
  private var audioFocusRequest: AudioFocusRequest? = null
  private var isTtsReady = false
  private var pendingSpeechText: String? = null
  private var activeTaskId: String? = null
  private var currentDndApplied = false
  private var isVoiceEnabled = true

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    acquireWakeLock()
    try {
      tts = TextToSpeech(this, this)
    } catch (_: Exception) {
      isTtsReady = false
      releaseWakeLock()
    }
  }

  private fun acquireWakeLock() {
    try {
      if (wakeLock == null) {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "chronos:routine_automation_wakelock"
        )?.apply {
          setReferenceCounted(false)
        }
      }
      if (wakeLock?.isHeld == false) {
        wakeLock?.acquire(3 * 60 * 1000L) // 3 minute maximum safety window
      }
    } catch (_: Exception) {}
  }

  private fun releaseWakeLock() {
    try {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
    } catch (_: Exception) {}
  }

  private fun requestTransientAudioFocus(): Boolean {
    val am = audioManager ?: return false
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val playbackAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()

        val afr = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
          .setAudioAttributes(playbackAttributes)
          .setAcceptsDelayedFocusGain(false)
          .setOnAudioFocusChangeListener { /* No-op for transient ducking */ }
          .build()
        audioFocusRequest = afr
        am.requestAudioFocus(afr) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
      } else {
        @Suppress("DEPRECATION")
        am.requestAudioFocus(
          null,
          AudioManager.STREAM_NOTIFICATION,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun abandonTransientAudioFocus() {
    val am = audioManager ?: return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
      } else {
        @Suppress("DEPRECATION")
        am.abandonAudioFocus(null)
      }
    } catch (_: Exception) {}
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) return START_NOT_STICKY

    if (intent.action == ACTION_STOP_AUTOMATION || intent.action == AutomationManager.ACTION_END_TASK) {
      stopAutomation()
      return START_NOT_STICKY
    }

    val taskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID) ?: "task_${System.currentTimeMillis()}"
    val title = intent.getStringExtra(AutomationManager.EXTRA_TASK_TITLE) ?: "Scheduled Task"
    val voiceMessage = intent.getStringExtra(AutomationManager.EXTRA_VOICE_MESSAGE) ?: "$title is starting now."
    val enableDnd = intent.getBooleanExtra(AutomationManager.EXTRA_ENABLE_DND, false)
    val enableHaptic = intent.getBooleanExtra(AutomationManager.EXTRA_ENABLE_HAPTIC, true)
    isVoiceEnabled = intent.getBooleanExtra(AutomationManager.EXTRA_IS_VOICE_ENABLED, true)

    val isSoundAlertEnabled = intent.getBooleanExtra(AutomationManager.EXTRA_IS_SOUND_ALERT_ENABLED, true)
    val ringtoneUriString = intent.getStringExtra(AutomationManager.EXTRA_CUSTOM_RINGTONE_URI)

    val notifTitle = intent.getStringExtra(AutomationManager.EXTRA_NOTIFICATION_TITLE) ?: title
    val notifBody = intent.getStringExtra(AutomationManager.EXTRA_NOTIFICATION_BODY) ?: voiceMessage

    activeTaskId = taskId

    acquireWakeLock()

    // Start as Foreground Service with custom notification
    val notification = buildNotification(notifTitle, notifBody, enableDnd)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (e: Exception) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.notify(NOTIFICATION_ID, notification)
    }

    // Apply DND mode strictly guarded by permission check
    currentDndApplied = if (enableDnd && AutomationManager.isDndPermissionGranted(this)) {
      AutomationManager.setDndMode(this, true)
    } else {
      false
    }

    // Audio & Haptic Alerts: strictly respect isSoundAlertEnabled toggle
    if (isSoundAlertEnabled) {
      // Gentle Haptic pulse
      if (enableHaptic) {
        AutomationManager.triggerHaptic(this)
      }

      // Play chosen system notification sound
      try {
        val soundUri = if (!ringtoneUriString.isNullOrBlank()) {
          Uri.parse(ringtoneUriString)
        } else {
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
        ringtone?.play()
      } catch (_: Exception) {}

      // Voice Reminder via TTS if enabled
      if (isVoiceEnabled) {
        if (isTtsReady) {
          speakMessage(voiceMessage)
        } else {
          pendingSpeechText = voiceMessage
        }
      } else {
        releaseWakeLock()
      }
    } else {
      // Sound alert is OFF: Alarms, DND and visual notifications fire silently
      releaseWakeLock()
    }

    _serviceState.value = ActiveAutomationState(
      isRunning = true,
      taskId = taskId,
      taskTitle = title,
      voiceMessage = voiceMessage,
      isDndActive = currentDndApplied,
      isSpeaking = isVoiceEnabled,
      isVoiceEnabled = isVoiceEnabled,
      notificationTitle = notifTitle,
      notificationBody = notifBody
    )

    return START_STICKY
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      try {
        tts?.language = Locale.getDefault()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            _serviceState.value = _serviceState.value.copy(isSpeaking = true)
          }

          override fun onDone(utteranceId: String?) {
            _serviceState.value = _serviceState.value.copy(isSpeaking = false)
            abandonTransientAudioFocus()
            releaseWakeLock()
          }

          override fun onError(utteranceId: String?) {
            _serviceState.value = _serviceState.value.copy(isSpeaking = false)
            abandonTransientAudioFocus()
            releaseWakeLock()
          }
        })
        isTtsReady = true
        if (isVoiceEnabled) {
          pendingSpeechText?.let {
            speakMessage(it)
            pendingSpeechText = null
          }
        }
      } catch (_: Exception) {
        isTtsReady = false
        abandonTransientAudioFocus()
        releaseWakeLock()
      }
    } else {
      isTtsReady = false
      abandonTransientAudioFocus()
      releaseWakeLock()
    }
  }

  private fun speakMessage(text: String) {
    if (!isVoiceEnabled) return
    try {
      requestTransientAudioFocus()
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chronos_voice_${System.currentTimeMillis()}")
    } catch (_: Exception) {
      abandonTransientAudioFocus()
      releaseWakeLock()
    }
  }

  private fun stopAutomation() {
    try {
      if (currentDndApplied) {
        AutomationManager.setDndMode(this, false)
        currentDndApplied = false
      }
      tts?.stop()
    } catch (_: Exception) {
    } finally {
      abandonTransientAudioFocus()
      releaseWakeLock()
      _serviceState.value = ActiveAutomationState(isRunning = false)
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
          @Suppress("DEPRECATION")
          stopForeground(true)
        }
      } catch (_: Exception) {}
      stopSelf()
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Routine Automation"
      val descriptionText = "Lock-screen TTS alerts and DND automation"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableVibration(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(title: String, body: String, dndActive: Boolean): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPending = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val dndBadge = if (dndActive) " • DND Active 🌙" else ""

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("$title$dndBadge")
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setShowWhen(true)
      .setContentIntent(openPending)
      .setOngoing(true)
      .build()
  }

  override fun onDestroy() {
    try {
      stopAutomation()
    } finally {
      try {
        tts?.shutdown()
      } catch (_: Exception) {}
      abandonTransientAudioFocus()
      releaseWakeLock()
      super.onDestroy()
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
