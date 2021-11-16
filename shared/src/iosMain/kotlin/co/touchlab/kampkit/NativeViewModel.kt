package co.touchlab.kampkit

import co.touchlab.kampkit.db.Beer
import co.touchlab.kampkit.models.BeerUseCase
import co.touchlab.kampkit.models.DataState
import co.touchlab.kampkit.models.ItemDataSummary
import co.touchlab.kermit.Logger
import co.touchlab.stately.ensureNeverFrozen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class NativeViewModel(
    private val onDataState: (DataState<ItemDataSummary>) -> Unit
) : KoinComponent {

    private val log: Logger by injectLogger("BreedModel")
    private val scope = MainScope(Dispatchers.Main, log)
    private val beerUseCase: BeerUseCase = BeerUseCase()
    private val _beerStateFlow: MutableStateFlow<DataState<ItemDataSummary>> = MutableStateFlow(
        DataState(loading = true)
    )

    init {
        ensureNeverFrozen()
        observeBeers()
    }

    fun consumeError() {
        _beerStateFlow.value = _beerStateFlow.value.copy(exception = null)
    }

    @OptIn(FlowPreview::class)
    fun observeBeers() {
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

        scope.launch {
            log.v { "Exposing flow through callbacks" }
            _beerStateFlow.collect { dataState ->
                onDataState(dataState)
            }
        }
    }

    fun refreshBeers(forced: Boolean = false) {
        scope.launch {
            log.v { "refreshBeers" }
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

    fun onDestroy() {
        scope.onDestroy()
    }
}
