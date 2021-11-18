package co.touchlab.kampkit.mock

import co.touchlab.kampkit.ktor.KtorApi
import co.touchlab.kampkit.ktor.NetResponse
import co.touchlab.kampkit.models.CredentialRequest
import co.touchlab.kampkit.models.UserAuthDto
import co.touchlab.kampkit.response.BeerResult

// TODO convert this to use Ktor's MockEngine
class KtorApiMock : KtorApi {
    private var nextGetResult: () -> List<BeerResult> = { throw error("Uninitialized!") }
    var calledCount = 0
        private set

    override suspend fun getJsonBeersFromApi(): List<BeerResult> {
        val result = nextGetResult()
        calledCount++
        return result
    }

    override suspend fun postAuth(credential: CredentialRequest): NetResponse {
        return successPostResult()
    }

    fun successGetResult(): List<BeerResult> {
        return listOf(
            BeerResult(name = "weissbier", tagline = "A weissbier"),
            BeerResult(name = "Punk Ipa", tagline = "A Punk Ipa")
        )
    }

    private fun successPostResult(): NetResponse {
        return NetResponse.Success(UserAuthDto("emial@email.com", 1, "username"), "Bearer authtoken")
    }

    fun prepareResult(beerResult: List<BeerResult>) {
        nextGetResult = { beerResult }
    }

    fun throwOnCall(throwable: Throwable) {
        nextGetResult = { throw throwable }
    }
}
