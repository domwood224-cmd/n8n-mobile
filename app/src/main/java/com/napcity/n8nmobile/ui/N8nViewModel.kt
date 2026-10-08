package com.napcity.n8nmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napcity.n8nmobile.api.N8nClient
import com.napcity.n8nmobile.data.N8nExecution
import com.napcity.n8nmobile.data.N8nWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val workflows: List<N8nWorkflow> = emptyList(),
    val executions: List<N8nExecution> = emptyList(),
    val error: String? = null
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

    fun loadWorkflows() {
        if (baseUrl.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getWorkflows()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        workflows = response.body()?.data ?: emptyList()
                    )
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

    fun loadExecutions(workflowId: String? = null) {
        if (baseUrl.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.getExecutions(workflowId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        executions = response.body()?.data ?: emptyList()
                    )
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

    fun toggleWorkflow(workflow: N8nWorkflow) {
        if (baseUrl.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            try {
                val api = N8nClient.getApi(baseUrl, apiKey)
                val response = api.updateWorkflow(
                    workflow.id,
                    mapOf("active" to !workflow.active)
                )
                if (response.isSuccessful) {
                    loadWorkflows() // Refresh
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Toggle failed: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Toggle failed"
                )
            }
        }
    }
}
