package io.github.szczepanskikrs.data

enum class MeasurementType(val displayName: String, val unit: String) {
    WEIGHT("Waga", "kg"),
    BLOOD_PRESSURE("Ciśnienie krwi", "mmHg"),
    BLOOD_SUGAR("Cukier", "mg/dL")
}

data class MeasurementEntry(
    val id: Long = 0L,
    val type: MeasurementType,
    val value1: Double, // Weight (kg), Systolic (mmHg), or Glucose (mg/dL)
    val value2: Double? = null, // Diastolic (mmHg) for Blood Pressure only
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class ExerciseType(
    val id: Long = 0L,
    val name: String,
    val isCustom: Boolean = false,
    val caloriesPerRep: Double = 0.0
)

data class ExerciseLog(
    val id: Long = 0L,
    val exerciseId: Long,
    val exerciseName: String = "", // Enriched from join
    val reps: Int, // Count (reps) or duration (seconds/minutes)
    val sets: Int,
    val weight: Double? = null, // Weight used in kg (optional), or distance in km for walks
    val calories: Double = 0.0, // Burned calories (estimated or manual)
    val routePath: String? = null, // JSON string of coordinates for walks
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
