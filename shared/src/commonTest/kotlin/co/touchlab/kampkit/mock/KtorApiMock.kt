package co.touchlab.kampkit.mock

import co.touchlab.kampkit.ktor.KtorApi
import co.touchlab.kampkit.response.BeerResult

// TODO convert this to use Ktor's MockEngine
class KtorApiMock : KtorApi {
    private var nextResult: () -> List<BeerResult> = { throw error("Uninitialized!") }
    var calledCount = 0
        private set

    override suspend fun getJsonFromApi(): List<BeerResult> {
        val result = nextResult()
        calledCount++
        return result
    }

    fun successResult(): List<BeerResult> {
        return listOf(
            BeerResult(name = "weissbier", tagline = "A weissbier"),
            BeerResult(name = "Punk Ipa", tagline = "A Punk Ipa")
        )
    }

    fun prepareResult(beerResult: List<BeerResult>) {
        nextResult = { beerResult }
    }

    fun throwOnCall(throwable: Throwable) {
        nextResult = { throw throwable }
    }
}
