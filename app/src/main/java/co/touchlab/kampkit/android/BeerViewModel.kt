package co.touchlab.kampkit.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kampkit.db.Beer
import co.touchlab.kampkit.injectLogger
import co.touchlab.kampkit.models.BeerUseCase
import co.touchlab.kampkit.models.DataState
import co.touchlab.kampkit.models.ItemDataSummary
import co.touchlab.kermit.Logger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class BeerViewModel : ViewModel(), KoinComponent {

    private val log: Logger by injectLogger("BreedViewModel")
    private val scope = viewModelScope
    private val beerUseCase: BeerUseCase = BeerUseCase()
    private val _beerStateFlow: MutableStateFlow<DataState<ItemDataSummary>> = MutableStateFlow(
        DataState(loading = true)
    )

    val beerStateFlow: StateFlow<DataState<ItemDataSummary>> = _beerStateFlow

    init {
        observeBeer()
    }

    @OptIn(FlowPreview::class)
    private fun observeBeer() {
        scope.launch {
            log.v { "getBreeds: Collecting Things" }
            flowOf(
                beerUseCase.refreshBeerIfStale(true),
                beerUseCase.getBeerFromCache()
            ).flattenMerge().collect { dataState ->
                if (dataState.loading) {
                    val temp = _beerStateFlow.value.copy(loading = true)
                    _beerStateFlow.value = temp
                } else {
                    _beerStateFlow.value = dataState
                }
            }
        }
    }

    fun refreshBeer(forced: Boolean = false) {
        scope.launch {
            log.v { "refreshBeer" }
            beerUseCase.refreshBeerIfStale(forced).collect { dataState ->
                if (dataState.loading) {
                    val temp = _beerStateFlow.value.copy(loading = true)
                    _beerStateFlow.value = temp
                } else {
                    _beerStateFlow.value = dataState
                }
            }
        }
    }

    fun updateBeerFavorite(beer: Beer) {
        scope.launch {
            beerUseCase.updateBeerFavorite(beer)
        }
    }
}
