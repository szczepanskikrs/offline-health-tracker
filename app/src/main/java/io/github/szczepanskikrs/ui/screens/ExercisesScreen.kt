package io.github.szczepanskikrs.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.szczepanskikrs.data.ExerciseLog
import io.github.szczepanskikrs.data.ExerciseType
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val exerciseTypes by viewModel.exerciseTypes.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()

    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var showAddTypeDialog by remember { mutableStateOf(false) }

    // Log Form State
    var repsStr by remember { mutableStateOf("") }
    var setsStr by remember { mutableStateOf("1") }
    var weightStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    // Auto-select first exercise when list loads
    LaunchedEffect(exerciseTypes) {
        if (selectedExercise == null && exerciseTypes.isNotEmpty()) {
            selectedExercise = exerciseTypes.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Śledzenie Ćwiczeń", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise selection header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wybierz ćwiczenie:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddTypeDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dodaj typ", fontSize = 14.sp)
                    }
                }
            }

            // Exercise Selection chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exerciseTypes.forEach { type ->
                        val isSelected = selectedExercise?.id == type.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedExercise = type },
                            label = { Text(type.name) },
                            trailingIcon = if (type.isCustom) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Usuń typ",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                viewModel.deleteExerciseType(type.id)
                                                if (selectedExercise?.id == type.id) {
                                                    selectedExercise = exerciseTypes.firstOrNull { it.id != type.id }
                                                }
                                            },
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Log session form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Zapisz serię dla: ${selectedExercise?.name ?: "..."}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = setsStr,
                                onValueChange = {
                                    setsStr = it
                                    formError = ""
                                },
                                label = { Text("Serie") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = repsStr,
                                onValueChange = {
                                    repsStr = it
                                    formError = ""
                                },
                                label = { Text("Powtórzenia") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightStr,
                                onValueChange = {
                                    weightStr = it
                                    formError = ""
                                },
                                label = { Text("Ciężar (kg) - opcjonalnie") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notatki") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        if (formError.isNotEmpty()) {
                            Text(
                                text = formError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val currentEx = selectedExercise
                                if (currentEx == null) {
                                    formError = "Wybierz ćwiczenie z listy powyżej."
                                    return@Button
                                }
                                val reps = repsStr.toIntOrNull()
                                val sets = setsStr.toIntOrNull()
                                if (reps == null || reps <= 0 || sets == null || sets <= 0) {
                                    formError = "Liczba serii i powtórzeń musi być dodatnią liczbą całkowitą."
                                    return@Button
                                }
                                val weight = weightStr.toDoubleOrNull()

                                viewModel.addExerciseLog(
                                    exerciseId = currentEx.id,
                                    reps = reps,
                                    sets = sets,
                                    weight = weight,
                                    notes = notes
                                )

                                // Reset form values (except sets which defaults to 1)
                                repsStr = ""
                                weightStr = ""
                                notes = ""
                                formError = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zapisz trening")
                        }
                    }
                }
            }

            // History header
            item {
                Text(
                    text = "Ostatnia historia treningów",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // History List
            if (exerciseLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak zarejestrowanych treningów. Czas zacząć ćwiczyć!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(exerciseLogs) { log ->
                    ExerciseLogCard(
                        log = log,
                        dateFormat = dateFormat,
                        onDelete = { viewModel.deleteExerciseLog(log.id) }
                    )
                }
            }
        }
    }

    // Add Custom Exercise Type Dialog
    if (showAddTypeDialog) {
        var newTypeName by remember { mutableStateOf("") }
        var typeErrorMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("Dodaj Nowe Ćwiczenie", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Wpisz nazwę ćwiczenia, które chcesz dodać (np. Podciąganie, Przysiad z wyskokiem).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = {
                            newTypeName = it
                            typeErrorMsg = ""
                        },
                        label = { Text("Nazwa ćwiczenia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (typeErrorMsg.isNotEmpty()) {
                        Text(text = typeErrorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameClean = newTypeName.trim()
                        if (nameClean.isEmpty()) {
                            typeErrorMsg = "Nazwa nie może być pusta."
                            return@Button
                        }
                        if (exerciseTypes.any { it.name.equals(nameClean, ignoreCase = true) }) {
                            typeErrorMsg = "Ćwiczenie o tej nazwie już istnieje."
                            return@Button
                        }
                        viewModel.addExerciseType(nameClean)
                        showAddTypeDialog = false
                    }
                ) {
                    Text("Dodaj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTypeDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun ExerciseLogCard(
    log: ExerciseLog,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsGymnastics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val repsLabel = if (log.exerciseName.lowercase().contains("pompki") || log.exerciseName.lowercase().contains("przysiady") || log.exerciseName.lowercase().contains("mostki")) "powtórzeń" else "powt./sek."
                val weightText = if (log.weight != null) " + ${log.weight} kg" else ""
                Text(
                    text = "${log.sets} serii x ${log.reps} $repsLabel$weightText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (log.notes.isNotEmpty()) {
                    Text(
                        text = "Komentarz: ${log.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń trening",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
