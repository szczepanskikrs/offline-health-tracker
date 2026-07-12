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

enum class MealType {
    BREAKFAST,  // Śniadanie
    SNACK,      // Drugie śniadanie / Podwieczorek
    LUNCH,      // Obiad
    SUPPER      // Kolacja
}

fun classifyRecipe(name: String, kcal: Double): MealType {
    val nameLower = name.lowercase()
    
    // Obiad keywords
    val lunchKeywords = listOf(
        "zupa", "krem z", "kotlet", "schab", "pierś", "kurczak", "indyk", "wołowina", "wieprzowina", 
        "gulasz", "pieczeń", "ryba", "łosoś", "dorsz", "pstrąg", "makaron", "spaghetti", "ryż", 
        "kasza", "ziemniaki", "placki", "naleśniki", "krokiety", "pierogi", "gołąbki", "bigos",
        "pyzy", "kopytka", "gulasz", "pieczony", "smażony", "duszony", "potrawka", "warzywa na patelnię"
    )
    
    // Śniadanie keywords
    val breakfastKeywords = listOf(
        "owsianka", "jajecznica", "jaja", "jajko", "jajka", "omlet", "tost", "kanapk", "chleb", 
        "bułk", "twaróg", "twarożek", "serek", "musli", "granola", "płatki", "parówki", "szynka", "pasta jajeczna"
    )
    
    // Przekąska keywords
    val snackKeywords = listOf(
        "koktajl", "smoothie", "sok", "surówk", "sałatk", "owoc", "jabłko", "banan", "gruszka",
        "jogurt", "orzech", "migdał", "rodzynk", "ciasto", "ciasteczk", "baton", "czekolada", 
        "budyń", "kisiel", "galaretka", "mus ", "kiszon"
    )
    
    if (lunchKeywords.any { nameLower.contains(it) }) {
        return MealType.LUNCH
    }
    if (breakfastKeywords.any { nameLower.contains(it) }) {
        return MealType.BREAKFAST
    }
    if (snackKeywords.any { nameLower.contains(it) }) {
        return MealType.SNACK
    }
    
    // Fallback based on calories
    return when {
        kcal >= 450.0 -> MealType.LUNCH
        kcal >= 250.0 -> MealType.BREAKFAST
        kcal >= 120.0 -> MealType.SUPPER
        else -> MealType.SNACK
    }
}

data class Recipe(
    val id: Long,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val salt: Double
) {
    val category: MealType
        get() = classifyRecipe(name, kcal)
}

data class MealPlanEntry(
    val id: Long = 0L,
    val date: String,          // YYYY-MM-DD
    val mealIndex: Int,       // 0 to 4 (e.g. 0=Śniadanie, 1=Drugie śniadanie, 2=Obiad, 3=Podwieczorek, 4=Kolacja)
    val recipeId: Long,
    val recipeName: String = "",   // Enriched from join
    val kcal: Double = 0.0,         // Enriched from join
    val protein: Double = 0.0,      // Enriched from join
    val fat: Double = 0.0,          // Enriched from join
    val carbs: Double = 0.0,        // Enriched from join
    val fiber: Double = 0.0,        // Enriched from join
    val salt: Double = 0.0,         // Enriched from join
    val isEaten: Boolean = false
)

data class RecipeIngredient(
    val id: Long = 0L,
    val dishId: Long,
    val name: String,
    val weight: Double
)
