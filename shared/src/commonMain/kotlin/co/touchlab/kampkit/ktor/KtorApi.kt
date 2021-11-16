package co.touchlab.kampkit.ktor

import co.touchlab.kampkit.response.BeerResult

interface KtorApi {
    suspend fun getJsonFromApi(): List<BeerResult>
}
