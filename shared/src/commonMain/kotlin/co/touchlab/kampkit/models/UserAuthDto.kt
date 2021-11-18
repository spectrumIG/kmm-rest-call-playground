package co.touchlab.kampkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAuthDto(
    @SerialName("email")
    val email: String,
    @SerialName("id")
    val id: Int,
    @SerialName("username")
    val username: String
)

