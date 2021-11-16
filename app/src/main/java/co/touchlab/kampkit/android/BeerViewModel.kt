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
    private val _breedStateFlow: MutableStateFlow<DataState<ItemDataSummary>> = MutableStateFlow(
        DataState(loading = true)
    )

    val breedStateFlow: StateFlow<DataState<ItemDataSummary>> = _breedStateFlow

    init {
        observeBreeds()
    }

    @OptIn(FlowPreview::class)
    private fun observeBreeds() {
        scope.launch {
            log.v { "getBreeds: Collecting Things" }
            flowOf(
                beerUseCase.refreshBeerIfStale(true),
                beerUseCase.getBeerFromCache()
            ).flattenMerge().collect { dataState ->
                if (dataState.loading) {
                    val temp = _breedStateFlow.value.copy(loading = true)
                    _breedStateFlow.value = temp
                } else {
                    _breedStateFlow.value = dataState
                }
            }
        }
    }

    fun refreshBreeds(forced: Boolean = false) {
        scope.launch {
            log.v { "refreshBreeds" }
            beerUseCase.refreshBeerIfStale(forced).collect { dataState ->
                if (dataState.loading) {
                    val temp = _breedStateFlow.value.copy(loading = true)
                    _breedStateFlow.value = temp
                } else {
                    _breedStateFlow.value = dataState
                }
            }
        }
    }

    fun updateBreedFavorite(beer: Beer) {
        scope.launch {
            beerUseCase.updateBeerFavorite(beer)
        }
    }
}
