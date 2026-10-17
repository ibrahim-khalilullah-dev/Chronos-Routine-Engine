package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.SchedulerEngine
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import org.json.JSONArray
import org.json.JSONObject

class RoutineRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("chronos_routine_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_ANCHORS = "key_anchors_json"
    private const val KEY_DYNAMIC_TASKS = "key_dynamic_tasks_json"
    private const val KEY_SLEEP_DURATION = "key_sleep_duration_mins"
    private const val KEY_BUFFER_DURATION = "key_buffer_duration_mins"
    private const val KEY_ACTIVE_DAY = "key_active_day"
    private const val PURGE_THRESHOLD_MILLIS = 30L * 24 * 60 * 60 * 1000L // 30 Days
  }

  fun getSleepDurationMinutes(): Int {
    return prefs.getInt(KEY_SLEEP_DURATION, 420) // Default 7 hours (420 mins)
  }

  fun setSleepDurationMinutes(minutes: Int) {
    prefs.edit().putInt(KEY_SLEEP_DURATION, minutes).apply()
  }

  fun getBufferMinutes(): Int {
    return prefs.getInt(KEY_BUFFER_DURATION, 5)
  }

  fun setBufferMinutes(minutes: Int) {
    prefs.edit().putInt(KEY_BUFFER_DURATION, minutes).apply()
  }

  fun getActiveDay(): RoutineDay {
    val name = prefs.getString(KEY_ACTIVE_DAY, RoutineDay.MONDAY.name) ?: RoutineDay.MONDAY.name
    return RoutineDay.fromString(name) ?: RoutineDay.MONDAY
  }

  fun setActiveDay(day: RoutineDay) {
    prefs.edit().putString(KEY_ACTIVE_DAY, day.name).apply()
  }

  fun getAnchors(): List<RoutineTask> {
    val rawJson = prefs.getString(KEY_ANCHORS, null)
    val sleepDuration = getSleepDurationMinutes()
    if (rawJson.isNullOrEmpty()) {
      val defaultAnchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDuration)
      saveAnchors(defaultAnchors)
      return defaultAnchors
    }

    return try {
      val list = deserializeTaskList(rawJson)
      SchedulerEngine.calculateAnchors(list, sleepDuration)
    } catch (_: Exception) {
      val defaultAnchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDuration)
      saveAnchors(defaultAnchors)
      defaultAnchors
    }
  }

  fun saveAnchors(anchors: List<RoutineTask>) {
    val json = serializeTaskList(anchors)
    prefs.edit().putString(KEY_ANCHORS, json).apply()
  }

  fun getDynamicTasks(): List<RoutineTask> {
    val rawJson = prefs.getString(KEY_DYNAMIC_TASKS, null)
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
        interTaskBufferMinutes = obj.optInt("interTaskBufferMinutes", 5)
      )
      list.add(task)
    }
    return list
  }
}
