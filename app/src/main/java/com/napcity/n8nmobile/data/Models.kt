package com.napcity.n8nmobile.data

data class N8nWorkflow(
    val id: String,
    val name: String,
    val active: Boolean,
    val createdAt: String = "",
    val updatedAt: String = "",
    val tags: List<N8nTag> = emptyList()
)

data class N8nTag(
    val id: String = "",
    val name: String = "",
    val color: Int = 0
)

data class N8nWorkflowDetail(
    val id: String,
    val name: String,
    val active: Boolean,
    val nodes: List<N8nNode> = emptyList(),
    val connections: Map<String, Any> = emptyMap(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val tags: List<N8nTag> = emptyList()
)

data class N8nNode(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val position: List<Int> = emptyList(),
    val parameters: Map<String, Any> = emptyMap()
)

data class N8nExecution(
    val id: String,
    val workflowId: String,
    val status: String,
    val startedAt: String = "",
    val stoppedAt: String = "",
    val mode: String = ""
)

data class N8nExecutionDetail(
    val id: String,
    val workflowId: String,
    val status: String,
    val startedAt: String = "",
    val stoppedAt: String = "",
    val mode: String = "",
    val data: Map<String, Any> = emptyMap()
)

data class WorkflowListResponse(
    val data: List<N8nWorkflow>
)

data class ExecutionListResponse(
    val data: List<N8nExecution>
)
