package co.touchlab.kampkit.ktor

import co.touchlab.kampkit.models.CredentialRequest
import co.touchlab.kampkit.models.UserAuthDto
import co.touchlab.kampkit.models.UserDto
import co.touchlab.kampkit.response.BeerResult
import co.touchlab.kermit.Logger
import co.touchlab.stately.ensureNeverFrozen
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.json.Json
import io.ktor.client.features.logging.Logger as KtorLogger

class KtorApiImpl(log: Logger) : KtorApi {

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

    override suspend fun getJsonBeersFromApi(): List<BeerResult> {

        log.d { "Fetching Beer from network" }

        return client.get {
            beers("v2/beers?per_page=80")
        }
    }

    override suspend fun postAuth(credential: CredentialRequest): NetResponse {
        log.d { "Authenthicate from Artoo" }
        val postResponse: HttpResponse
        try {

            postResponse = client.post("https://artoo-develop.k8s-facile.it/api/v1/security/session") {
                contentType(ContentType.Application.Json)
                body = UserDto(username = credential.username, password = credential.password)
            }
        } catch (t: Throwable) {
            return NetResponse.Error(t.message.orEmpty())
        }

        val dto: UserAuthDto = postResponse.receive()

        return when (postResponse.status.value) {
            in 200..209 -> NetResponse.Success(
                dto, postResponse.headers["Authorization"].toString()
            )

            else -> NetResponse.Error(postResponse.toString())
        }
    }

    private fun HttpRequestBuilder.beers(path: String) {
        url {
            takeFrom("https://api.punkapi.com/")
            encodedPath = path
        }
    }
}

sealed class NetResponse {
    data class Success(val dto: UserAuthDto, val authToken: String) : NetResponse()
    data class Error(val message: String) : NetResponse()
}
