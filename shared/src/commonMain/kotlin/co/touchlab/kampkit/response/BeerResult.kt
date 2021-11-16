package co.touchlab.kampkit.response

import kotlinx.serialization.Serializable

@Serializable
data class BeerResult(
    val name: String?,
    val tagline: String?
)
