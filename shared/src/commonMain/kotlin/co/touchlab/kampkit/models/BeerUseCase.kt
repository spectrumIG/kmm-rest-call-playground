package co.touchlab.kampkit.models

import co.touchlab.kampkit.DatabaseHelper
import co.touchlab.kampkit.db.Beer
import co.touchlab.kampkit.injectLogger
import co.touchlab.kampkit.ktor.KtorApi
import co.touchlab.kermit.Logger
import co.touchlab.stately.ensureNeverFrozen
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BeerUseCase : KoinComponent {
    private val dbHelper: DatabaseHelper by inject()
    private val settings: Settings by inject()
    private val ktorApi: KtorApi by inject()
    private val log: Logger by injectLogger("BeerUseCase")
    private val clock: Clock by inject()

    companion object {
        internal const val DB_TIMESTAMP_KEY = "DbTimestampKey"
    }

    init {
        ensureNeverFrozen()
    }

    fun refreshBeerIfStale(forced: Boolean = false): Flow<DataState<ItemDataSummary>> = flow {
        emit(DataState(loading = true))
        val currentTimeMS = clock.now().toEpochMilliseconds()
        val stale = isBeerListStale(currentTimeMS)
        val networkBeerDataState: DataState<ItemDataSummary>
        if (stale || forced) {
            networkBeerDataState = getBeersFromNetwork(currentTimeMS)
            if (networkBeerDataState.data != null) {
                dbHelper.insertBeers(networkBeerDataState.data.allItems)
            } else {
                emit(networkBeerDataState)
            }
        }
    }

    fun getBeerFromCache(): Flow<DataState<ItemDataSummary>> =
        dbHelper.selectAllItems()
            .mapNotNull { itemList ->
                if (itemList.isEmpty()) {
                    null
                } else {
                    DataState<ItemDataSummary>(
                        data = ItemDataSummary(
                            itemList.maxByOrNull { it.name.length },
                            itemList
                        )
                    )
                }
            }

    private fun isBeerListStale(currentTimeMS: Long): Boolean {
        val lastDownloadTimeMS = settings.getLong(DB_TIMESTAMP_KEY, 0)
        val oneHourMS = 60 * 60 * 1000
        val stale = lastDownloadTimeMS + oneHourMS < currentTimeMS
        if (!stale) {
            log.i { "Breeds not fetched from network. Recently updated" }
        }
        return stale
    }

    suspend fun getBeersFromNetwork(currentTimeMS: Long): DataState<ItemDataSummary> {
        return try {
            val beerResult = ktorApi.getJsonBeersFromApi()
            log.v { "Breed network result: ${beerResult.isNotEmpty()}" }
            log.v { "Fetched ${beerResult.size} breeds from network" }
            settings.putLong(DB_TIMESTAMP_KEY, currentTimeMS)
            if (beerResult.isEmpty()) {
                DataState<ItemDataSummary>(empty = true)
            } else {
                DataState<ItemDataSummary>(
                    ItemDataSummary(
                        null,
                        beerResult.map { Beer(0L, it.name.orEmpty(), 0L) }
                    )
                )
            }
        } catch (e: Exception) {
            log.e(e) { "Error downloading breed list" }
            DataState<ItemDataSummary>(exception = "Unable to download breed list")
        }
    }

    suspend fun updateBeerFavorite(beer: Beer) {
        dbHelper.updateFavorite(beer.id, beer.favorite != 1L)
    }
}

data class ItemDataSummary(val longestItem: Beer?, val allItems: List<Beer>)
