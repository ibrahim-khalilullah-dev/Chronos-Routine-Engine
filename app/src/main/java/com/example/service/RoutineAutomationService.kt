package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.RoutineRepository
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
  val notificationBody: String? = null,
  val formattedEndTime: String? = null
)

class RoutineAutomationService : Service(), TextToSpeech.OnInitListener {

  companion object {
    const val CHANNEL_ID = "chronos_automation_silent_channel"
    private const val LEGACY_CHANNEL_ID = "chronos_automation_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_AUTOMATION = "com.example.ACTION_STOP_AUTOMATION"
    private const val WAKELOCK_MAX_TIMEOUT_MS = 25_000L
    private const val TTS_SAFETY_TIMEOUT_MS = 20_000L
    private const val RINGTONE_MAX_TIMEOUT_MS = 10_000L

    private const val SCREEN_BUTTON_GUARD_MS = 200L
    private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    private const val EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
    private val WATCHED_VOLUME_STREAMS = intArrayOf(
      AudioManager.STREAM_ALARM,
      AudioManager.STREAM_MUSIC,
      AudioManager.STREAM_RING,
      AudioManager.STREAM_NOTIFICATION
    )

    private val _serviceState = MutableStateFlow(ActiveAutomationState())
    val serviceState: StateFlow<ActiveAutomationState> = _serviceState.asStateFlow()

    @Volatile
    private var activeInstance: RoutineAutomationService? = null

    fun onDndApplied(taskId: String?) {
      val current = _serviceState.value
      if (current.isRunning && (taskId == null || current.taskId == taskId)) {
        _serviceState.value = current.copy(isDndActive = true)
      }
      activeInstance?.let { instance ->
        if (taskId == null || instance.activeTaskId == taskId) {
          instance.currentDndApplied = true
        }
      }
    }

    fun stopVoiceIfSpeaking(context: Context): Boolean {
      val instance = activeInstance
      val state = _serviceState.value
      if (instance != null && (state.isSpeaking || instance.hasActiveVoiceSession())) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
          instance.stopVoiceOnly()
        } else {
          Handler(Looper.getMainLooper()).post {
            instance.stopVoiceOnly()
          }
        }
        return true
      }
      if (state.isSpeaking) {
        _serviceState.value = state.copy(isSpeaking = false)
        return true
      }
      return false
    }

    fun requestStop(context: Context, taskId: String?) {
      val current = _serviceState.value
      if (!current.isRunning) return
      if (taskId != null && current.taskId != taskId) return
      val instance = activeInstance
      if (instance != null && (taskId == null || instance.activeTaskId == taskId)) {
        Handler(Looper.getMainLooper()).post {
          if (taskId == null || instance.activeTaskId == taskId) {
            instance.stopAutomation()
          }
        }
      } else {
        try {
          context.stopService(Intent(context, RoutineAutomationService::class.java))
        } catch (_: Exception) {}
      }
    }
  }

  private var tts: TextToSpeech? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var audioManager: AudioManager? = null
  private var audioFocusRequest: AudioFocusRequest? = null
  private var isTtsReady = false
  private var triedGoogleTtsFallback = false
  private var pendingSpeechText: String? = null
  private var activeTaskId: String? = null
  private var currentDndApplied = false
  private var isDndConfiguredForTask = false
  private var dndHandler: Handler? = null
  private var dndRunnable: Runnable? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var ttsDelayRunnable: Runnable? = null
  private var ttsSafetyTimeoutRunnable: Runnable? = null
  private var isVoiceEnabled = true
  private var voiceVolume = 1.0f
  private var activeRingtone: Ringtone? = null
  private var activeMediaPlayer: MediaPlayer? = null
  private var ringtoneTimeoutRunnable: Runnable? = null
  private var voiceSessionStartElapsedMs: Long = 0L
  private var hardwareStopReceiver: BroadcastReceiver? = null
  private var volumeSettingsObserver: ContentObserver? = null
  private var isHardwareStopRegistered = false
  private val initialStreamVolumes = mutableMapOf<Int, Int>()
  @Volatile
  private var isStopping = false

  override fun onCreate() {
    super.onCreate()
    isStopping = false
    activeInstance = this
    createNotificationChannel()
    audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
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
        wakeLock?.acquire(WAKELOCK_MAX_TIMEOUT_MS)
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

  private fun cancelTtsSafetyTimeout() {
    ttsSafetyTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
    ttsSafetyTimeoutRunnable = null
  }

  private fun scheduleTtsSafetyTimeout() {
    cancelTtsSafetyTimeout()
    val timeoutRunnable = Runnable {
      ttsSafetyTimeoutRunnable = null
      _serviceState.value = _serviceState.value.copy(isSpeaking = false)
      releaseTtsEngine()
    }
    ttsSafetyTimeoutRunnable = timeoutRunnable
    mainHandler.postDelayed(timeoutRunnable, TTS_SAFETY_TIMEOUT_MS)
  }

  private fun hasActiveVoiceSession(): Boolean {
    return ttsDelayRunnable != null || tts != null || _serviceState.value.isSpeaking
  }

  internal fun isHardwareStopRegisteredForTesting(): Boolean = isHardwareStopRegistered

  private fun registerVoiceStopHardwareListeners() {
    unregisterVoiceStopHardwareListeners()
    val am = audioManager
    if (am != null) {
      for (stream in WATCHED_VOLUME_STREAMS) {
        try {
          initialStreamVolumes[stream] = am.getStreamVolume(stream)
        } catch (_: Exception) {}
      }
    }

    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
          Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
            val elapsed = SystemClock.elapsedRealtime() - voiceSessionStartElapsedMs
            if (elapsed > SCREEN_BUTTON_GUARD_MS) {
              stopVoiceOnly()
            }
          }
          VOLUME_CHANGED_ACTION -> {
            val newVol = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1)
            val prevVol = intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, -1)
            if (newVol >= 0 && prevVol >= 0 && newVol == prevVol) {
              return
            }
            stopVoiceOnly()
          }
        }
      }
    }
    try {
      val filter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(VOLUME_CHANGED_ACTION)
      }
      ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
      hardwareStopReceiver = receiver
      isHardwareStopRegistered = true
    } catch (_: Exception) {
      hardwareStopReceiver = null
      isHardwareStopRegistered = false
    }

    val observer = object : ContentObserver(mainHandler) {
      override fun onChange(selfChange: Boolean, uri: Uri?) {
        val lastSeg = uri?.lastPathSegment
        val currAm = audioManager
        if (currAm != null) {
          var streamChanged = false
          for (stream in WATCHED_VOLUME_STREAMS) {
            val currentVol = try {
              currAm.getStreamVolume(stream)
            } catch (_: Exception) {
              continue
            }
            val prevVol = initialStreamVolumes[stream]
            if (prevVol != null && currentVol != prevVol) {
              initialStreamVolumes[stream] = currentVol
              streamChanged = true
            }
          }
          if (streamChanged || (lastSeg != null && lastSeg.startsWith("volume_"))) {
            stopVoiceOnly()
          }
        } else if (lastSeg != null && lastSeg.startsWith("volume_")) {
          stopVoiceOnly()
        }
      }
    }
    try {
      contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
      volumeSettingsObserver = observer
    } catch (_: Exception) {
      volumeSettingsObserver = null
    }
  }

  private fun unregisterVoiceStopHardwareListeners() {
    hardwareStopReceiver?.let { receiver ->
      try {
        unregisterReceiver(receiver)
      } catch (_: Exception) {}
    }
    hardwareStopReceiver = null
    isHardwareStopRegistered = false

    volumeSettingsObserver?.let { observer ->
      try {
        contentResolver.unregisterContentObserver(observer)
      } catch (_: Exception) {}
    }
    volumeSettingsObserver = null
    initialStreamVolumes.clear()
  }

  internal fun stopVoiceOnly() {
    ttsDelayRunnable?.let { mainHandler.removeCallbacks(it) }
    ttsDelayRunnable = null
    stopActiveSoundAlert()
    releaseTtsEngine()
    val current = _serviceState.value
    if (current.isSpeaking) {
      _serviceState.value = current.copy(isSpeaking = false)
    }
  }

  private fun releaseTtsEngine() {
    cancelTtsSafetyTimeout()
    unregisterVoiceStopHardwareListeners()
    pendingSpeechText = null
    isTtsReady = false
    triedGoogleTtsFallback = false
    try {
      tts?.stop()
    } catch (_: Exception) {}
    try {
      tts?.shutdown()
    } catch (_: Exception) {}
    tts = null
    abandonTransientAudioFocus()
    releaseWakeLock()
  }

  private fun cancelRingtoneTimeout() {
    ringtoneTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
    ringtoneTimeoutRunnable = null
  }

  private fun scheduleRingtoneTimeout() {
    cancelRingtoneTimeout()
    val timeoutRunnable = Runnable {
      ringtoneTimeoutRunnable = null
      stopActiveSoundAlert()
    }
    ringtoneTimeoutRunnable = timeoutRunnable
    mainHandler.postDelayed(timeoutRunnable, RINGTONE_MAX_TIMEOUT_MS)
  }

  private fun stopActiveSoundAlert() {
    cancelRingtoneTimeout()
    try {
      activeRingtone?.stop()
    } catch (_: Exception) {}
    activeRingtone = null
    try {
      activeMediaPlayer?.let { mp ->
        if (mp.isPlaying) {
          mp.stop()
        }
        mp.reset()
        mp.release()
      }
    } catch (_: Exception) {}
    activeMediaPlayer = null
  }

  private fun requestTransientAudioFocus(): Boolean {
    val am = audioManager ?: return false
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val playbackAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
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
          AudioManager.STREAM_ALARM,
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
    if (intent == null) {
      releaseTtsEngine()
      stopSelf()
      return START_NOT_STICKY
    }

    if (intent.action == ACTION_STOP_AUTOMATION || intent.action == AutomationManager.ACTION_END_TASK) {
      val targetTaskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID)
      // Guard: Only stop if targetTaskId is null (manual user stop) or matches activeTaskId!
      if (targetTaskId == null || targetTaskId == activeTaskId) {
        stopAutomation()
      }
      return START_NOT_STICKY
    }

    if (intent.action == AutomationManager.ACTION_APPLY_DND) {
      currentDndApplied = true
      _serviceState.value = _serviceState.value.copy(isDndActive = true)
      return START_NOT_STICKY
    }

    isStopping = false
    val taskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID) ?: "task_${System.currentTimeMillis()}"
    val title = intent.getStringExtra(AutomationManager.EXTRA_TASK_TITLE) ?: "Scheduled Task"
    val voiceMessage = intent.getStringExtra(AutomationManager.EXTRA_VOICE_MESSAGE) ?: "$title is starting now."
    val enableDnd = intent.getBooleanExtra(AutomationManager.EXTRA_ENABLE_DND, false)
    isDndConfiguredForTask = enableDnd
    val dndDelayMinutes = intent.getIntExtra(AutomationManager.EXTRA_DND_DELAY_MINUTES, 0).coerceIn(0, 25)
    val enableHaptic = intent.getBooleanExtra(AutomationManager.EXTRA_ENABLE_HAPTIC, true)
    isVoiceEnabled = intent.getBooleanExtra(AutomationManager.EXTRA_IS_VOICE_ENABLED, true)
    voiceVolume = intent.getFloatExtra(AutomationManager.EXTRA_VOICE_VOLUME, 1.0f).coerceIn(0.0f, 1.0f)

    val isSoundAlertEnabled = intent.getBooleanExtra(AutomationManager.EXTRA_IS_SOUND_ALERT_ENABLED, true)
    val ringtoneUriString = intent.getStringExtra(AutomationManager.EXTRA_CUSTOM_RINGTONE_URI)

    val notifTitle = intent.getStringExtra(AutomationManager.EXTRA_NOTIFICATION_TITLE) ?: title
    val notifBody = intent.getStringExtra(AutomationManager.EXTRA_NOTIFICATION_BODY) ?: voiceMessage
    val categoryEmoji = intent.getStringExtra(AutomationManager.EXTRA_CATEGORY_EMOJI) ?: "⏱️"
    val categoryName = intent.getStringExtra(AutomationManager.EXTRA_CATEGORY_NAME) ?: "Routine Focus"
    val formattedDuration = intent.getStringExtra(AutomationManager.EXTRA_FORMATTED_DURATION) ?: ""
    val formattedEndTime = intent.getStringExtra(AutomationManager.EXTRA_FORMATTED_END_TIME) ?: ""

    val previousTaskId = activeTaskId
    activeTaskId = taskId
    ttsDelayRunnable?.let { mainHandler.removeCallbacks(it) }
    ttsDelayRunnable = null
    pendingSpeechText = null

    acquireWakeLock()

    // Start as Foreground Service with professional One UI notification
    val notification = buildNotification(
      title = notifTitle,
      categoryEmoji = categoryEmoji,
      categoryName = categoryName,
      formattedDuration = formattedDuration,
      formattedEndTime = formattedEndTime,
      voiceMessage = voiceMessage
    )
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

    // Apply DND mode strictly guarded by permission check & task ownership:
    // If dndDelayMinutes <= 0 && enableDnd, engage DND immediately; if dndDelayMinutes > 0, AlarmManager fires ACTION_APPLY_DND
    val activeDndOwner = AutomationManager.getActiveDndTaskId(this)
    if (enableDnd && AutomationManager.isDndPermissionGranted(this)) {
      if (dndDelayMinutes <= 0) {
        currentDndApplied = AutomationManager.setDndMode(this, true, taskId)
      } else {
        if (currentDndApplied && (activeDndOwner == null || activeDndOwner == previousTaskId || activeDndOwner == taskId)) {
          AutomationManager.setDndMode(this, false, activeDndOwner ?: taskId)
        }
        currentDndApplied = false
      }
    } else {
      if (currentDndApplied && (activeDndOwner == null || activeDndOwner == previousTaskId || activeDndOwner == taskId)) {
        AutomationManager.setDndMode(this, false, activeDndOwner ?: taskId)
      }
      currentDndApplied = false
    }

    // Decoupled Haptic Pulse (fires independently of sound alert toggle)
    if (enableHaptic) {
      AutomationManager.triggerHaptic(this)
    }

    // Decoupled Ringtone / Chime Sound Alert (silent notification channel ensures no double-chime)
    stopActiveSoundAlert()
    if (isSoundAlertEnabled) {
      playTaskRingtone(ringtoneUriString)
    }

    // Decoupled Lazy Voice Reminder via TTS (only instantiated when isVoiceEnabled == true)
    if (isVoiceEnabled) {
      voiceSessionStartElapsedMs = SystemClock.elapsedRealtime()
      registerVoiceStopHardwareListeners()
      scheduleTtsSafetyTimeout()
      if (isSoundAlertEnabled) {
        val scheduledSpeech = voiceMessage
        val runnable = Runnable {
          ttsDelayRunnable = null
          if (isVoiceEnabled && activeTaskId == taskId) {
            triggerLazyTtsSpeech(scheduledSpeech)
          }
        }
        ttsDelayRunnable = runnable
        mainHandler.postDelayed(runnable, 1500L)
      } else {
        triggerLazyTtsSpeech(voiceMessage)
      }
    } else {
      releaseTtsEngine()
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
      notificationBody = notifBody,
      formattedEndTime = formattedEndTime.ifBlank { null }
    )

    return START_NOT_STICKY
  }

  private fun playTaskRingtone(ringtoneUriString: String?) {
    val soundUri = try {
      if (!ringtoneUriString.isNullOrBlank()) {
        Uri.parse(ringtoneUriString)
      } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      }
    } catch (_: Exception) {
      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    } ?: return

    val alarmAttrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    var playedViaRingtone = false
    try {
      val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
      if (ringtone != null) {
        ringtone.audioAttributes = alarmAttrs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          ringtone.isLooping = false
        }
        activeRingtone = ringtone
        ringtone.play()
        scheduleRingtoneTimeout()
        playedViaRingtone = true
      }
    } catch (_: Exception) {
      activeRingtone = null
    }

    if (!playedViaRingtone) {
      try {
        val mp = MediaPlayer().apply {
          setAudioAttributes(alarmAttrs)
          setDataSource(applicationContext, soundUri)
          isLooping = false
          setOnCompletionListener {
            stopActiveSoundAlert()
          }
          setOnErrorListener { _, _, _ ->
            stopActiveSoundAlert()
            true
          }
          prepare()
          start()
        }
        activeMediaPlayer = mp
      } catch (_: Exception) {
        stopActiveSoundAlert()
      }
    }
  }

  private fun triggerLazyTtsSpeech(text: String) {
    if (!isVoiceEnabled) {
      releaseTtsEngine()
      return
    }
    if (tts == null) {
      isTtsReady = false
      triedGoogleTtsFallback = false
      pendingSpeechText = text
      try {
        tts = TextToSpeech(applicationContext, this)
      } catch (_: Exception) {
        _serviceState.value = _serviceState.value.copy(isSpeaking = false)
        releaseTtsEngine()
      }
    } else if (isTtsReady) {
      speakMessage(text)
    } else {
      pendingSpeechText = text
    }
  }

  override fun onInit(status: Int) {
    if (!isVoiceEnabled) {
      _serviceState.value = _serviceState.value.copy(isSpeaking = false)
      releaseTtsEngine()
      return
    }
    if (status == TextToSpeech.SUCCESS) {
      try {
        val pending = pendingSpeechText
        if (pending != null &&
          AutomationManager.containsBengali(pending) &&
          !AutomationManager.isBengaliTtsAvailable(tts) &&
          !triedGoogleTtsFallback &&
          AutomationManager.hasGoogleTtsEngine(this)
        ) {
          triedGoogleTtsFallback = true
          isTtsReady = false
          try {
            tts?.shutdown()
          } catch (_: Exception) {}
          tts = TextToSpeech(applicationContext, this, "com.google.android.tts")
          return
        }

        val repo = RoutineRepository(applicationContext)
        val presetId = repo.getVoicePresetId()
        AutomationManager.configureAlarmTtsAttributes(tts)
        AutomationManager.applyTtsLanguage(tts, pending ?: "", presetId)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            _serviceState.value = _serviceState.value.copy(isSpeaking = true)
          }

          override fun onDone(utteranceId: String?) {
            mainHandler.post {
              _serviceState.value = _serviceState.value.copy(isSpeaking = false)
              releaseTtsEngine()
            }
          }

          override fun onError(utteranceId: String?) {
            mainHandler.post {
              _serviceState.value = _serviceState.value.copy(isSpeaking = false)
              releaseTtsEngine()
            }
          }
        })
        isTtsReady = true
        if (isVoiceEnabled) {
          pending?.let {
            speakMessage(it)
            pendingSpeechText = null
          } ?: run {
            _serviceState.value = _serviceState.value.copy(isSpeaking = false)
            releaseTtsEngine()
          }
        }
      } catch (_: Exception) {
        _serviceState.value = _serviceState.value.copy(isSpeaking = false)
        releaseTtsEngine()
      }
    } else {
      if (!triedGoogleTtsFallback && AutomationManager.hasGoogleTtsEngine(this)) {
        triedGoogleTtsFallback = true
        isTtsReady = false
        try {
          tts?.shutdown()
        } catch (_: Exception) {}
        try {
          tts = TextToSpeech(applicationContext, this, "com.google.android.tts")
          return
        } catch (_: Exception) {}
      }
      _serviceState.value = _serviceState.value.copy(isSpeaking = false)
      releaseTtsEngine()
    }
  }

  private fun speakMessage(text: String) {
    if (!isVoiceEnabled || tts == null) {
      _serviceState.value = _serviceState.value.copy(isSpeaking = false)
      releaseTtsEngine()
      return
    }
    try {
      if (AutomationManager.containsBengali(text) &&
        !AutomationManager.isBengaliTtsAvailable(tts) &&
        !triedGoogleTtsFallback &&
        AutomationManager.hasGoogleTtsEngine(this)
      ) {
        triedGoogleTtsFallback = true
        isTtsReady = false
        pendingSpeechText = text
        try {
          tts?.shutdown()
        } catch (_: Exception) {}
        tts = TextToSpeech(applicationContext, this, "com.google.android.tts")
        return
      }
      scheduleTtsSafetyTimeout()
      val repo = RoutineRepository(applicationContext)
      val presetId = repo.getVoicePresetId()
      val effectiveVol = voiceVolume.coerceIn(0.0f, 1.0f)
      AutomationManager.configureAlarmTtsAttributes(tts)
      AutomationManager.applyTtsLanguage(tts, text, presetId)
      requestTransientAudioFocus()
      val params = AutomationManager.createTtsVolumeParams(effectiveVol)
      val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "chronos_voice_${System.currentTimeMillis()}")
      if (speakResult == TextToSpeech.ERROR) {
        _serviceState.value = _serviceState.value.copy(isSpeaking = false)
        releaseTtsEngine()
      }
    } catch (_: Exception) {
      _serviceState.value = _serviceState.value.copy(isSpeaking = false)
      releaseTtsEngine()
    }
  }

  private fun stopAutomation() {
    if (isStopping) return
    isStopping = true
    try {
      dndRunnable?.let { dndHandler?.removeCallbacks(it) }
      dndRunnable = null
      dndHandler = null
      ttsDelayRunnable?.let { mainHandler.removeCallbacks(it) }
      ttsDelayRunnable = null
      stopActiveSoundAlert()
      val stoppedTaskId = activeTaskId
      val activeDndOwner = AutomationManager.getActiveDndTaskId(this)
      if ((currentDndApplied || isDndConfiguredForTask) &&
        (activeDndOwner == null || (stoppedTaskId != null && activeDndOwner == stoppedTaskId)) &&
        !AutomationManager.hasZeroDelayFollowUpDndTask(this, stoppedTaskId)
      ) {
        AutomationManager.setDndMode(this, false, stoppedTaskId)
      }
      currentDndApplied = false
      isDndConfiguredForTask = false
      releaseTtsEngine()
    } catch (_: Exception) {
    } finally {
      activeTaskId = null
      abandonTransientAudioFocus()
      releaseWakeLock()
      _serviceState.value = ActiveAutomationState(isRunning = false, isDndActive = false)
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
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      try {
        nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
      } catch (_: Exception) {}
      val name = "Routine Automation"
      val descriptionText = "Lock-screen routine banners and DND automation"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        setSound(null, null)
        enableVibration(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      nm.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(
    title: String,
    categoryEmoji: String,
    categoryName: String,
    formattedDuration: String,
    formattedEndTime: String = "",
    voiceMessage: String
  ): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPending = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Clean One UI layout without prefixes like "Category:" or "Duration:".
    // Format: ${categoryEmoji} ${categoryName}  •  ${formattedDuration}  •  Ends at ${formattedEndTime}
    val parts = mutableListOf<String>()
    if (categoryName.isNotBlank()) parts.add("$categoryEmoji $categoryName")
    if (formattedDuration.isNotBlank()) parts.add(formattedDuration)
    if (formattedEndTime.isNotBlank()) parts.add("Ends at $formattedEndTime")
    val contentLine = if (parts.isNotEmpty()) parts.joinToString("  •  ") else "$categoryEmoji Routine"

    // Clean, bold task title ONLY (No "Name:" or "Title:" prefix)
    val cleanTitle = title.trim()

    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(cleanTitle)
      .setContentText(contentLine)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setShowWhen(true)
      .setContentIntent(openPending)
      .setOngoing(true)

    // Sub-text / Expanded Body: If custom voice/notification message exists, display it cleanly in BigTextStyle: “${voiceMessage}”
    val cleanVoice = voiceMessage.trim().removeSurrounding("“", "”").removeSurrounding("\"", "\"")
    val expandedText = if (cleanVoice.isNotBlank()) {
      "$contentLine\n\n“$cleanVoice”"
    } else {
      contentLine
    }
    builder.setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))

    return builder.build()
  }

  override fun onDestroy() {
    if (activeInstance === this) {
      activeInstance = null
    }
    try {
      if (!isStopping) {
        stopAutomation()
      } else {
        stopActiveSoundAlert()
        releaseTtsEngine()
      }
    } finally {
      super.onDestroy()
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
