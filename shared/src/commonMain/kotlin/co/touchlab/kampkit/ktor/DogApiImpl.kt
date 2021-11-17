package co.touchlab.kampkit.ktor

import co.touchlab.kampkit.response.BeerResult
import co.touchlab.kermit.Logger
import co.touchlab.stately.ensureNeverFrozen
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import io.ktor.client.features.logging.Logger as KtorLogger

class DogApiImpl(log: Logger) : KtorApi {

    // If this is a constructor property, then it gets captured
    // inside HttpClient config and freezes this whole class.
    @Suppress("CanBePrimaryConstructorProperty")
    private val log = log

    private val client = HttpClient {
        install(JsonFeature) {

            serializer = KotlinxSerializer(
                Json {
                    isLenient = false
                    ignoreUnknownKeys = true
                    allowSpecialFloatingPointValues = true
                    useArrayPolymorphism = false
                }
            )
        }
        install(Logging) {
            logger = object : KtorLogger {
                override fun log(message: String) {
                    log.v { message }
                }
            }

            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            val timeout = 30000L
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    init {
        ensureNeverFrozen()
    }

    override suspend fun getJsonFromApi(): List<BeerResult> {
        log.d { "Fetching Beer from network" }
        return client.get<List<BeerResult>> {
            dogs("v2/beers?per_page=80")
        }
    }

    private fun HttpRequestBuilder.dogs(path: String) {
        url {
            takeFrom("https://api.punkapi.com/")
            encodedPath = path
        }
    }
}
