package io.github.szczepanskikrs.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.szczepanskikrs.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthTrackerViewModel(context: Context) : ViewModel() {
    private val dbHelper = DatabaseHelper(context.applicationContext)
    private val sharedPrefs = context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)

    private val _measurements = MutableStateFlow<List<MeasurementEntry>>(emptyList())
    val measurements: StateFlow<List<MeasurementEntry>> = _measurements.asStateFlow()

    private val _exerciseTypes = MutableStateFlow<List<ExerciseType>>(emptyList())
    val exerciseTypes: StateFlow<List<ExerciseType>> = _exerciseTypes.asStateFlow()

    private val _exerciseLogs = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val exerciseLogs: StateFlow<List<ExerciseLog>> = _exerciseLogs.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(false)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(Pair(8, 0))
    val reminderTime: StateFlow<Pair<Int, Int>> = _reminderTime.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        loadData()
        loadSettings()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _measurements.value = dbHelper.getAllMeasurements()
            _exerciseTypes.value = dbHelper.getAllExerciseTypes()
            _exerciseLogs.value = dbHelper.getAllExerciseLogs()
        }
    }

    private fun loadSettings() {
        val enabled = sharedPrefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, false)
        val hour = sharedPrefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 8)
        val minute = sharedPrefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)
        _remindersEnabled.value = enabled
        _reminderTime.value = Pair(hour, minute)
        _themeMode.value = sharedPrefs.getString("theme_mode", "system") ?: "system"
    }

    fun addMeasurement(type: MeasurementType, val1: Double, val2: Double?, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = MeasurementEntry(type = type, value1 = val1, value2 = val2, notes = notes)
            dbHelper.insertMeasurement(entry)
            loadData()
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteMeasurement(id)
            loadData()
        }
    }

    fun addExerciseType(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertExerciseType(name)
            loadData()
        }
    }

    fun deleteExerciseType(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteExerciseType(id)
            loadData()
        }
    }

    fun addExerciseLog(exerciseId: Long, reps: Int, sets: Int, weight: Double?, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = ExerciseLog(exerciseId = exerciseId, reps = reps, sets = sets, weight = weight, notes = notes)
            dbHelper.insertExerciseLog(log)
            loadData()
        }
    }

    fun deleteExerciseLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteExerciseLog(id)
            loadData()
        }
    }

    fun toggleReminders(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            if (enabled) {
                val time = _reminderTime.value
                NotificationHelper.scheduleDailyReminder(context, time.first, time.second)
            } else {
                NotificationHelper.cancelDailyReminder(context)
            }
            _remindersEnabled.value = enabled
        }
    }

    fun updateReminderTime(hour: Int, minute: Int, context: Context) {
        viewModelScope.launch {
            _reminderTime.value = Pair(hour, minute)
            if (_remindersEnabled.value) {
                NotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }
}

class HealthTrackerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthTrackerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
