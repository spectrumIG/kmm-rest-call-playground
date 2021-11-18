package co.touchlab.kampkit.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kampkit.injectLogger
import co.touchlab.kampkit.models.AuthUseCase
import co.touchlab.kampkit.models.UserPacketInfo
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class AppViewModel : ViewModel(), KoinComponent {

    private val log: Logger by injectLogger("BreedViewModel")
    private val scope = viewModelScope
    private val authUseCase: AuthUseCase = AuthUseCase()

    private val _autStateFlow: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Unininitialized) // Just to have an initializer...not very good

    val authStateFlow: StateFlow<ViewState> = _autStateFlow

    fun login(user: String, pass: String) {
        _autStateFlow.value = ViewState.Loading
        scope.launch {
            log.v { "refreshBeer" }
            authUseCase.authenticateUserWith(username = user, password = pass).collect { dataState ->
                when {
                    dataState.loading -> {
                        _autStateFlow.value = ViewState.Loading
                    }
                    dataState.data == null -> {
                        _autStateFlow.value = ViewState.Error(dataState.exception)
                    }
                    else -> {
                        _autStateFlow.value = ViewState.AuthSuccess(dataState.data?.userInfo)
                    }
                }
            }
        }
    }

    fun goToInitialState() {
        _autStateFlow.value = ViewState.Unininitialized
    }
}

sealed class ViewState {
    data class AuthSuccess(val authValue: UserPacketInfo?) : ViewState()
    data class Error(val message: String?) : ViewState()
    object Loading : ViewState()
    object Unininitialized : ViewState()
}
