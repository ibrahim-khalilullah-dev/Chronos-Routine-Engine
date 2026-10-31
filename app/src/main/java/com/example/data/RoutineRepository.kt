package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.engine.SchedulerEngine
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class RoutineRepository(context: Context) {

  private val appContext: Context = context.applicationContext
  private val prefs: SharedPreferences =
    appContext.getSharedPreferences("chronos_routine_prefs", Context.MODE_PRIVATE)

  private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  companion object {
    private const val KEY_ANCHORS = "key_anchors_json"
    private const val KEY_DYNAMIC_TASKS = "key_dynamic_tasks_json"
    private const val KEY_SLEEP_DURATION = "key_sleep_duration_mins"
    private const val KEY_BUFFER_DURATION = "key_buffer_duration_mins"
    private const val KEY_ACTIVE_DAY = "key_active_day"
    private const val PURGE_THRESHOLD_MILLIS = 30L * 24 * 60 * 60 * 1000L // 30 Days
    private const val VAULT_FILE_NAME = "chronos_vault.json"
    private const val VAULT_DIRECTORY_NAME = "ChronosBackup"
  }

  init {
    checkAndAutoRestoreVault()
  }

  /**
   * Checks for persistent offline local vault and automatically restores data if
   * SharedPreferences is in a pristine or unpopulated state.
   * Returns true if a restore was executed.
   */
  fun checkAndAutoRestoreVault(): Boolean {
    val hasStoredAnchors = prefs.contains(KEY_ANCHORS) && !prefs.getString(KEY_ANCHORS, null).isNullOrEmpty()
    val hasStoredDynamic = prefs.contains(KEY_DYNAMIC_TASKS) && !prefs.getString(KEY_DYNAMIC_TASKS, null).isNullOrEmpty()

    // If SharedPreferences already contains data, avoid overwriting
    if (hasStoredAnchors && hasStoredDynamic) {
      return false
    }

    val vaultFile = findVaultFile() ?: return false
    return try {
      val jsonStr = vaultFile.readText()
      if (jsonStr.isBlank()) return false
      val vaultObj = JSONObject(jsonStr)

      val anchorsJson = vaultObj.optJSONArray("anchors")
      val dynamicTasksJson = vaultObj.optJSONArray("dynamicTasks")
      val sleepDuration = vaultObj.optInt("sleepDurationMinutes", 420)
      val bufferMinutes = vaultObj.optInt("bufferMinutes", 5)

      val editor = prefs.edit()
      if (!prefs.contains(KEY_SLEEP_DURATION)) {
        editor.putInt(KEY_SLEEP_DURATION, sleepDuration)
      }
      if (!prefs.contains(KEY_BUFFER_DURATION)) {
        editor.putInt(KEY_BUFFER_DURATION, bufferMinutes)
      }

      var restoredSomething = false
      if (!hasStoredAnchors && anchorsJson != null && anchorsJson.length() > 0) {
        editor.putString(KEY_ANCHORS, anchorsJson.toString())
        restoredSomething = true
      }
      if (!hasStoredDynamic && dynamicTasksJson != null && dynamicTasksJson.length() > 0) {
        editor.putString(KEY_DYNAMIC_TASKS, dynamicTasksJson.toString())
        restoredSomething = true
      }

      if (restoredSomething) {
        editor.commit() // Commit synchronously to ensure immediate availability
        true
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun findVaultFile(): File? {
    // 1. App-specific external files root directory (survives app updates & accessible without storage permissions)
    try {
      val extDir = File(appContext.getExternalFilesDir(null), VAULT_DIRECTORY_NAME)
      val extFile = File(extDir, VAULT_FILE_NAME)
      if (extFile.exists() && extFile.length() > 0) {
        return extFile
      }
    } catch (_: Exception) {}

    // 2. Primary Public Documents directory (survives app uninstall on shared storage)
    try {
      val primaryDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        VAULT_DIRECTORY_NAME
      )
      val primaryFile = File(primaryDir, VAULT_FILE_NAME)
      if (primaryFile.exists() && primaryFile.length() > 0) {
        return primaryFile
      }
    } catch (_: Exception) {}

    // 3. Fallback App-specific external documents directory
    try {
      val fallbackDir = File(
        appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
        VAULT_DIRECTORY_NAME
      )
      val fallbackFile = File(fallbackDir, VAULT_FILE_NAME)
      if (fallbackFile.exists() && fallbackFile.length() > 0) {
        return fallbackFile
      }
    } catch (_: Exception) {}

    // 4. App internal files directory (included in backup_rules.xml)
    try {
      val internalDir = File(appContext.filesDir, VAULT_DIRECTORY_NAME)
      val internalFile = File(internalDir, VAULT_FILE_NAME)
      if (internalFile.exists() && internalFile.length() > 0) {
        return internalFile
      }
    } catch (_: Exception) {}

    return null
  }

  private fun persistVaultAsync() {
    ioScope.launch {
      try {
        persistVaultInternal()
      } catch (_: Exception) {}
    }
  }

  private fun persistVaultInternal() {
    val anchorsRaw = prefs.getString(KEY_ANCHORS, null)
    val dynamicRaw = prefs.getString(KEY_DYNAMIC_TASKS, null)
    val sleepDuration = getSleepDurationMinutes()
    val bufferMinutes = getBufferMinutes()

    val vaultObj = JSONObject().apply {
      put("version", 1)
      put("timestamp", System.currentTimeMillis())
      put("sleepDurationMinutes", sleepDuration)
      put("bufferMinutes", bufferMinutes)
      put("anchors", if (!anchorsRaw.isNullOrEmpty()) JSONArray(anchorsRaw) else JSONArray())
      put("dynamicTasks", if (!dynamicRaw.isNullOrEmpty()) JSONArray(dynamicRaw) else JSONArray())
    }
    val content = vaultObj.toString(2)

    // 1. App-specific external files root directory (guaranteed accessible)
    try {
      val extDir = File(appContext.getExternalFilesDir(null), VAULT_DIRECTORY_NAME)
      if (!extDir.exists()) {
        extDir.mkdirs()
      }
      val extFile = File(extDir, VAULT_FILE_NAME)
      extFile.writeText(content)
    } catch (_: Exception) {}

    // 2. Primary Public Documents directory (survives app uninstall)
    try {
      val primaryDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        VAULT_DIRECTORY_NAME
      )
      if (!primaryDir.exists()) {
        primaryDir.mkdirs()
      }
      val primaryFile = File(primaryDir, VAULT_FILE_NAME)
      primaryFile.writeText(content)
    } catch (_: Exception) {}

    // 3. Fallback App-specific external documents directory
    try {
      val fallbackDir = File(
        appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
        VAULT_DIRECTORY_NAME
      )
      if (!fallbackDir.exists()) {
        fallbackDir.mkdirs()
      }
      val fallbackFile = File(fallbackDir, VAULT_FILE_NAME)
      fallbackFile.writeText(content)
    } catch (_: Exception) {}

    // 4. App internal files directory (included in backup_rules.xml)
    try {
      val internalDir = File(appContext.filesDir, VAULT_DIRECTORY_NAME)
      if (!internalDir.exists()) {
        internalDir.mkdirs()
      }
      val internalFile = File(internalDir, VAULT_FILE_NAME)
      internalFile.writeText(content)
    } catch (_: Exception) {}
  }

  fun getSleepDurationMinutes(): Int {
    return prefs.getInt(KEY_SLEEP_DURATION, 420) // Default 7 hours (420 mins)
  }

  fun setSleepDurationMinutes(minutes: Int) {
    prefs.edit().putInt(KEY_SLEEP_DURATION, minutes).apply()
    persistVaultAsync()
  }

  fun getBufferMinutes(): Int {
    return prefs.getInt(KEY_BUFFER_DURATION, 5)
  }

  fun setBufferMinutes(minutes: Int) {
    prefs.edit().putInt(KEY_BUFFER_DURATION, minutes).apply()
    persistVaultAsync()
  }

  fun getActiveDay(): RoutineDay {
    val systemToday = when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
      java.util.Calendar.SATURDAY -> RoutineDay.SATURDAY
      java.util.Calendar.SUNDAY -> RoutineDay.SUNDAY
      java.util.Calendar.MONDAY -> RoutineDay.MONDAY
      java.util.Calendar.TUESDAY -> RoutineDay.TUESDAY
      java.util.Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      java.util.Calendar.THURSDAY -> RoutineDay.THURSDAY
      java.util.Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }
    val name = prefs.getString(KEY_ACTIVE_DAY, systemToday.name) ?: systemToday.name
    return RoutineDay.fromString(name) ?: systemToday
  }

  fun setActiveDay(day: RoutineDay) {
    prefs.edit().putString(KEY_ACTIVE_DAY, day.name).apply()
  }

  fun getAnchors(): List<RoutineTask> {
    var rawJson = prefs.getString(KEY_ANCHORS, null)
    if (rawJson.isNullOrEmpty()) {
      if (checkAndAutoRestoreVault()) {
        rawJson = prefs.getString(KEY_ANCHORS, null)
      }
    }
    val sleepDuration = getSleepDurationMinutes()
    if (rawJson.isNullOrEmpty()) {
      val defaultAnchors = SchedulerEngine.createDefaultAnchors(sleepDuration)
      saveAnchors(defaultAnchors)
      return defaultAnchors
    }

    return try {
      val list = deserializeTaskList(rawJson)
      SchedulerEngine.calculateAnchors(list, sleepDuration)
    } catch (_: Exception) {
      val defaultAnchors = SchedulerEngine.createDefaultAnchors(sleepDuration)
      saveAnchors(defaultAnchors)
      defaultAnchors
    }
  }

  fun saveAnchors(anchors: List<RoutineTask>) {
    val json = serializeTaskList(anchors)
    prefs.edit().putString(KEY_ANCHORS, json).apply()
    persistVaultAsync()
  }

  fun getDynamicTasks(): List<RoutineTask> {
    var rawJson = prefs.getString(KEY_DYNAMIC_TASKS, null)
    if (rawJson.isNullOrEmpty()) {
      if (checkAndAutoRestoreVault()) {
        rawJson = prefs.getString(KEY_DYNAMIC_TASKS, null)
      }
    }
    if (rawJson.isNullOrEmpty()) {
      return emptyList()
    }

    return try {
      val allTasks = deserializeTaskList(rawJson)
      // Auto-purge any tasks deleted more than 30 days ago
      val now = System.currentTimeMillis()
      val cleaned = allTasks.filter { task ->
        if (task.isDeleted && task.deletedTimestamp != null) {
          (now - task.deletedTimestamp) < PURGE_THRESHOLD_MILLIS
        } else {
          true
        }
      }
      if (cleaned.size != allTasks.size) {
        saveDynamicTasks(cleaned)
      }
      cleaned
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveDynamicTasks(tasks: List<RoutineTask>) {
    val nonSplitTasks = tasks.filter { !it.isSplitPart }
    val json = serializeTaskList(nonSplitTasks)
    prefs.edit().putString(KEY_DYNAMIC_TASKS, json).apply()
    persistVaultAsync()
  }

  fun softDeleteTask(taskId: String): List<RoutineTask> {
    val current = getDynamicTasks()
    val updated = current.map { task ->
      if (task.id == taskId || task.parentTaskId == taskId) {
        task.copy(
          isDeleted = true,
          deletedTimestamp = System.currentTimeMillis()
        )
      } else {
        task
      }
    }
    saveDynamicTasks(updated)
    return updated
  }

  fun restoreTask(taskId: String): List<RoutineTask> {
    val current = getDynamicTasks()
    val updated = current.map { task ->
      if (task.id == taskId || task.parentTaskId == taskId) {
        task.copy(
          isDeleted = false,
          deletedTimestamp = null
        )
      } else {
        task
      }
    }
    saveDynamicTasks(updated)
    return updated
  }

  fun purgeTaskPermanently(taskId: String): List<RoutineTask> {
    val current = getDynamicTasks()
    val updated = current.filter { it.id != taskId && it.parentTaskId != taskId }
    saveDynamicTasks(updated)
    return updated
  }

  fun purgeAllDeletedTasks(): List<RoutineTask> {
    val current = getDynamicTasks()
    val updated = current.filter { !it.isDeleted }
    saveDynamicTasks(updated)
    return updated
  }

  fun getDeletedTasks(): List<RoutineTask> {
    return getDynamicTasks().filter { it.isDeleted }
  }

  private fun serializeTaskList(tasks: List<RoutineTask>): String {
    val jsonArray = JSONArray()
    for (t in tasks) {
      val obj = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("type", t.type.name)
        if (t.slotNumber != null) put("slotNumber", t.slotNumber)
        put("startTimeMinute", t.startTimeMinute)
        put("durationMinutes", t.durationMinutes)
        put("voiceMessage", t.voiceMessage)
        put("enableDnd", t.enableDnd)
        put("enableHaptic", t.enableHaptic)
        put("allowSplitting", t.allowSplitting)
        put("isDeleted", t.isDeleted)
        if (t.deletedTimestamp != null) put("deletedTimestamp", t.deletedTimestamp)
        put("isVoiceEnabled", t.isVoiceEnabled)
        put("customNotificationTitle", t.customNotificationTitle)
        put("customNotificationBody", t.customNotificationBody)
        put("isFluidTwin", t.isFluidTwin)
        if (t.fluidTwinId != null) put("fluidTwinId", t.fluidTwinId)
        if (t.fluidTwinPart != null) put("fluidTwinPart", t.fluidTwinPart)
        if (t.fluidTotalPoolMinutes != null) put("fluidTotalPoolMinutes", t.fluidTotalPoolMinutes)
        if (t.customRingtoneUri != null) put("customRingtoneUri", t.customRingtoneUri)
        if (t.customRingtoneTitle != null) put("customRingtoneTitle", t.customRingtoneTitle)
        put("isSoundAlertEnabled", t.isSoundAlertEnabled)
        put("toleranceMinutes", t.toleranceMinutes)
        put("interTaskBufferMinutes", t.interTaskBufferMinutes)
        if (t.category != null) put("category", t.category)
        if (t.fridayTitle != null) put("fridayTitle", t.fridayTitle)
        if (t.fridayStartTimeMinute != null) put("fridayStartTimeMinute", t.fridayStartTimeMinute)
        if (t.fridayDurationMinutes != null) put("fridayDurationMinutes", t.fridayDurationMinutes)
        if (t.fridayVoiceMessage != null) put("fridayVoiceMessage", t.fridayVoiceMessage)
        put("voiceVolume", t.voiceVolume.toDouble())
        put("isPausedIndefinitely", t.isPausedIndefinitely)
        if (t.pausedUntilDateMillis != null) put("pausedUntilDateMillis", t.pausedUntilDateMillis)
        put("isPausedTodayOnly", t.isPausedTodayOnly)

        val daysArray = JSONArray()
        t.selectedDays.forEach { daysArray.put(it.name) }
        put("selectedDays", daysArray)
      }
      jsonArray.put(obj)
    }
    return jsonArray.toString()
  }

  private fun deserializeTaskList(jsonStr: String): List<RoutineTask> {
    val list = mutableListOf<RoutineTask>()
    val jsonArray = JSONArray(jsonStr)
    for (i in 0 until jsonArray.length()) {
      val obj = jsonArray.getJSONObject(i)
      val daysSet = mutableSetOf<RoutineDay>()
      val daysArray = obj.optJSONArray("selectedDays")
      if (daysArray != null) {
        for (d in 0 until daysArray.length()) {
          RoutineDay.fromString(daysArray.getString(d))?.let { daysSet.add(it) }
        }
      }
      if (daysSet.isEmpty()) daysSet.addAll(RoutineDay.allDays())

      val task = RoutineTask(
        id = obj.getString("id"),
        title = obj.getString("title"),
        type = try {
          TaskType.valueOf(obj.getString("type"))
        } catch (_: Exception) {
          TaskType.DYNAMIC
        },
        slotNumber = if (obj.has("slotNumber")) obj.getInt("slotNumber") else null,
        startTimeMinute = obj.getInt("startTimeMinute"),
        durationMinutes = obj.getInt("durationMinutes"),
        voiceMessage = obj.optString("voiceMessage", ""),
        enableDnd = obj.optBoolean("enableDnd", false),
        enableHaptic = obj.optBoolean("enableHaptic", true),
        allowSplitting = obj.optBoolean("allowSplitting", true),
        selectedDays = daysSet,
        isDeleted = obj.optBoolean("isDeleted", false),
        deletedTimestamp = if (obj.has("deletedTimestamp")) obj.getLong("deletedTimestamp") else null,
        isVoiceEnabled = obj.optBoolean("isVoiceEnabled", true),
        customNotificationTitle = obj.optString("customNotificationTitle", ""),
        customNotificationBody = obj.optString("customNotificationBody", ""),
        isFluidTwin = obj.optBoolean("isFluidTwin", false),
        fluidTwinId = if (obj.has("fluidTwinId")) obj.getString("fluidTwinId") else null,
        fluidTwinPart = if (obj.has("fluidTwinPart")) obj.getInt("fluidTwinPart") else null,
        fluidTotalPoolMinutes = if (obj.has("fluidTotalPoolMinutes")) obj.getInt("fluidTotalPoolMinutes") else null,
        customRingtoneUri = if (obj.has("customRingtoneUri")) obj.getString("customRingtoneUri") else null,
        customRingtoneTitle = if (obj.has("customRingtoneTitle")) obj.getString("customRingtoneTitle") else null,
        isSoundAlertEnabled = obj.optBoolean("isSoundAlertEnabled", true),
        toleranceMinutes = obj.optInt("toleranceMinutes", 0),
        interTaskBufferMinutes = obj.optInt("interTaskBufferMinutes", 5),
        category = if (obj.has("category")) obj.getString("category") else null,
        fridayTitle = if (obj.has("fridayTitle")) obj.getString("fridayTitle") else null,
        fridayStartTimeMinute = if (obj.has("fridayStartTimeMinute")) obj.getInt("fridayStartTimeMinute") else null,
        fridayDurationMinutes = if (obj.has("fridayDurationMinutes")) obj.getInt("fridayDurationMinutes") else null,
        fridayVoiceMessage = if (obj.has("fridayVoiceMessage")) obj.getString("fridayVoiceMessage") else null,
        voiceVolume = if (obj.has("voiceVolume")) obj.getDouble("voiceVolume").toFloat() else 1.0f,
        isPausedIndefinitely = obj.optBoolean("isPausedIndefinitely", false),
        pausedUntilDateMillis = if (obj.has("pausedUntilDateMillis")) obj.getLong("pausedUntilDateMillis") else null,
        isPausedTodayOnly = obj.optBoolean("isPausedTodayOnly", false)
      )
      list.add(task)
    }
    return list
  }
}
