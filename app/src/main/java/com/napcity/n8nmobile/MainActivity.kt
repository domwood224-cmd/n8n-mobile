package com.napcity.n8nmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.napcity.n8nmobile.api.N8nClient
import com.napcity.n8nmobile.data.N8nExecution
import com.napcity.n8nmobile.data.N8nWorkflow
import com.napcity.n8nmobile.data.SettingsStore
import com.napcity.n8nmobile.ui.N8nViewModel
import com.napcity.n8nmobile.ui.theme.ThemeN8NMobile
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeN8NMobile {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    N8nApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun N8nApp() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val viewModel: N8nViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSettings by remember { mutableStateOf(!settings.isConfigured) }
    var selectedWorkflow by remember { mutableStateOf<N8nWorkflow?>(null) }
    var selectedExecution by remember { mutableStateOf<N8nExecution?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf<N8nWorkflow?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(settings.isConfigured) {
        if (settings.isConfigured) {
            viewModel.configure(settings.baseUrl, settings.apiKey)
            viewModel.loadWorkflows()
        }
    }

    // Snackbar for messages
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("n8n Mobile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    IconButton(onClick = {
                        if (settings.isConfigured) {
                            viewModel.configure(settings.baseUrl, settings.apiKey)
                            if (selectedTab == 0) viewModel.loadWorkflows() else viewModel.loadExecutions()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                showSettings -> SettingsScreen(
                    settings = settings,
                    onSave = { url, key ->
                        N8nClient.invalidate()
                        viewModel.configure(url, key)
                        viewModel.loadWorkflows()
                        showSettings = false
                        selectedTab = 0
                    },
                    onCancel = { if (settings.isConfigured) showSettings = false }
                )
                selectedExecution != null -> ExecutionDetailScreen(
                    execution = selectedExecution!!,
                    viewModel = viewModel,
                    onBack = { selectedExecution = null; viewModel.clearSelection() },
                    onRetry = {
                        viewModel.retryExecution(selectedExecution!!)
                        selectedExecution = null
                    }
                )
                selectedWorkflow != null -> WorkflowDetailScreen(
                    workflow = selectedWorkflow!!,
                    viewModel = viewModel,
                    onBack = { selectedWorkflow = null; viewModel.clearSelection() },
                    onTrigger = { viewModel.triggerWorkflow(selectedWorkflow!!) },
                    onDuplicate = { viewModel.duplicateWorkflow(selectedWorkflow!!) },
                    onDelete = { showDeleteConfirm = selectedWorkflow },
                    onExport = {
                        viewModel.exportWorkflowJson(selectedWorkflow!!.id) { json ->
                            // TODO: share via intent
                        }
                    },
                    onExecutionClick = { selectedExecution = it }
                )
                else -> Column {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("Workflows", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = {
                            selectedTab = 1
                            viewModel.loadExecutions()
                        }) {
                            Text("Executions", modifier = Modifier.padding(12.dp))
                        }
                    }
                    if (selectedTab == 0) {
                        WorkflowListScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onWorkflowClick = {
                                selectedWorkflow = it
                                viewModel.loadWorkflowDetail(it.id)
                                viewModel.loadExecutions(it.id)
                            },
                            onToggle = { viewModel.toggleWorkflow(it) },
                            onTrigger = { viewModel.triggerWorkflow(it) }
                        )
                    } else {
                        ExecutionsScreen(
                            uiState = uiState,
                            onExecutionClick = {
                                selectedExecution = it
                                viewModel.loadExecutionDetail(it.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { workflow ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete workflow?") },
            text = { Text("\"${workflow.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWorkflow(workflow)
                    showDeleteConfirm = null
                    selectedWorkflow = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowListScreen(
    uiState: com.napcity.n8nmobile.ui.UiState,
    viewModel: N8nViewModel,
    onWorkflowClick: (N8nWorkflow) -> Unit,
    onToggle: (N8nWorkflow) -> Unit,
    onTrigger: (N8nWorkflow) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search workflows") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true
        )
        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.statusFilter == "all",
                onClick = { viewModel.setStatusFilter("all") },
                label = { Text("All (${uiState.workflows.size})") }
            )
            FilterChip(
                selected = uiState.statusFilter == "active",
                onClick = { viewModel.setStatusFilter("active") },
                label = { Text("Active") }
            )
            FilterChip(
                selected = uiState.statusFilter == "inactive",
                onClick = { viewModel.setStatusFilter("inactive") },
                label = { Text("Inactive") }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // List
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.filteredWorkflows.isEmpty() -> Text(
                    if (uiState.workflows.isEmpty()) "No workflows found" else "No matches",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredWorkflows, key = { it.id }) { workflow ->
                        WorkflowCard(
                            workflow = workflow,
                            onClick = { onWorkflowClick(workflow) },
                            onToggle = { onToggle(workflow) },
                            onTrigger = { onTrigger(workflow) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowCard(
    workflow: N8nWorkflow,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onTrigger: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workflow.name, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (workflow.active) "● Active" else "○ Inactive",
                        color = if (workflow.active) Color(0xFF4CAF50) else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (workflow.tags.isNotEmpty()) {
                        Text(
                            "  • ${workflow.tags.joinToString(", ") { it.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onTrigger) {
                Icon(Icons.Default.PlayArrow, "Trigger", tint = MaterialTheme.colorScheme.primary)
            }
            Switch(checked = workflow.active, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
fun ExecutionsScreen(
    uiState: com.napcity.n8nmobile.ui.UiState,
    onExecutionClick: (N8nExecution) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            uiState.executions.isEmpty() -> Text(
                "No executions found",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.executions, key = { it.id }) { execution ->
                    ExecutionCard(execution = execution, onClick = { onExecutionClick(execution) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionCard(execution: N8nExecution, onClick: () -> Unit = {}) {
    val statusColor = when (execution.status.lowercase()) {
        "success" -> Color(0xFF4CAF50)
        "error" -> Color(0xFFF44336)
        "running" -> Color(0xFF2196F3)
        "waiting" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Execution ${execution.id.take(8)}", fontWeight = FontWeight.SemiBold)
                Text(execution.status.uppercase(), color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (execution.startedAt.isNotBlank()) {
                    Text(execution.startedAt.take(19).replace("T", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (execution.status.equals("error", ignoreCase = true)) {
                Icon(Icons.Default.Error, "Failed", tint = Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun WorkflowDetailScreen(
    workflow: N8nWorkflow,
    viewModel: N8nViewModel,
    onBack: () -> Unit,
    onTrigger: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onExecutionClick: (N8nExecution) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(workflow.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Trigger now") }, onClick = { showMenu = false; onTrigger() }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    DropdownMenuItem(text = { Text("Export JSON") }, onClick = { showMenu = false; onExport() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
        Text(if (workflow.active) "● Active" else "○ Inactive", color = if (workflow.active) Color(0xFF4CAF50) else Color.Gray)
        uiState.selectedWorkflowDetail?.let { detail ->
            Text("${detail.nodes.size} nodes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onTrigger, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Trigger Workflow")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recent Executions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.executions.isEmpty()) {
            Text("No executions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(uiState.executions, key = { it.id }) { execution ->
                    ExecutionCard(execution = execution, onClick = { onExecutionClick(execution) })
                }
            }
        }
    }
}

@Composable
fun ExecutionDetailScreen(
    execution: N8nExecution,
    viewModel: N8nViewModel,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.selectedExecution
    val statusColor = when (execution.status.lowercase()) {
        "success" -> Color(0xFF4CAF50)
        "error" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Execution ${execution.id.take(8)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Status: ${execution.status.uppercase()}", color = statusColor, fontWeight = FontWeight.Bold)
                Text("Mode: ${execution.mode}")
                if (execution.startedAt.isNotBlank()) Text("Started: ${execution.startedAt.take(19).replace("T", " ")}")
                if (execution.stoppedAt.isNotBlank()) Text("Stopped: ${execution.stoppedAt.take(19).replace("T", " ")}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (execution.status.equals("error", ignoreCase = true)) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Execution")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (detail != null && detail.data.isNotEmpty()) {
            Text("Execution Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    item {
                        Text(
                            com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(detail.data).take(5000),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: SettingsStore, onSave: (String, String) -> Unit, onCancel: () -> Unit) {
    var url by remember { mutableStateOf(settings.baseUrl) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var showKey by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("n8n Connection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Enter your n8n instance URL and API key. Find your API key in n8n under Settings → API.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Instance URL") },
            placeholder = { Text("https://your-n8n.com") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true
        )
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle")
                }
            },
            singleLine = true
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (settings.isConfigured) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            }
            Button(
                onClick = {
                    settings.baseUrl = url.trim()
                    settings.apiKey = apiKey.trim()
                    onSave(url.trim(), apiKey.trim())
                },
                enabled = url.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Connect") }
        }
        Text("Credentials are stored encrypted on-device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
