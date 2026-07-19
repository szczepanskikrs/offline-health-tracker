package io.github.szczepanskikrs.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "health_tracker.db"
        private const val DATABASE_VERSION = 8

        // Table Names
        private const val TABLE_MEASUREMENTS = "measurements"
        private const val TABLE_EXERCISE_TYPES = "exercise_types"
        private const val TABLE_EXERCISE_LOGS = "exercise_logs"
        private const val TABLE_RECIPES = "recipes"
        private const val TABLE_MEAL_PLAN = "meal_plan"
        private const val TABLE_RECIPE_INGREDIENTS = "recipe_ingredients"

        // Common Column
        private const val KEY_ID = "id"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_NOTES = "notes"

        // Measurements Table Columns
        private const val KEY_MEASUREMENT_TYPE = "type"
        private const val KEY_VALUE1 = "value1"
        private const val KEY_VALUE2 = "value2"

        // Exercise Types Table Columns
        private const val KEY_EXERCISE_NAME = "name"
        private const val KEY_IS_CUSTOM = "is_custom"
        private const val KEY_CALORIES_PER_REP = "calories_per_rep"

        // Exercise Logs Table Columns
        private const val KEY_EXERCISE_ID = "exercise_id"
        private const val KEY_REPS = "reps"
        private const val KEY_SETS = "sets"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_CALORIES = "calories"
        private const val KEY_ROUTE_PATH = "route_path"

        // Recipes Table Columns
        private const val KEY_RECIPE_NAME = "name"
        private const val KEY_RECIPE_KCAL = "kcal"
        private const val KEY_RECIPE_PROTEIN = "protein"
        private const val KEY_RECIPE_FAT = "fat"
        private const val KEY_RECIPE_CARBS = "carbs"
        private const val KEY_RECIPE_FIBER = "fiber"
        private const val KEY_RECIPE_SALT = "salt"
        private const val KEY_RECIPE_WATER = "water"

        // Meal Plan Table Columns
        private const val KEY_MEAL_DATE = "date"
        private const val KEY_MEAL_INDEX = "meal_index"
        private const val KEY_MEAL_RECIPE_ID = "recipe_id"
        private const val KEY_MEAL_EATEN = "is_eaten"
        private const val KEY_MEAL_SCALE = "scale"

        // Recipe Ingredients Table Columns
        private const val KEY_ING_DISH_ID = "dish_id"
        private const val KEY_ING_NAME = "name"
        private const val KEY_ING_WEIGHT = "weight"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Measurements Table
        val createMeasurementsTable = ("CREATE TABLE $TABLE_MEASUREMENTS ("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_MEASUREMENT_TYPE TEXT,"
                + "$KEY_VALUE1 REAL,"
                + "$KEY_VALUE2 REAL,"
                + "$KEY_TIMESTAMP INTEGER,"
                + "$KEY_NOTES TEXT"
                + ")")
        db.execSQL(createMeasurementsTable)

        // Create Exercise Types Table
        val createExerciseTypesTable = ("CREATE TABLE $TABLE_EXERCISE_TYPES ("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_EXERCISE_NAME TEXT UNIQUE,"
                + "$KEY_IS_CUSTOM INTEGER,"
                + "$KEY_CALORIES_PER_REP REAL DEFAULT 0.0"
                + ")")
        db.execSQL(createExerciseTypesTable)

        // Create Exercise Logs Table
        val createExerciseLogsTable = ("CREATE TABLE $TABLE_EXERCISE_LOGS ("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_EXERCISE_ID INTEGER,"
                + "$KEY_REPS INTEGER,"
                + "$KEY_SETS INTEGER,"
                + "$KEY_WEIGHT REAL,"
                + "$KEY_CALORIES REAL,"
                + "$KEY_ROUTE_PATH TEXT,"
                + "$KEY_TIMESTAMP INTEGER,"
                + "$KEY_NOTES TEXT,"
                + "FOREIGN KEY($KEY_EXERCISE_ID) REFERENCES $TABLE_EXERCISE_TYPES($KEY_ID) ON DELETE CASCADE"
                + ")")
        db.execSQL(createExerciseLogsTable)

        // Create Recipes Table
        val createRecipesTable = ("CREATE TABLE $TABLE_RECIPES ("
                + "$KEY_ID INTEGER PRIMARY KEY,"
                + "$KEY_RECIPE_NAME TEXT,"
                + "$KEY_RECIPE_KCAL REAL,"
                + "$KEY_RECIPE_PROTEIN REAL,"
                + "$KEY_RECIPE_FAT REAL,"
                + "$KEY_RECIPE_CARBS REAL,"
                + "$KEY_RECIPE_FIBER REAL,"
                + "$KEY_RECIPE_SALT REAL,"
                + "$KEY_RECIPE_WATER REAL DEFAULT 0.0"
                + ")")
        db.execSQL(createRecipesTable)

        // Create Meal Plan Table
        val createMealPlanTable = ("CREATE TABLE $TABLE_MEAL_PLAN ("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_MEAL_DATE TEXT,"
                + "$KEY_MEAL_INDEX INTEGER,"
                + "$KEY_MEAL_RECIPE_ID INTEGER,"
                + "$KEY_MEAL_EATEN INTEGER DEFAULT 0,"
                + "$KEY_MEAL_SCALE REAL DEFAULT 1.0,"
                + "FOREIGN KEY($KEY_MEAL_RECIPE_ID) REFERENCES $TABLE_RECIPES($KEY_ID) ON DELETE CASCADE"
                + ")")
        db.execSQL(createMealPlanTable)

        // Create Recipe Ingredients Table
        val createRecipeIngredientsTable = ("CREATE TABLE $TABLE_RECIPE_INGREDIENTS ("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_ING_DISH_ID INTEGER,"
                + "$KEY_ING_NAME TEXT,"
                + "$KEY_ING_WEIGHT REAL"
                + ")")
        db.execSQL(createRecipeIngredientsTable)

        // Prepopulate default exercise types
        insertDefaultExerciseTypes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_EXERCISE_LOGS ADD COLUMN $KEY_CALORIES REAL DEFAULT 0.0")
            db.execSQL("INSERT OR IGNORE INTO $TABLE_EXERCISE_TYPES (name, is_custom) VALUES ('Spacer', 0)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_EXERCISE_LOGS ADD COLUMN $KEY_ROUTE_PATH TEXT")
        }
        if (oldVersion < 4) {
            // Check if column exists first or alter table directly
            try {
                db.execSQL("ALTER TABLE $TABLE_EXERCISE_TYPES ADD COLUMN $KEY_CALORIES_PER_REP REAL DEFAULT 0.0")
            } catch (e: Exception) {
                // Column might already exist in some edge test cases
            }
            db.execSQL("UPDATE $TABLE_EXERCISE_TYPES SET $KEY_CALORIES_PER_REP = 0.35 WHERE $KEY_EXERCISE_NAME = 'Pompki'")
            db.execSQL("UPDATE $TABLE_EXERCISE_TYPES SET $KEY_CALORIES_PER_REP = 0.45 WHERE $KEY_EXERCISE_NAME = 'Przysiady'")
            db.execSQL("UPDATE $TABLE_EXERCISE_TYPES SET $KEY_CALORIES_PER_REP = 0.25 WHERE $KEY_EXERCISE_NAME = 'Mostki'")
            db.execSQL("UPDATE $TABLE_EXERCISE_TYPES SET $KEY_CALORIES_PER_REP = 0.0 WHERE $KEY_EXERCISE_NAME = 'Spacer'")
        }
        if (oldVersion < 5) {
            // Create Recipes Table
            val createRecipesTable = ("CREATE TABLE IF NOT EXISTS $TABLE_RECIPES ("
                    + "$KEY_ID INTEGER PRIMARY KEY,"
                    + "$KEY_RECIPE_NAME TEXT,"
                    + "$KEY_RECIPE_KCAL REAL,"
                    + "$KEY_RECIPE_PROTEIN REAL,"
                    + "$KEY_RECIPE_FAT REAL,"
                    + "$KEY_RECIPE_CARBS REAL,"
                    + "$KEY_RECIPE_FIBER REAL,"
                    + "$KEY_RECIPE_SALT REAL"
                    + ")")
            db.execSQL(createRecipesTable)

            // Create Meal Plan Table
            val createMealPlanTable = ("CREATE TABLE IF NOT EXISTS $TABLE_MEAL_PLAN ("
                    + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "$KEY_MEAL_DATE TEXT,"
                    + "$KEY_MEAL_INDEX INTEGER,"
                    + "$KEY_MEAL_RECIPE_ID INTEGER,"
                    + "$KEY_MEAL_EATEN INTEGER DEFAULT 0,"
                    + "FOREIGN KEY($KEY_MEAL_RECIPE_ID) REFERENCES $TABLE_RECIPES($KEY_ID) ON DELETE CASCADE"
                    + ")")
            db.execSQL(createMealPlanTable)
        }
        if (oldVersion < 6) {
            // Create Recipe Ingredients Table
            val createRecipeIngredientsTable = ("CREATE TABLE IF NOT EXISTS $TABLE_RECIPE_INGREDIENTS ("
                    + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "$KEY_ING_DISH_ID INTEGER,"
                    + "$KEY_ING_NAME TEXT,"
                    + "$KEY_ING_WEIGHT REAL"
                    + ")")
            db.execSQL(createRecipeIngredientsTable)
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MEAL_PLAN ADD COLUMN $KEY_MEAL_SCALE REAL DEFAULT 1.0")
            } catch (e: Exception) {
                // Column might already exist
            }
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $KEY_RECIPE_WATER REAL DEFAULT 0.0")
                // Re-import recipes inside current upgrade transaction to populate new column
                val inputStream = context.assets.open("recipes.csv")
                importRecipesFromCsvWithDb(db, inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun insertDefaultExerciseTypes(db: SQLiteDatabase) {
        val defaults = listOf(
            Triple("Pompki", 0, 0.35),
            Triple("Przysiady", 0, 0.45),
            Triple("Mostki", 0, 0.25),
            Triple("Spacer", 0, 0.0)
        )
        for (item in defaults) {
            val values = ContentValues().apply {
                put(KEY_EXERCISE_NAME, item.first)
                put(KEY_IS_CUSTOM, item.second)
                put(KEY_CALORIES_PER_REP, item.third)
            }
            db.insert(TABLE_EXERCISE_TYPES, null, values)
        }
    }

    // --- MEASUREMENT OPERATIONS ---

    fun insertMeasurement(entry: MeasurementEntry): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_MEASUREMENT_TYPE, entry.type.name)
            put(KEY_VALUE1, entry.value1)
            put(KEY_VALUE2, entry.value2)
            put(KEY_TIMESTAMP, entry.timestamp)
            put(KEY_NOTES, entry.notes)
        }
        return db.insert(TABLE_MEASUREMENTS, null, values)
    }

    fun getAllMeasurements(): List<MeasurementEntry> {
        val list = mutableListOf<MeasurementEntry>()
        val selectQuery = "SELECT * FROM $TABLE_MEASUREMENTS ORDER BY $KEY_TIMESTAMP DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(KEY_ID)
            val typeIndex = cursor.getColumnIndex(KEY_MEASUREMENT_TYPE)
            val v1Index = cursor.getColumnIndex(KEY_VALUE1)
            val v2Index = cursor.getColumnIndex(KEY_VALUE2)
            val tsIndex = cursor.getColumnIndex(KEY_TIMESTAMP)
            val notesIndex = cursor.getColumnIndex(KEY_NOTES)

            do {
                val typeStr = cursor.getString(typeIndex)
                val type = try {
                    MeasurementType.valueOf(typeStr)
                } catch (e: Exception) {
                    MeasurementType.WEIGHT
                }
                
                val val2 = if (cursor.isNull(v2Index)) null else cursor.getDouble(v2Index)

                list.add(
                    MeasurementEntry(
                        id = cursor.getLong(idIndex),
                        type = type,
                        value1 = cursor.getDouble(v1Index),
                        value2 = val2,
                        timestamp = cursor.getLong(tsIndex),
                        notes = cursor.getString(notesIndex) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteMeasurement(id: Long): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_MEASUREMENTS, "$KEY_ID = ?", arrayOf(id.toString())) > 0
    }

    // --- EXERCISE TYPE OPERATIONS ---

    fun insertExerciseType(name: String, isCustom: Boolean = true, caloriesPerRep: Double = 0.0): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_EXERCISE_NAME, name)
            put(KEY_IS_CUSTOM, if (isCustom) 1 else 0)
            put(KEY_CALORIES_PER_REP, caloriesPerRep)
        }
        return db.insertWithOnConflict(TABLE_EXERCISE_TYPES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getAllExerciseTypes(): List<ExerciseType> {
        val list = mutableListOf<ExerciseType>()
        val selectQuery = "SELECT * FROM $TABLE_EXERCISE_TYPES ORDER BY $KEY_IS_CUSTOM ASC, $KEY_EXERCISE_NAME ASC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(KEY_ID)
            val nameIndex = cursor.getColumnIndex(KEY_EXERCISE_NAME)
            val isCustomIndex = cursor.getColumnIndex(KEY_IS_CUSTOM)
            val caloriesPerRepIndex = cursor.getColumnIndex(KEY_CALORIES_PER_REP)

            do {
                val caloriesPerRep = if (caloriesPerRepIndex != -1 && !cursor.isNull(caloriesPerRepIndex)) {
                    cursor.getDouble(caloriesPerRepIndex)
                } else {
                    0.0
                }
                list.add(
                    ExerciseType(
                        id = cursor.getLong(idIndex),
                        name = cursor.getString(nameIndex) ?: "",
                        isCustom = cursor.getInt(isCustomIndex) == 1,
                        caloriesPerRep = caloriesPerRep
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteExerciseType(id: Long): Boolean {
        val db = this.writableDatabase
        // Also cascade delete exercise logs associated with this exercise
        db.delete(TABLE_EXERCISE_LOGS, "$KEY_EXERCISE_ID = ?", arrayOf(id.toString()))
        return db.delete(TABLE_EXERCISE_TYPES, "$KEY_ID = ?", arrayOf(id.toString())) > 0
    }

    // --- EXERCISE LOG OPERATIONS ---

    fun insertExerciseLog(log: ExerciseLog): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_EXERCISE_ID, log.exerciseId)
            put(KEY_REPS, log.reps)
            put(KEY_SETS, log.sets)
            put(KEY_WEIGHT, log.weight)
            put(KEY_CALORIES, log.calories)
            put(KEY_ROUTE_PATH, log.routePath)
            put(KEY_TIMESTAMP, log.timestamp)
            put(KEY_NOTES, log.notes)
        }
        return db.insert(TABLE_EXERCISE_LOGS, null, values)
    }

    fun getAllExerciseLogs(): List<ExerciseLog> {
        val list = mutableListOf<ExerciseLog>()
        val selectQuery = """
            SELECT l.*, t.$KEY_EXERCISE_NAME 
            FROM $TABLE_EXERCISE_LOGS l
            JOIN $TABLE_EXERCISE_TYPES t ON l.$KEY_EXERCISE_ID = t.$KEY_ID
            ORDER BY l.$KEY_TIMESTAMP DESC
        """.trimIndent()
        
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(KEY_ID)
            val exIdIndex = cursor.getColumnIndex(KEY_EXERCISE_ID)
            val nameIndex = cursor.getColumnIndex(KEY_EXERCISE_NAME)
            val repsIndex = cursor.getColumnIndex(KEY_REPS)
            val setsIndex = cursor.getColumnIndex(KEY_SETS)
            val weightIndex = cursor.getColumnIndex(KEY_WEIGHT)
            val caloriesIndex = cursor.getColumnIndex(KEY_CALORIES)
            val routePathIndex = cursor.getColumnIndex(KEY_ROUTE_PATH)
            val tsIndex = cursor.getColumnIndex(KEY_TIMESTAMP)
            val notesIndex = cursor.getColumnIndex(KEY_NOTES)

            do {
                val weightVal = if (cursor.isNull(weightIndex)) null else cursor.getDouble(weightIndex)
                val caloriesVal = if (caloriesIndex != -1 && !cursor.isNull(caloriesIndex)) cursor.getDouble(caloriesIndex) else 0.0
                val routePathVal = if (routePathIndex != -1 && !cursor.isNull(routePathIndex)) cursor.getString(routePathIndex) else null
                list.add(
                    ExerciseLog(
                        id = cursor.getLong(idIndex),
                        exerciseId = cursor.getLong(exIdIndex),
                        exerciseName = cursor.getString(nameIndex) ?: "Nieznane",
                        reps = cursor.getInt(repsIndex),
                        sets = cursor.getInt(setsIndex),
                        weight = weightVal,
                        calories = caloriesVal,
                        routePath = routePathVal,
                        timestamp = cursor.getLong(tsIndex),
                        notes = cursor.getString(notesIndex) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteExerciseLog(id: Long): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_EXERCISE_LOGS, "$KEY_ID = ?", arrayOf(id.toString())) > 0
    }

    // --- RECIPES & MEAL PLAN OPERATIONS ---

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val curVal = StringBuilder()
        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal.setLength(0)
            } else {
                curVal.append(ch)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }

    fun isRecipesTableEmpty(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_RECIPES", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count == 0
    }

    fun importRecipesFromCsv(inputStream: java.io.InputStream) {
        val db = this.writableDatabase
        importRecipesFromCsvWithDb(db, inputStream)
    }

    fun importRecipesFromCsvWithDb(db: SQLiteDatabase, inputStream: java.io.InputStream) {
        db.beginTransaction()
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream, "UTF-8"))
            val headerLine = reader.readLine() ?: return
            val headers = parseCsvLine(headerLine)
            
            // Find indices of columns
            val idxId = headers.indexOf("id")
            val idxName = headers.indexOf("name")
            val idxKcal = headers.indexOf("Wartość energetyczna [kcal]")
            val idxProtein = headers.indexOf("Białko ogółem [g]")
            val idxFat = headers.indexOf("Tłuszcz [g]")
            val idxCarbs = headers.indexOf("Węglowodany ogółem [g]")
            val idxFiber = headers.indexOf("Błonnik pokarmowy [g]")
            val idxSalt = headers.indexOf("Sól [g]")
            val idxWater = headers.indexOf("Woda [g]")

            var line = reader.readLine()
            while (line != null) {
                if (line.isBlank()) {
                    line = reader.readLine()
                    continue
                }
                val parts = parseCsvLine(line)
                if (parts.size > idxId && parts.size > idxName) {
                    val idVal = parts[idxId].toLongOrNull()
                    val nameVal = parts[idxName]
                    if (idVal != null && nameVal.isNotBlank()) {
                        val kcalVal = if (idxKcal != -1 && idxKcal < parts.size) parts[idxKcal].toDoubleOrNull() ?: 0.0 else 0.0
                        val proteinVal = if (idxProtein != -1 && idxProtein < parts.size) parts[idxProtein].toDoubleOrNull() ?: 0.0 else 0.0
                        val fatVal = if (idxFat != -1 && idxFat < parts.size) parts[idxFat].toDoubleOrNull() ?: 0.0 else 0.0
                        val carbsVal = if (idxCarbs != -1 && idxCarbs < parts.size) parts[idxCarbs].toDoubleOrNull() ?: 0.0 else 0.0
                        val fiberVal = if (idxFiber != -1 && idxFiber < parts.size) parts[idxFiber].toDoubleOrNull() ?: 0.0 else 0.0
                        val saltVal = if (idxSalt != -1 && idxSalt < parts.size) parts[idxSalt].toDoubleOrNull() ?: 0.0 else 0.0
                        val waterVal = if (idxWater != -1 && idxWater < parts.size) parts[idxWater].toDoubleOrNull() ?: 0.0 else 0.0

                        val values = ContentValues().apply {
                            put(KEY_ID, idVal)
                            put(KEY_RECIPE_NAME, nameVal)
                            put(KEY_RECIPE_KCAL, kcalVal)
                            put(KEY_RECIPE_PROTEIN, proteinVal)
                            put(KEY_RECIPE_FAT, fatVal)
                            put(KEY_RECIPE_CARBS, carbsVal)
                            put(KEY_RECIPE_FIBER, fiberVal)
                            put(KEY_RECIPE_SALT, saltVal)
                            put(KEY_RECIPE_WATER, waterVal)
                        }
                        db.insertWithOnConflict(TABLE_RECIPES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
                line = reader.readLine()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllRecipes(): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val selectQuery = "SELECT * FROM $TABLE_RECIPES ORDER BY $KEY_RECIPE_NAME ASC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndex(KEY_ID)
            val nameIdx = cursor.getColumnIndex(KEY_RECIPE_NAME)
            val kcalIdx = cursor.getColumnIndex(KEY_RECIPE_KCAL)
            val protIdx = cursor.getColumnIndex(KEY_RECIPE_PROTEIN)
            val fatIdx = cursor.getColumnIndex(KEY_RECIPE_FAT)
            val carbsIdx = cursor.getColumnIndex(KEY_RECIPE_CARBS)
            val fibIdx = cursor.getColumnIndex(KEY_RECIPE_FIBER)
            val saltIdx = cursor.getColumnIndex(KEY_RECIPE_SALT)

            do {
                list.add(
                    Recipe(
                        id = cursor.getLong(idIdx),
                        name = cursor.getString(nameIdx) ?: "",
                        kcal = cursor.getDouble(kcalIdx),
                        protein = cursor.getDouble(protIdx),
                        fat = cursor.getDouble(fatIdx),
                        carbs = cursor.getDouble(carbsIdx),
                        fiber = cursor.getDouble(fibIdx),
                        salt = cursor.getDouble(saltIdx)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertMealPlanEntry(date: String, mealIndex: Int, recipeId: Long, scale: Double = 1.0): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_MEAL_DATE, date)
            put(KEY_MEAL_INDEX, mealIndex)
            put(KEY_MEAL_RECIPE_ID, recipeId)
            put(KEY_MEAL_EATEN, 0)
            put(KEY_MEAL_SCALE, scale)
        }
        return db.insert(TABLE_MEAL_PLAN, null, values)
    }

    fun getMealPlanForDate(date: String): List<MealPlanEntry> {
        val list = mutableListOf<MealPlanEntry>()
        val selectQuery = """
            SELECT m.*, r.$KEY_RECIPE_NAME, r.$KEY_RECIPE_KCAL, r.$KEY_RECIPE_PROTEIN, r.$KEY_RECIPE_FAT, r.$KEY_RECIPE_CARBS, r.$KEY_RECIPE_FIBER, r.$KEY_RECIPE_SALT
            FROM $TABLE_MEAL_PLAN m
            JOIN $TABLE_RECIPES r ON m.$KEY_MEAL_RECIPE_ID = r.$KEY_ID
            WHERE m.$KEY_MEAL_DATE = ?
            ORDER BY m.$KEY_MEAL_INDEX ASC
        """.trimIndent()

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(date))

        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndex(KEY_ID)
            val dateIdx = cursor.getColumnIndex(KEY_MEAL_DATE)
            val mealIdxIdx = cursor.getColumnIndex(KEY_MEAL_INDEX)
            val recIdIdx = cursor.getColumnIndex(KEY_MEAL_RECIPE_ID)
            val eatenIdx = cursor.getColumnIndex(KEY_MEAL_EATEN)
            val scaleIdx = cursor.getColumnIndex(KEY_MEAL_SCALE)
            val nameIdx = cursor.getColumnIndex(KEY_RECIPE_NAME)
            val kcalIdx = cursor.getColumnIndex(KEY_RECIPE_KCAL)
            val protIdx = cursor.getColumnIndex(KEY_RECIPE_PROTEIN)
            val fatIdx = cursor.getColumnIndex(KEY_RECIPE_FAT)
            val carbsIdx = cursor.getColumnIndex(KEY_RECIPE_CARBS)
            val fibIdx = cursor.getColumnIndex(KEY_RECIPE_FIBER)
            val saltIdx = cursor.getColumnIndex(KEY_RECIPE_SALT)

            do {
                val scale = if (scaleIdx >= 0) cursor.getDouble(scaleIdx) else 1.0
                list.add(
                    MealPlanEntry(
                        id = cursor.getLong(idIdx),
                        date = cursor.getString(dateIdx) ?: "",
                        mealIndex = cursor.getInt(mealIdxIdx),
                        recipeId = cursor.getLong(recIdIdx),
                        recipeName = cursor.getString(nameIdx) ?: "",
                        kcal = cursor.getDouble(kcalIdx) * scale,
                        protein = cursor.getDouble(protIdx) * scale,
                        fat = cursor.getDouble(fatIdx) * scale,
                        carbs = cursor.getDouble(carbsIdx) * scale,
                        fiber = cursor.getDouble(fibIdx) * scale,
                        salt = cursor.getDouble(saltIdx) * scale,
                        scale = scale,
                        isEaten = cursor.getInt(eatenIdx) == 1
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateMealPlanEntryEaten(id: Long, isEaten: Boolean): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_MEAL_EATEN, if (isEaten) 1 else 0)
        }
        return db.update(TABLE_MEAL_PLAN, values, "$KEY_ID = ?", arrayOf(id.toString())) > 0
    }

    fun clearMealPlanForDate(date: String): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_MEAL_PLAN, "$KEY_MEAL_DATE = ?", arrayOf(date)) > 0
    }

    fun clearMealPlanForRange(startDate: String, endDate: String): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_MEAL_PLAN, "$KEY_MEAL_DATE BETWEEN ? AND ?", arrayOf(startDate, endDate)) > 0
    }

    // --- INGREDIENTS & SHOPPING LIST OPERATIONS ---

    fun isIngredientsTableEmpty(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_RECIPE_INGREDIENTS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count == 0
    }

    fun importIngredientsFromCsv(inputStream: java.io.InputStream) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream, "UTF-8"))
            val headerLine = reader.readLine() ?: return
            val headers = parseCsvLine(headerLine)
            
            val idxDishId = headers.indexOf("dish_id")
            val idxName = headers.indexOf("name")
            val idxWeight = headers.indexOf("weight [g]")

            var line = reader.readLine()
            while (line != null) {
                if (line.isBlank()) {
                    line = reader.readLine()
                    continue
                }
                val parts = parseCsvLine(line)
                if (parts.size > idxDishId && parts.size > idxName && parts.size > idxWeight) {
                    val dishIdVal = parts[idxDishId].toLongOrNull()
                    val nameVal = parts[idxName]
                    val weightVal = parts[idxWeight].toDoubleOrNull() ?: 0.0

                    if (dishIdVal != null && nameVal.isNotBlank()) {
                        val values = ContentValues().apply {
                            put(KEY_ING_DISH_ID, dishIdVal)
                            put(KEY_ING_NAME, nameVal)
                            put(KEY_ING_WEIGHT, weightVal)
                        }
                        db.insert(TABLE_RECIPE_INGREDIENTS, null, values)
                    }
                }
                line = reader.readLine()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredient> {
        val list = mutableListOf<RecipeIngredient>()
        val db = this.readableDatabase

        // Fetch serving weight components
        val recipeQuery = "SELECT water, protein, fat, carbs, fiber, salt FROM $TABLE_RECIPES WHERE id = ?"
        var servingWeight = 0.0
        val rCursor = db.rawQuery(recipeQuery, arrayOf(recipeId.toString()))
        if (rCursor.moveToFirst()) {
            servingWeight = rCursor.getDouble(0) + rCursor.getDouble(1) + rCursor.getDouble(2) + rCursor.getDouble(3) + rCursor.getDouble(4) + rCursor.getDouble(5)
        }
        rCursor.close()

        // Fetch sum of raw weights
        val rawQuery = "SELECT SUM($KEY_ING_WEIGHT) FROM $TABLE_RECIPE_INGREDIENTS WHERE $KEY_ING_DISH_ID = ?"
        var rawWeight = 0.0
        val rawCursor = db.rawQuery(rawQuery, arrayOf(recipeId.toString()))
        if (rawCursor.moveToFirst()) {
            rawWeight = rawCursor.getDouble(0)
        }
        rawCursor.close()

        // If recipe serving weight and raw weight are available, scale ingredients to 1 serving
        val servingScale = if (rawWeight > 0.0 && servingWeight > 0.0) servingWeight / rawWeight else 1.0

        val selectQuery = "SELECT * FROM $TABLE_RECIPE_INGREDIENTS WHERE $KEY_ING_DISH_ID = ? ORDER BY $KEY_ING_NAME ASC"
        val cursor = db.rawQuery(selectQuery, arrayOf(recipeId.toString()))

        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndex(KEY_ID)
            val dishIdIdx = cursor.getColumnIndex(KEY_ING_DISH_ID)
            val nameIdx = cursor.getColumnIndex(KEY_ING_NAME)
            val weightIdx = cursor.getColumnIndex(KEY_ING_WEIGHT)

            do {
                list.add(
                    RecipeIngredient(
                        id = cursor.getLong(idIdx),
                        dishId = cursor.getLong(dishIdIdx),
                        name = cursor.getString(nameIdx) ?: "",
                        weight = cursor.getDouble(weightIdx) * servingScale
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getWeeklyShoppingList(startDate: String, endDate: String): List<Pair<String, Double>> {
        val list = mutableListOf<Pair<String, Double>>()
        val selectQuery = """
            SELECT ri.$KEY_ING_NAME, 
                   SUM(ri.$KEY_ING_WEIGHT * COALESCE(mp.$KEY_MEAL_SCALE, 1.0) * 
                       COALESCE(
                           (SELECT (r.water + r.protein + r.fat + r.carbs + r.fiber + r.salt) 
                            FROM $TABLE_RECIPES r 
                            WHERE r.id = mp.$KEY_MEAL_RECIPE_ID) / 
                           NULLIF((SELECT SUM(weight) 
                                   FROM $TABLE_RECIPE_INGREDIENTS 
                                   WHERE $KEY_ING_DISH_ID = mp.$KEY_MEAL_RECIPE_ID), 0), 
                           1.0
                       )
                   )
            FROM $TABLE_MEAL_PLAN mp
            JOIN $TABLE_RECIPE_INGREDIENTS ri ON mp.$KEY_MEAL_RECIPE_ID = ri.$KEY_ING_DISH_ID
            WHERE mp.$KEY_MEAL_DATE BETWEEN ? AND ?
            GROUP BY ri.$KEY_ING_NAME
            ORDER BY ri.$KEY_ING_NAME ASC
        """.trimIndent()

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(startDate, endDate))

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(0) ?: ""
                val weight = cursor.getDouble(1)
                list.add(Pair(name, weight))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
