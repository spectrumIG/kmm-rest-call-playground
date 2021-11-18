package co.touchlab.kampkit.ktor

import co.touchlab.kampkit.models.CredentialRequest
import co.touchlab.kampkit.models.UserAuthDto
import co.touchlab.kampkit.response.BeerResult

interface KtorApi {
    suspend fun getJsonBeersFromApi(): List<BeerResult>

    suspend fun postAuth(credential: CredentialRequest): NetResponse
}
