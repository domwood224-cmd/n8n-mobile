package com.napcity.n8nmobile.api

import com.napcity.n8nmobile.data.ExecutionListResponse
import com.napcity.n8nmobile.data.N8nWorkflow
import com.napcity.n8nmobile.data.WorkflowListResponse
import retrofit2.Response
import retrofit2.http.*

interface N8nApi {
    @GET("api/v1/workflows")
    suspend fun getWorkflows(): Response<WorkflowListResponse>

    @GET("api/v1/workflows/{id}")
    suspend fun getWorkflow(@Path("id") id: String): Response<N8nWorkflow>

    @PATCH("api/v1/workflows/{id}")
    suspend fun updateWorkflow(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<N8nWorkflow>

    @GET("api/v1/executions")
    suspend fun getExecutions(
        @Query("workflowId") workflowId: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ExecutionListResponse>

    @DELETE("api/v1/workflows/{id}")
    suspend fun deleteWorkflow(@Path("id") id: String): Response<Unit>
}
