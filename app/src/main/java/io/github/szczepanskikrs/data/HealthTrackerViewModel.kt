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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

class HealthTrackerViewModel(private val context: Context) : ViewModel() {
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

    // Meal Plan states
    private val _selectedMealPlanDate = MutableStateFlow(getCurrentDateString())
    val selectedMealPlanDate: StateFlow<String> = _selectedMealPlanDate.asStateFlow()

    private val _mealPlan = MutableStateFlow<List<MealPlanEntry>>(emptyList())
    val mealPlan: StateFlow<List<MealPlanEntry>> = _mealPlan.asStateFlow()

    private val _recipesCount = MutableStateFlow(0)
    val recipesCount: StateFlow<Int> = _recipesCount.asStateFlow()

    private val _isGeneratingMealPlan = MutableStateFlow(false)
    val isGeneratingMealPlan: StateFlow<Boolean> = _isGeneratingMealPlan.asStateFlow()

    private val _weeklyShoppingList = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val weeklyShoppingList: StateFlow<List<Pair<String, Double>>> = _weeklyShoppingList.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Import recipes from CSV if empty
            if (dbHelper.isRecipesTableEmpty()) {
                try {
                    val inputStream = context.assets.open("recipes.csv")
                    dbHelper.importRecipesFromCsv(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Import ingredients from CSV if empty
            if (dbHelper.isIngredientsTableEmpty()) {
                try {
                    val inputStream = context.assets.open("ingredients.csv")
                    dbHelper.importIngredientsFromCsv(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadData()
            loadSettings()
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _measurements.value = dbHelper.getAllMeasurements()
            _exerciseTypes.value = dbHelper.getAllExerciseTypes()
            _exerciseLogs.value = dbHelper.getAllExerciseLogs()
            
            // Load recipes count and meal plan
            _recipesCount.value = dbHelper.getAllRecipes().size
            _mealPlan.value = dbHelper.getMealPlanForDate(_selectedMealPlanDate.value)
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

    fun addExerciseType(name: String, caloriesPerRep: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertExerciseType(name, true, caloriesPerRep)
            loadData()
        }
    }

    fun deleteExerciseType(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteExerciseType(id)
            loadData()
        }
    }

    fun addExerciseLog(exerciseId: Long, reps: Int, sets: Int, weight: Double?, calories: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = ExerciseLog(exerciseId = exerciseId, reps = reps, sets = sets, weight = weight, calories = calories, routePath = null, notes = notes)
            dbHelper.insertExerciseLog(log)
            loadData()
        }
    }

    fun addWalkLog(exerciseId: Long, durationMinutes: Int, distanceKm: Double, calories: Double, routePathJson: String?, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = ExerciseLog(
                exerciseId = exerciseId,
                reps = durationMinutes,
                sets = 1,
                weight = distanceKm,
                calories = calories,
                routePath = routePathJson,
                notes = notes
            )
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

    // --- MEAL PLAN OPERATIONS ---

    fun selectMealPlanDate(date: String) {
        _selectedMealPlanDate.value = date
        loadMealPlanForSelectedDate()
    }

    fun loadMealPlanForSelectedDate() {
        viewModelScope.launch(Dispatchers.IO) {
            _mealPlan.value = dbHelper.getMealPlanForDate(_selectedMealPlanDate.value)
        }
    }

    fun toggleMealPlanEntryEaten(id: Long, isEaten: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateMealPlanEntryEaten(id, isEaten)
            loadMealPlanForSelectedDate()
        }
    }

    fun rollAlternativeMeal(mealEntryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentEntry = _mealPlan.value.find { it.id == mealEntryId } ?: return@launch
            val recipes = dbHelper.getAllRecipes()
            if (recipes.isEmpty()) return@launch

            // Find the recipe to determine its category
            val currentRecipe = recipes.find { it.id == currentEntry.recipeId } ?: return@launch
            val currentCategory = currentRecipe.category

            val targetKcal = currentEntry.kcal
            // Find alternative recipes of the SAME category within +/- 150 kcal, excluding current one
            val alternatives = recipes.filter { 
                it.category == currentCategory && 
                Math.abs(it.kcal - targetKcal) <= 150.0 && 
                it.id != currentEntry.recipeId 
            }
            val pool = if (alternatives.isNotEmpty()) alternatives else recipes.filter { it.category == currentCategory }
            val finalPool = if (pool.isNotEmpty()) pool else recipes
            val newRecipe = finalPool.random()

            // Update in database
            val db = dbHelper.writableDatabase
            val values = android.content.ContentValues().apply {
                put("recipe_id", newRecipe.id)
                put("is_eaten", 0)
            }
            db.update("meal_plan", values, "id = ?", arrayOf(mealEntryId.toString()))
            loadMealPlanForSelectedDate()
        }
    }

    fun generateWeeklyMealPlan(
        startDateString: String, 
        dailyKcalTarget: Double, 
        numMealsPerDay: Int, 
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingMealPlan.value = true
            var success = false
            try {
                val recipes = dbHelper.getAllRecipes()
                if (recipes.isNotEmpty()) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startCalendar = Calendar.getInstance()
                    val startDate = sdf.parse(startDateString)
                    if (startDate != null) {
                        startCalendar.time = startDate
                    }

                    // Clear the existing meal plan for this 7-day range first
                    val endCalendar = startCalendar.clone() as Calendar
                    endCalendar.add(Calendar.DAY_OF_YEAR, 6)
                    val endDateString = sdf.format(endCalendar.time)

                    dbHelper.clearMealPlanForRange(startDateString, endDateString)

                    // To ensure diversity/variety, we keep track of recipes selected in the last 2 days
                    val recentRecipeIds = mutableSetOf<Long>()
                    val recentHistory = mutableListOf<Set<Long>>() // list of sets for each day

                    val random = Random()

                    // Prepare slot categories depending on numMealsPerDay
                    val slotCategories = getSlotCategories(numMealsPerDay)

                    for (dayOffset in 0..6) {
                        val currentDayCalendar = startCalendar.clone() as Calendar
                        currentDayCalendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                        val currentDayString = sdf.format(currentDayCalendar.time)

                        // Generate N meals for this day
                        val dayRecipes = generateDailyRecipesForSlots(
                            recipes = recipes,
                            targetKcal = dailyKcalTarget,
                            slotCategories = slotCategories,
                            avoidIds = recentRecipeIds,
                            random = random
                        )

                        // Save to database
                        for ((index, recipe) in dayRecipes.withIndex()) {
                            dbHelper.insertMealPlanEntry(currentDayString, index, recipe.id)
                        }

                        // Update history for variety: only avoid recipes used in the last 2 days
                        val dayRecipeIds = dayRecipes.map { it.id }.toSet()
                        recentHistory.add(dayRecipeIds)
                        recentRecipeIds.addAll(dayRecipeIds)
                        if (recentHistory.size > 2) {
                            val removedDay = recentHistory.removeAt(0)
                            recentRecipeIds.removeAll(removedDay)
                        }
                    }
                    success = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (success) {
                    _selectedMealPlanDate.value = startDateString
                }
                _isGeneratingMealPlan.value = false
                loadMealPlanForSelectedDate()
                viewModelScope.launch(Dispatchers.Main) {
                    onComplete(success)
                }
            }
        }
    }

    private fun getSlotCategories(numMeals: Int): List<List<MealType>> {
        return when (numMeals) {
            3 -> listOf(
                listOf(MealType.BREAKFAST),                       // Śniadanie
                listOf(MealType.LUNCH),                           // Obiad
                listOf(MealType.SUPPER, MealType.BREAKFAST)       // Kolacja
            )
            4 -> listOf(
                listOf(MealType.BREAKFAST),                       // Śniadanie
                listOf(MealType.SNACK),                           // Drugie śniadanie
                listOf(MealType.LUNCH),                           // Obiad
                listOf(MealType.SUPPER, MealType.BREAKFAST)       // Kolacja
            )
            5 -> listOf(
                listOf(MealType.BREAKFAST),                       // Śniadanie
                listOf(MealType.SNACK),                           // Drugie śniadanie
                listOf(MealType.LUNCH),                           // Obiad
                listOf(MealType.SNACK),                           // Podwieczorek
                listOf(MealType.SUPPER, MealType.BREAKFAST)       // Kolacja
            )
            else -> List(numMeals) { listOf(MealType.BREAKFAST, MealType.SNACK, MealType.LUNCH, MealType.SUPPER) }
        }
    }

    private fun generateDailyRecipesForSlots(
        recipes: List<Recipe>,
        targetKcal: Double,
        slotCategories: List<List<MealType>>,
        avoidIds: Set<Long>,
        random: Random
    ): List<Recipe> {
        var bestCombo = emptyList<Recipe>()
        var bestScore = Double.MAX_VALUE

        // Prepare pool for each slot
        val slotPools = slotCategories.map { categories ->
            val slotRecipes = recipes.filter { it.category in categories }
            val preferredPool = slotRecipes.filter { it.id !in avoidIds }
            if (preferredPool.isNotEmpty()) preferredPool else if (slotRecipes.isNotEmpty()) slotRecipes else recipes
        }

        for (trial in 0 until 1000) {
            val candidate = mutableListOf<Recipe>()
            val chosenIds = mutableSetOf<Long>()
            
            for (slotIndex in slotPools.indices) {
                val pool = slotPools[slotIndex]
                var recipe = pool[random.nextInt(pool.size)]
                // Avoid duplicates in the same day if possible (retry up to 5 times)
                var retries = 5
                while (recipe.id in chosenIds && retries > 0) {
                    recipe = pool[random.nextInt(pool.size)]
                    retries--
                }
                candidate.add(recipe)
                chosenIds.add(recipe.id)
            }

            val sumKcal = candidate.sumOf { it.kcal }
            val diff = Math.abs(sumKcal - targetKcal)
            if (diff < bestScore) {
                bestScore = diff
                bestCombo = candidate
                if (diff < 15.0) {
                    break
                }
            }
        }
        return bestCombo
    }

    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredient> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            dbHelper.getIngredientsForRecipe(recipeId)
        }
    }

    fun loadWeeklyShoppingList(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _weeklyShoppingList.value = dbHelper.getWeeklyShoppingList(startDate, endDate)
        }
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
