package com.napcity.n8nmobile.api

import com.napcity.n8nmobile.data.ExecutionListResponse
import com.napcity.n8nmobile.data.N8nExecutionDetail
import com.napcity.n8nmobile.data.N8nWorkflow
import com.napcity.n8nmobile.data.N8nWorkflowDetail
import com.napcity.n8nmobile.data.WorkflowListResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface N8nApi {
    // Workflows
    @GET("api/v1/workflows")
    suspend fun getWorkflows(): Response<WorkflowListResponse>

    @GET("api/v1/workflows/{id}")
    suspend fun getWorkflow(@Path("id") id: String): Response<N8nWorkflowDetail>

    @POST("api/v1/workflows")
    suspend fun createWorkflow(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<N8nWorkflowDetail>

    @PATCH("api/v1/workflows/{id}")
    suspend fun updateWorkflow(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<N8nWorkflow>

    @DELETE("api/v1/workflows/{id}")
    suspend fun deleteWorkflow(@Path("id") id: String): Response<Unit>

    @POST("api/v1/workflows/{id}/trigger")
    suspend fun triggerWorkflow(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): Response<ResponseBody>

    // Executions
    @GET("api/v1/executions")
    suspend fun getExecutions(
        @Query("workflowId") workflowId: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null
    ): Response<ExecutionListResponse>

    @GET("api/v1/executions/{id}")
    suspend fun getExecution(@Path("id") id: String): Response<N8nExecutionDetail>

    @POST("api/v1/executions/{id}/retry")
    suspend fun retryExecution(@Path("id") id: String): Response<ResponseBody>

    @DELETE("api/v1/executions/{id}")
    suspend fun deleteExecution(@Path("id") id: String): Response<Unit>

    // Tags
    @GET("api/v1/tags")
    suspend fun getTags(): Response<Map<String, Any>>

    // Credentials (list only, no secrets exposed)
    @GET("api/v1/credentials")
    suspend fun getCredentials(): Response<Map<String, Any>>

    // Variables
    @GET("api/v1/variables")
    suspend fun getVariables(): Response<Map<String, Any>>
}
