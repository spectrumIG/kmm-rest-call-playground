package co.touchlab.kampkit.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("password")
    val password: String,
    @SerialName("username")
    val username: String
)