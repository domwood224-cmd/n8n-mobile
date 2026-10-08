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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.napcity.n8nmobile.api.N8nClient
import com.napcity.n8nmobile.data.N8nExecution
import com.napcity.n8nmobile.data.N8nWorkflow
import com.napcity.n8nmobile.data.SettingsStore
import com.napcity.n8nmobile.ui.N8nViewModel
import com.napcity.n8nmobile.ui.theme.ThemeN8NMobile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeN8NMobile {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    var showSettings by remember { mutableStateOf(!settings.isConfigured) }
    var selectedWorkflow by remember { mutableStateOf<N8nWorkflow?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(settings.isConfigured) {
        if (settings.isConfigured) {
            viewModel.configure(settings.baseUrl, settings.apiKey)
            viewModel.loadWorkflows()
        }
    }

    Scaffold(
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
                            if (selectedTab == 0) viewModel.loadWorkflows()
                            else viewModel.loadExecutions()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    onSave = { url, key ->
                        N8nClient.invalidate()
                        viewModel.configure(url, key)
                        viewModel.loadWorkflows()
                        showSettings = false
                        selectedTab = 0
                    },
                    onCancel = {
                        if (settings.isConfigured) showSettings = false
                    }
                )
            } else if (selectedWorkflow != null) {
                WorkflowDetailScreen(
                    workflow = selectedWorkflow!!,
                    viewModel = viewModel,
                    onBack = { selectedWorkflow = null }
                )
            } else {
                Column {
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
                    when (selectedTab) {
                        0 -> WorkflowListScreen(
                            uiState = uiState,
                            onWorkflowClick = {
                                selectedWorkflow = it
                                viewModel.loadExecutions(it.id)
                            },
                            onToggle = { viewModel.toggleWorkflow(it) }
                        )
                        1 -> ExecutionsScreen(uiState = uiState)
                    }
                }
            }
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error - in production use SnackbarHost
        }
    }
}

@Composable
fun WorkflowListScreen(
    uiState: com.napcity.n8nmobile.ui.UiState,
    onWorkflowClick: (N8nWorkflow) -> Unit,
    onToggle: (N8nWorkflow) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.workflows.isEmpty() -> {
                Text(
                    "No workflows found",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.workflows) { workflow ->
                        WorkflowCard(
                            workflow = workflow,
                            onClick = { onWorkflowClick(workflow) },
                            onToggle = { onToggle(workflow) }
                        )
                    }
                }
            }
        }
        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowCard(
    workflow: N8nWorkflow,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workflow.name, fontWeight = FontWeight.SemiBold)
                Text(
                    if (workflow.active) "Active" else "Inactive",
                    color = if (workflow.active) Color(0xFF4CAF50) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = workflow.active,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun ExecutionsScreen(uiState: com.napcity.n8nmobile.ui.UiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.executions.isEmpty() -> {
                Text(
                    "No executions found",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.executions) { execution ->
                        ExecutionCard(execution = execution)
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionCard(execution: N8nExecution) {
    val statusColor = when (execution.status.lowercase()) {
        "success" -> Color(0xFF4CAF50)
        "error" -> Color(0xFFF44336)
        "running" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Execution ${execution.id.take(8)}", fontWeight = FontWeight.SemiBold)
                Text(
                    execution.status.uppercase(),
                    color = statusColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                if (execution.startedAt.isNotBlank()) {
                    Text(
                        execution.startedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WorkflowDetailScreen(
    workflow: N8nWorkflow,
    viewModel: N8nViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text(workflow.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (workflow.active) "● Active" else "○ Inactive",
            color = if (workflow.active) Color(0xFF4CAF50) else Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recent Executions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.executions.isEmpty()) {
            Text("No executions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.executions) { execution ->
                    ExecutionCard(execution = execution)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var url by remember { mutableStateOf(settings.baseUrl) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("n8n Connection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Enter your n8n instance URL and API key. Find your API key in n8n under Settings → API.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Instance URL") },
            placeholder = { Text("https://your-n8n.com") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) PasswordVisualTransformation() else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Toggle visibility"
                    )
                }
            },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (settings.isConfigured) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
            Button(
                onClick = {
                    settings.baseUrl = url.trim()
                    settings.apiKey = apiKey.trim()
                    onSave(url.trim(), apiKey.trim())
                },
                enabled = url.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect")
            }
        }
        Text(
            "Credentials are stored encrypted on-device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
