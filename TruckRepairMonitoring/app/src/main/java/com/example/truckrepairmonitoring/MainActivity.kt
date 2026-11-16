package com.example.truckrepairmonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = RepairDatabase.get(applicationContext).repairDao()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val vm: RepairViewModel = viewModel(factory = RepairViewModel.Factory(dao))
                TruckRepairApp(vm)
            }
        }
    }
}

class RepairViewModel(private val dao: RepairDao) : ViewModel() {
    val records = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(record: RepairRecord, isNew: Boolean) = viewModelScope.launch {
        if (isNew) dao.insert(record) else dao.update(record)
    }

    fun remove(record: RepairRecord) = viewModelScope.launch { dao.delete(record) }

    class Factory(private val dao: RepairDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RepairViewModel(dao) as T
    }
}

private enum class Screen { TRUCKS, REPORTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckRepairApp(vm: RepairViewModel) {
    val records by vm.records.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.TRUCKS) }
    var selectedPlate by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RepairRecord?>(null) }
    var deleting by remember { mutableStateOf<RepairRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Truck Repair Monitor", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (screen == Screen.TRUCKS) {
                FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add repair")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.TRUCKS,
                    onClick = { screen = Screen.TRUCKS; selectedPlate = null },
                    icon = { Text("🚚") },
                    label = { Text("Trucks") }
                )
                NavigationBarItem(
                    selected = screen == Screen.REPORTS,
                    onClick = { screen = Screen.REPORTS; selectedPlate = null },
                    icon = { Text("📊") },
                    label = { Text("Monthly reports") }
                )
            }
        }
    ) { padding ->
        when (screen) {
            Screen.TRUCKS -> TrucksScreen(
                records = records,
                selectedPlate = selectedPlate,
                onSelectPlate = { selectedPlate = it },
                onEdit = { editing = it; showEditor = true },
                onDelete = { deleting = it },
                modifier = Modifier.padding(padding)
            )
            Screen.REPORTS -> ReportsScreen(records, Modifier.padding(padding))
        }
    }

    if (showEditor) {
        RepairEditor(
            initial = editing,
            defaultPlate = selectedPlate.orEmpty(),
            onDismiss = { showEditor = false },
            onSave = { record ->
                vm.save(record, editing == null)
                showEditor = false
            }
        )
    }

    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete repair record?") },
            text = { Text("Delete ${record.repairPart} for ${record.plateNumber}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.remove(record); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TrucksScreen(
    records: List<RepairRecord>,
    selectedPlate: String?,
    onSelectPlate: (String?) -> Unit,
    onEdit: (RepairRecord) -> Unit,
    onDelete: (RepairRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val plates = records.map { it.plateNumber }.distinct().sorted()
    val selectedRecords = records.filter { it.plateNumber == selectedPlate }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Truck list", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (plates.isEmpty()) {
            EmptyMessage("No repair records yet. Tap + to add the first truck repair.")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plates.forEach { plate ->
                    FilterChip(
                        selected = selectedPlate == plate,
                        onClick = { onSelectPlate(if (selectedPlate == plate) null else plate) },
                        label = { Text(plate) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (selectedPlate == null) {
            Text("Select a plate number to view its repair history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            plates.forEach { plate ->
                val truckRecords = records.filter { it.plateNumber == plate }
                Card(onClick = { onSelectPlate(plate) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plate, fontWeight = FontWeight.Bold)
                            Text("${truckRecords.size} repair record(s)", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(peso(truckRecords.sumOf { it.amount }), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val total = selectedRecords.sumOf { it.amount }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(selectedPlate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Total repair expenses", style = MaterialTheme.typography.bodySmall)
                    Text(peso(total), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${selectedRecords.size} repair record(s)")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Repair history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (selectedRecords.isEmpty()) EmptyMessage("No repairs for this truck.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedRecords, key = { it.id }) { record ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(record.repairPart, fontWeight = FontWeight.Bold)
                                    Text(record.repairDate, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(peso(record.amount), fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { onEdit(record) }) { Icon(Icons.Default.Edit, "Edit") }
                                IconButton(onClick = { onDelete(record) }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(records: List<RepairRecord>, modifier: Modifier = Modifier) {
    var monthText by remember { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthText) {
        runCatching { YearMonth.parse(monthText) }.getOrNull()
    }
    val monthly = if (month == null) emptyList() else records.filter {
        runCatching { YearMonth.from(LocalDate.parse(it.repairDate)) == month }.getOrDefault(false)
    }
    val total = monthly.sumOf { it.amount }
    val byTruck = monthly.groupBy { it.plateNumber }.toSortedMap()

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Monthly reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = monthText,
            onValueChange = { monthText = it },
            label = { Text("Report month (YYYY-MM)") },
            supportingText = { Text("Example: 2026-11") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (month == null) {
            Text("Enter a valid month in YYYY-MM format.", color = MaterialTheme.colorScheme.error)
        } else {
            Spacer(Modifier.height(12.dp))
            SummaryCard("Overall fleet report – $month", total, monthly.size)
            Spacer(Modifier.height(16.dp))
            Text("Individual truck reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (byTruck.isEmpty()) {
                EmptyMessage("No repair expenses recorded for this month.")
            } else {
                byTruck.forEach { (plate, items) ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(plate, fontWeight = FontWeight.Bold)
                            Text("${items.size} repair record(s)", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            items.forEach { item ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${item.repairDate} • ${item.repairPart}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(peso(item.amount), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Truck total", fontWeight = FontWeight.Bold)
                                Text(peso(items.sumOf { it.amount }), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, count: Int) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(peso(amount), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("$count repair record(s)")
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepairEditor(
    initial: RepairRecord?,
    defaultPlate: String,
    onDismiss: () -> Unit,
    onSave: (RepairRecord) -> Unit
) {
    var plate by remember { mutableStateOf(initial?.plateNumber ?: defaultPlate) }
    var part by remember { mutableStateOf(initial?.repairPart ?: "") }
    var date by remember { mutableStateOf(initial?.repairDate ?: LocalDate.now().toString()) }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    val parsedAmount = amount.toDoubleOrNull()
    val valid = plate.isNotBlank() && part.isNotBlank() &&
        runCatching { LocalDate.parse(date); true }.getOrDefault(false) &&
        parsedAmount != null && parsedAmount >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add repair record" else "Edit repair record") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(plate, { plate = it.uppercase() }, label = { Text("Plate number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(part, { part = it }, label = { Text("Repair part") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₱)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(RepairRecord(
                        id = initial?.id ?: 0,
                        plateNumber = plate.trim(),
                        repairPart = part.trim(),
                        repairDate = date.trim(),
                        amount = parsedAmount!!
                    ))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun peso(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    return format.format(value)
}
