package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComplaintCreateRequest(
    @SerialName("title")
    val title: String,

    @SerialName("body")
    val body: String
)

@Serializable
data class ComplaintResponse(
    @SerialName("id")
    val id: Long,

    @SerialName("patient_id")
    val patientId: Long,

    @SerialName("title")
    val title: String,

    @SerialName("body")
    val body: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ComplaintPatchRequest(
    @SerialName("title")
    val title: String? = null,

    @SerialName("body")
    val body: String? = null
)