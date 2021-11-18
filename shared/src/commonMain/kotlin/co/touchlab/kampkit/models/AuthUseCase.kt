package co.touchlab.kampkit.models

import co.touchlab.kampkit.injectLogger
import co.touchlab.kampkit.ktor.KtorApi
import co.touchlab.kampkit.ktor.NetResponse
import co.touchlab.kermit.Logger
import co.touchlab.stately.ensureNeverFrozen
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.jvm.JvmInline

class AuthUseCase : KoinComponent {
    private val settings: Settings by inject()
    private val ktorApi: KtorApi by inject()
    private val log: Logger by injectLogger("BeerUseCase")
    private val clock: Clock by inject()

    init {
        ensureNeverFrozen()
    }

    suspend fun authenticateUserWith(
        username: String,
        password: String
    ): Flow<DataState<AuthDataSummary>> = flow {
        emit(DataState(loading = true))
        when (val result = ktorApi.postAuth(CredentialRequest(username, password))) {
            is NetResponse.Error -> emit(DataState(exception = result.message))
            is NetResponse.Success -> emit(
                DataState(
                    AuthDataSummary(
                        UserPacketInfo(
                            Username(result.dto.username), Email(result.dto.username),
                            Token(result.authToken)
                        )
                    )
                )
            )
        }
    }
}

data class AuthDataSummary(val userInfo: UserPacketInfo)

data class UserPacketInfo(val username: Username, val email: Email, val token: Token)

@JvmInline
value class Username(val username: String)

@JvmInline
value class Email(val email: String)

@JvmInline
value class Token(val rawToken: String)
