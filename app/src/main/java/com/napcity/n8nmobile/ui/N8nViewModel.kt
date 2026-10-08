package com.napcity.n8nmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napcity.n8nmobile.api.N8nClient
import com.napcity.n8nmobile.data.N8nExecution
import com.napcity.n8nmobile.data.N8nExecutionDetail
import com.napcity.n8nmobile.data.N8nWorkflow
import com.napcity.n8nmobile.data.N8nWorkflowDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val workflows: List<N8nWorkflow> = emptyList(),
    val filteredWorkflows: List<N8nWorkflow> = emptyList(),
    val executions: List<N8nExecution> = emptyList(),
    val selectedExecution: N8nExecutionDetail? = null,
    val selectedWorkflowDetail: N8nWorkflowDetail? = null,
    val searchQuery: String = "",
    val statusFilter: String = "all", // all, active, inactive
    val error: String? = null,
    val successMessage: String? = null
)

class N8nViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var baseUrl: String = ""
    private var apiKey: String = ""

    fun configure(url: String, key: String) {
        baseUrl = url
        apiKey = key
        N8nClient.invalidate()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    // --- Workflows ---

    fun loadWorkflows() {
        if (baseUrl.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getWorkflows()
                if (response.isSuccessful) {
                    val workflows = response.body()?.data ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        workflows = workflows,
                    )
                    applyFilter()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error ${response.code()}: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Connection failed"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    fun setStatusFilter(filter: String) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        var filtered = state.workflows
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
        }
        filtered = when (state.statusFilter) {
            "active" -> filtered.filter { it.active }
            "inactive" -> filtered.filter { !it.active }
            else -> filtered
        }
        _uiState.value = state.copy(filteredWorkflows = filtered)
    }

    fun toggleWorkflow(workflow: N8nWorkflow) {
        viewModelScope.launch {
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.updateWorkflow(workflow.id, mapOf("active" to !workflow.active))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "${workflow.name} ${if (!workflow.active) "activated" else "deactivated"}"
                    )
                    loadWorkflows()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Toggle failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Toggle failed")
            }
        }
    }

    fun triggerWorkflow(workflow: N8nWorkflow) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.triggerWorkflow(workflow.id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (response.isSuccessful) "${workflow.name} triggered" else "Trigger failed: ${response.code()}"
                )
                if (response.isSuccessful) loadExecutions(workflow.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Trigger failed")
            }
        }
    }

    fun loadWorkflowDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getWorkflow(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedWorkflowDetail = if (response.isSuccessful) response.body() else null,
                    error = if (!response.isSuccessful) "Error ${response.code()}" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun duplicateWorkflow(workflow: N8nWorkflow) {
        viewModelScope.launch {
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val detailResp = api.getWorkflow(workflow.id)
                if (!detailResp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(error = "Could not load workflow for duplication")
                    return@launch
                }
                val detail = detailResp.body()!!
                val createResp = api.createWorkflow(mapOf(
                    "name" to "${workflow.name} (copy)",
                    "nodes" to detail.nodes,
                    "connections" to detail.connections,
                    "active" to false
                ))
                if (createResp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Duplicated as \"${workflow.name} (copy)\"")
                    loadWorkflows()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Duplicate failed: ${createResp.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Duplicate failed")
            }
        }
    }

    fun deleteWorkflow(workflow: N8nWorkflow) {
        viewModelScope.launch {
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.deleteWorkflow(workflow.id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "${workflow.name} deleted")
                    loadWorkflows()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Delete failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Delete failed")
            }
        }
    }

    fun exportWorkflowJson(id: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getWorkflow(id)
                if (response.isSuccessful) {
                    val json = com.google.gson.Gson().toJson(response.body())
                    onResult(json)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Export failed: ${response.code()}")
                    onResult(null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                onResult(null)
            }
        }
    }

    // --- Executions ---

    fun loadExecutions(workflowId: String? = null) {
        if (baseUrl.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getExecutions(workflowId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    executions = if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList(),
                    error = if (!response.isSuccessful) "Error ${response.code()}" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadExecutionDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getExecution(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedExecution = if (response.isSuccessful) response.body() else null,
                    error = if (!response.isSuccessful) "Error ${response.code()}" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun retryExecution(execution: N8nExecution) {
        viewModelScope.launch {
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.retryExecution(execution.id)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (response.isSuccessful) "Execution retried" else "Retry failed: ${response.code()}"
                )
                if (response.isSuccessful) loadExecutions(execution.workflowId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Retry failed")
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedExecution = null,
            selectedWorkflowDetail = null
        )
    }
}
