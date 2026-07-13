package io.github.szczepanskikrs.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import io.github.szczepanskikrs.data.MealPlanEntry
import io.github.szczepanskikrs.data.RecipeIngredient
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedMealPlanDate.collectAsState()
    val mealPlan by viewModel.mealPlan.collectAsState()
    val recipesCount by viewModel.recipesCount.collectAsState()
    val isGenerating by viewModel.isGeneratingMealPlan.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showShoppingList by remember { mutableStateOf(false) }

    // Parse week days for the selected date
    val weekDays = remember(selectedDate) { getWeekDays(selectedDate) }
    
    // Formatting helpers
    val dayNameFormat = SimpleDateFormat("EE", Locale.forLanguageTag("pl"))
    val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("pl"))

    val selectedDateObj = remember(selectedDate) {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
        } catch (e: Exception) {
            Date()
        }
    } ?: Date()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadłospis", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { showShoppingList = true }) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Lista zakupów")
                    }
                    IconButton(onClick = { showGenerateDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generuj nowy jadłospis")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Week Navigator (Previous/Next week buttons and horizontal day selector)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val prevWeekDate = navigateWeek(selectedDate, -1)
                        viewModel.selectMealPlanDate(prevWeekDate)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Poprzedni tydzień")
                    }
                    
                    Text(
                        text = fullDateFormat.format(selectedDateObj),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = {
                        val nextWeekDate = navigateWeek(selectedDate, 1)
                        viewModel.selectMealPlanDate(nextWeekDate)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Następny tydzień")
                    }
                }

                // Horizontal row of 7 week days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    weekDays.forEach { (date, dateStr) ->
                        val isSelected = dateStr == selectedDate
                        val dayName = dayNameFormat.format(date).replaceFirstChar { it.uppercase() }
                        val dayNum = dayNumFormat.format(date)

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clickable {
                                    viewModel.selectMealPlanDate(dateStr)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(dayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(dayNum, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Nutritional Summary Card
                DailySummarySection(mealPlan = mealPlan)

                // Meals List / Placeholder
                if (mealPlan.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Brak zaplanowanych posiłków na ten dzień.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showGenerateDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Wygeneruj jadłospis na cały tydzień")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(mealPlan, key = { _, item -> item.id }) { index, entry ->
                            MealEntryRow(
                                entry = entry,
                                totalMealsCount = mealPlan.size,
                                viewModel = viewModel,
                                onToggleEaten = { eaten ->
                                    viewModel.toggleMealPlanEntryEaten(entry.id, eaten)
                                },
                                onReplace = {
                                    viewModel.rollAlternativeMeal(entry.id)
                                    Toast.makeText(context, "Wylosowano inny posiłek!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            if (isGenerating) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Generowanie jadłospisu...",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Weekly generation config dialog
    if (showGenerateDialog) {
        GenerateWeeklyPlanDialog(
            recipesCount = recipesCount,
            selectedDate = selectedDate,
            onDismiss = { showGenerateDialog = false },
            onConfirm = { kcal, mealsCount, targetMonday ->
                showGenerateDialog = false
                viewModel.generateWeeklyMealPlan(targetMonday, kcal, mealsCount) { success ->
                    if (success) {
                        Toast.makeText(context, "Wygenerowano jadłospis!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Błąd podczas generowania jadłospisu.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Weekly shopping list dialog
    if (showShoppingList) {
        ShoppingListDialog(
            selectedDate = selectedDate,
            viewModel = viewModel,
            onDismiss = { showShoppingList = false }
        )
    }
}

@Composable
fun DailySummarySection(mealPlan: List<MealPlanEntry>) {
    val totalPlannedKcal = mealPlan.sumOf { it.kcal }
    val totalEatenKcal = mealPlan.filter { it.isEaten }.sumOf { it.kcal }

    val plannedProt = mealPlan.sumOf { it.protein }
    val eatenProt = mealPlan.filter { it.isEaten }.sumOf { it.protein }

    val plannedFat = mealPlan.sumOf { it.fat }
    val eatenFat = mealPlan.filter { it.isEaten }.sumOf { it.fat }

    val plannedCarbs = mealPlan.sumOf { it.carbs }
    val eatenCarbs = mealPlan.filter { it.isEaten }.sumOf { it.carbs }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Podsumowanie dnia",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${totalEatenKcal.toInt()} / ${totalPlannedKcal.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (mealPlan.isNotEmpty()) {
                val progress = if (totalPlannedKcal > 0) (totalEatenKcal / totalPlannedKcal).toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                // Compact single line of macros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MacroTextLabel(label = "B", eaten = eatenProt, planned = plannedProt, color = Color(0xFFE57373))
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    MacroTextLabel(label = "T", eaten = eatenFat, planned = plannedFat, color = Color(0xFFFFB74D))
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    MacroTextLabel(label = "W", eaten = eatenCarbs, planned = plannedCarbs, color = Color(0xFF64B5F6))
                }
            } else {
                Text(
                    text = "Brak posiłków zaplanowanych na dzisiaj.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MacroTextLabel(label: String, eaten: Double, planned: Double, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: ${eaten.toInt()}/${planned.toInt()}g",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MealEntryRow(
    entry: MealPlanEntry,
    totalMealsCount: Int,
    viewModel: HealthTrackerViewModel,
    onToggleEaten: (Boolean) -> Unit,
    onReplace: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val mealTitle = remember(entry.mealIndex, totalMealsCount) {
        getPolishMealName(entry.mealIndex, totalMealsCount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isEaten) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = if (entry.isEaten) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Badge index icon or generic meal icon
                        Icon(
                            imageVector = when(entry.mealIndex) {
                                0 -> Icons.Default.LocalCafe
                                1 -> Icons.Default.Spa
                                2 -> Icons.Default.Restaurant
                                3 -> Icons.Default.Fastfood
                                else -> Icons.Default.RestaurantMenu
                            },
                            contentDescription = null,
                            tint = if (entry.isEaten) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = mealTitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${entry.kcal.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black
                            )
                            if (entry.scale != 1.0) {
                                Text(
                                    text = "${(entry.scale * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.scale > 1.0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }

                // Checkbox to mark eaten
                Checkbox(
                    checked = entry.isEaten,
                    onCheckedChange = onToggleEaten,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }

            SelectionContainer {
                Text(
                    text = entry.recipeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isEaten) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                )
            }

            // Mini row of nutrients (displayed in collapsed mode too for glanceability)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NutrientShortBadge(label = "B", value = entry.protein, color = Color(0xFFE57373))
                NutrientShortBadge(label = "T", value = entry.fat, color = Color(0xFFFFB74D))
                NutrientShortBadge(label = "W", value = entry.carbs, color = Color(0xFF64B5F6))
            }

            // Expanded detail section
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Ingredients list
                    var ingredients by remember(entry.recipeId) { mutableStateOf<List<RecipeIngredient>?>(null) }
                    LaunchedEffect(entry.recipeId) {
                        ingredients = viewModel.getIngredientsForRecipe(entry.recipeId)
                    }

                    Text("Składniki:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (ingredients == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally),
                            strokeWidth = 2.dp
                        )
                    } else if (ingredients!!.isEmpty()) {
                        Text("Brak szczegółowej listy składników.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ingredients!!.forEach { ing ->
                                val scaledWeight = ing.weight * entry.scale
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${ing.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = if (scaledWeight >= 1000.0) String.format(Locale.getDefault(), "%.2f kg", scaledWeight / 1000.0) else "${scaledWeight.toInt()} g",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Text("Szczegóły wartości odżywczych (100g):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Błonnik: ${String.format(Locale.getDefault(), "%.1f", entry.fiber)} g", fontSize = 13.sp)
                            Text("Sól: ${String.format(Locale.getDefault(), "%.2f", entry.salt)} g", fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Białko: ${String.format(Locale.getDefault(), "%.1f", entry.protein)} g", fontSize = 13.sp)
                            Text("Tłuszcz: ${String.format(Locale.getDefault(), "%.1f", entry.fat)} g", fontSize = 13.sp)
                            Text("Węglowodany: ${String.format(Locale.getDefault(), "%.1f", entry.carbs)} g", fontSize = 13.sp)
                        }
                    }

                    // Action buttons (Roll alternative)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onReplace,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Wylosuj inny")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientShortBadge(label: String, value: Double, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .clickable(enabled = false) {}
                .padding(0.dp)
                .clip(CircleShape)
        ) {
            Surface(color = color, modifier = Modifier.fillMaxSize()) {}
        }
        Text(
            text = "$label: ${value.toInt()}g",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GenerateWeeklyPlanDialog(
    recipesCount: Int,
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Int, String) -> Unit
) {
    var kcalInput by remember { mutableStateOf("2000") }
    var mealsCount by remember { mutableStateOf(4) }
    
    val isKcalValid = remember(kcalInput) {
        val kcal = kcalInput.toDoubleOrNull()
        kcal != null && kcal in 1000.0..5000.0
    }

    val sdfDb = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val thisWeekMonday = remember { getThisWeekMonday() }
    val nextWeekMonday = remember { getNextWeekMonday() }
    val viewedWeekMonday = remember(selectedDate) { getViewedWeekMonday(selectedDate) }

    val options = remember(selectedDate) {
        val list = mutableListOf<Triple<String, String, String>>() // Key, Label, DbDateStr
        
        list.add(Triple("this", "Bieżący tydzień (${getWeekRangeLabel(thisWeekMonday)})", sdfDb.format(thisWeekMonday)))
        list.add(Triple("next", "Następny tydzień (${getWeekRangeLabel(nextWeekMonday)})", sdfDb.format(nextWeekMonday)))
        
        val thisStr = sdfDb.format(thisWeekMonday)
        val nextStr = sdfDb.format(nextWeekMonday)
        val viewedStr = sdfDb.format(viewedWeekMonday)
        if (viewedStr != thisStr && viewedStr != nextStr) {
            list.add(Triple("viewed", "Wybrany tydzień (${getWeekRangeLabel(viewedWeekMonday)})", viewedStr))
        }
        list
    }

    var selectedOptionIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Generuj Jadłospis tygodniowy", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (recipesCount == 0) {
                    Text(
                        "Baza przepisów jest pusta. Trwa importowanie lub sprawdź dane w recipes.csv.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "Dostępnych przepisów w bazie: $recipesCount. System dopasuje potrawy tak, aby suma kaloryczna każdego dnia była bliska celowi, dbając o zróżnicowanie posiłków.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text("Wybierz tydzień:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOptionIndex = index }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOptionIndex == index,
                                onClick = { selectedOptionIndex = index }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.second, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = kcalInput,
                    onValueChange = { kcalInput = it },
                    label = { Text("Dzienny cel kaloryczny (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isKcalValid,
                    supportingText = {
                        if (!isKcalValid) {
                            Text("Wprowadź wartość od 1000 do 5000 kcal")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Liczba posiłków dziennie:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(3, 4, 5).forEach { count ->
                        val isSelected = mealsCount == count
                        FilterChip(
                            selected = isSelected,
                            onClick = { mealsCount = count },
                            label = { Text("$count posiłki") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kcal = kcalInput.toDoubleOrNull() ?: 2000.0
                    val targetMonday = options[selectedOptionIndex].third
                    onConfirm(kcal, mealsCount, targetMonday)
                },
                enabled = isKcalValid && recipesCount > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Wygeneruj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

private fun getPolishMealName(index: Int, totalMeals: Int): String {
    return when (totalMeals) {
        3 -> when (index) {
            0 -> "Śniadanie"
            1 -> "Obiad"
            2 -> "Kolacja"
            else -> "Posiłek ${index + 1}"
        }
        4 -> when (index) {
            0 -> "Śniadanie"
            1 -> "Drugie śniadanie"
            2 -> "Obiad"
            3 -> "Kolacja"
            else -> "Posiłek ${index + 1}"
        }
        5 -> when (index) {
            0 -> "Śniadanie"
            1 -> "Drugie śniadanie"
            2 -> "Obiad"
            3 -> "Podwieczorek"
            4 -> "Kolacja"
            else -> "Posiłek ${index + 1}"
        }
        else -> "Posiłek ${index + 1}"
    }
}

private fun getThisWeekMonday(): Date {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

private fun getNextWeekMonday(): Date {
    val cal = Calendar.getInstance()
    cal.time = getThisWeekMonday()
    cal.add(Calendar.DAY_OF_YEAR, 7)
    return cal.time
}

private fun getViewedWeekMonday(selectedDateString: String): Date {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    try {
        val d = sdf.parse(selectedDateString)
        if (d != null) {
            cal.time = d
        }
    } catch (e: Exception) {}
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

private fun getWeekRangeLabel(mondayDate: Date): String {
    val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.time = mondayDate
    val startStr = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, 6)
    val endStr = sdf.format(cal.time)
    return "Pn, $startStr - Nd, $endStr"
}

private fun getWeekDays(referenceDateString: String): List<Pair<Date, String>> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    try {
        val d = sdf.parse(referenceDateString)
        if (d != null) {
            cal.time = d
        }
    } catch (e: Exception) {}

    // Adjust to Monday of that week
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val list = mutableListOf<Pair<Date, String>>()
    for (i in 0 until 7) {
        list.add(Pair(cal.time, sdf.format(cal.time)))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}

private fun navigateWeek(selectedDateString: String, weeksOffset: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    try {
        val d = sdf.parse(selectedDateString)
        if (d != null) {
            cal.time = d
        }
    } catch (e: Exception) {}
    cal.add(Calendar.WEEK_OF_YEAR, weeksOffset)
    return sdf.format(cal.time)
}

@Composable
fun ShoppingListDialog(
    selectedDate: String,
    viewModel: HealthTrackerViewModel,
    onDismiss: () -> Unit
) {
    val shoppingList by viewModel.weeklyShoppingList.collectAsState()
    val weekDays = remember(selectedDate) { getWeekDays(selectedDate) }
    val mondayStr = weekDays.first().second
    val sundayStr = weekDays.last().second

    val sdfSource = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfTarget = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val formattedRange = remember(mondayStr, sundayStr) {
        try {
            val mon = sdfSource.parse(mondayStr)
            val sun = sdfSource.parse(sundayStr)
            if (mon != null && sun != null) {
                "${sdfTarget.format(mon)} - ${sdfTarget.format(sun)}"
            } else {
                "$mondayStr - $sundayStr"
            }
        } catch (e: Exception) {
            "$mondayStr - $sundayStr"
        }
    }

    LaunchedEffect(mondayStr, sundayStr) {
        viewModel.loadWeeklyShoppingList(mondayStr, sundayStr)
    }

    val coroutineScope = rememberCoroutineScope()
    val checkedItems = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Lista Zakupów", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formattedRange,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (shoppingList.isEmpty()) {
                    Text(
                        text = "Brak składników do kupienia. Najpierw wygeneruj jadłospis na ten tydzień.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(shoppingList) { (name, weight) ->
                            val isChecked = checkedItems.contains(name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            checkedItems.remove(name)
                                        } else {
                                            checkedItems.add(name)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                checkedItems.add(name)
                                            } else {
                                                checkedItems.remove(name)
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (weight >= 1000.0) {
                                        String.format(Locale.getDefault(), "%.2f kg", weight / 1000.0)
                                    } else {
                                        "${weight.toInt()} g"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Zamknij")
            }
        },
        dismissButton = {
            if (shoppingList.isNotEmpty()) {
                val clipboard = LocalClipboard.current
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        val clipboardText = shoppingList.joinToString("\n") { (name, weight) ->
                            val weightStr = if (weight >= 1000.0) {
                                String.format(Locale.getDefault(), "%.2f kg", weight / 1000.0)
                            } else {
                                "${weight.toInt()} g"
                            }
                            "- $name: $weightStr"
                        }
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Shopping List", clipboardText)))
                        }
                        Toast.makeText(context, "Skopiowano listę do schowka!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kopiuj listę")
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
