package co.touchlab.kampkit

import co.touchlab.kampkit.models.AuthUseCase
import co.touchlab.kampkit.models.UserPacketInfo
import co.touchlab.kermit.Logger
import co.touchlab.stately.ensureNeverFrozen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class NativeViewModel(
    private val onDataState: (ViewState) -> Unit
) : KoinComponent {

    private val log: Logger by injectLogger("BreedModel")
    private val scope = MainScope(Dispatchers.Main, log)
    private val authUseCase: AuthUseCase = AuthUseCase()

    private val _autStateFlow: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Uninitialized) // Just to have an initializer...not very good

    val authStateFlow: StateFlow<ViewState> = _autStateFlow

    init {
        ensureNeverFrozen()
        exposeDataState() // <-- this is the point of conjunction between flow and callback. Simpler but not scalable way to manage flow in a native way.
    }

    private fun exposeDataState() {
        scope.launch {
            log.v { "Exposing flow through callbacks" }
            _autStateFlow.collect { dataState ->
                onDataState(dataState)
            }
        }
    }

    fun login(user: String, pass: String) {
        _autStateFlow.value = ViewState.Loading
        scope.launch {
            log.v { "Auth user action started" }
            authUseCase.authenticateUserWith(username = user, password = pass).collect { dataState ->
                when {
                    dataState.loading -> {
                        _autStateFlow.value = ViewState.Loading
                    }
                    dataState.data == null -> {
                        _autStateFlow.value = ViewState.Error(dataState.exception)
                    }
                    else -> {
                        _autStateFlow.value = ViewState.AuthSuccess(dataState.data.userInfo)
                    }
                }
            }
        }
    }

    fun goToInitialState() {
        _autStateFlow.value = ViewState.Uninitialized
    }

    fun onDestroy() {
        scope.onDestroy()
    }
}

sealed class ViewState {
    data class AuthSuccess(val authValue: UserPacketInfo?) : ViewState()
    data class Error(val message: String?) : ViewState()
    object Loading : ViewState()
    object Uninitialized : ViewState()
}
