package io.github.szczepanskikrs.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "health_tracker.db"
        private const val DATABASE_VERSION = 2

        // Table Names
        private const val TABLE_MEASUREMENTS = "measurements"
        private const val TABLE_EXERCISE_TYPES = "exercise_types"
        private const val TABLE_EXERCISE_LOGS = "exercise_logs"

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

        // Exercise Logs Table Columns
        private const val KEY_EXERCISE_ID = "exercise_id"
        private const val KEY_REPS = "reps"
        private const val KEY_SETS = "sets"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_CALORIES = "calories"
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
                + "$KEY_IS_CUSTOM INTEGER"
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
                + "$KEY_TIMESTAMP INTEGER,"
                + "$KEY_NOTES TEXT,"
                + "FOREIGN KEY($KEY_EXERCISE_ID) REFERENCES $TABLE_EXERCISE_TYPES($KEY_ID) ON DELETE CASCADE"
                + ")")
        db.execSQL(createExerciseLogsTable)

        // Prepopulate default exercise types
        insertDefaultExerciseTypes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_EXERCISE_LOGS ADD COLUMN $KEY_CALORIES REAL DEFAULT 0.0")
            db.execSQL("INSERT OR IGNORE INTO $TABLE_EXERCISE_TYPES (name, is_custom) VALUES ('Spacer', 0)")
        }
    }

    private fun insertDefaultExerciseTypes(db: SQLiteDatabase) {
        val defaults = listOf("Pompki", "Przysiady", "Mostki", "Spacer")
        for (name in defaults) {
            val values = ContentValues().apply {
                put(KEY_EXERCISE_NAME, name)
                put(KEY_IS_CUSTOM, 0) // Built-in
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

    fun insertExerciseType(name: String, isCustom: Boolean = true): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_EXERCISE_NAME, name)
            put(KEY_IS_CUSTOM, if (isCustom) 1 else 0)
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

            do {
                list.add(
                    ExerciseType(
                        id = cursor.getLong(idIndex),
                        name = cursor.getString(nameIndex) ?: "",
                        isCustom = cursor.getInt(isCustomIndex) == 1
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
            val tsIndex = cursor.getColumnIndex(KEY_TIMESTAMP)
            val notesIndex = cursor.getColumnIndex(KEY_NOTES)

            do {
                val weightVal = if (cursor.isNull(weightIndex)) null else cursor.getDouble(weightIndex)
                val caloriesVal = if (caloriesIndex != -1 && !cursor.isNull(caloriesIndex)) cursor.getDouble(caloriesIndex) else 0.0
                list.add(
                    ExerciseLog(
                        id = cursor.getLong(idIndex),
                        exerciseId = cursor.getLong(exIdIndex),
                        exerciseName = cursor.getString(nameIndex) ?: "Nieznane",
                        reps = cursor.getInt(repsIndex),
                        sets = cursor.getInt(setsIndex),
                        weight = weightVal,
                        calories = caloriesVal,
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
}
