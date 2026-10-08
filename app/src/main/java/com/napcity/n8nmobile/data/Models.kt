package com.napcity.n8nmobile.data

data class N8nWorkflow(
    val id: String,
    val name: String,
    val active: Boolean,
    val createdAt: String = "",
    val updatedAt: String = "",
    val tags: List<String> = emptyList()
)

data class N8nExecution(
    val id: String,
    val workflowId: String,
    val status: String, // success, error, running, waiting
    val startedAt: String = "",
    val stoppedAt: String = "",
    val mode: String = ""
)

data class WorkflowListResponse(
    val data: List<N8nWorkflow>
)

data class ExecutionListResponse(
    val data: List<N8nExecution>
)
