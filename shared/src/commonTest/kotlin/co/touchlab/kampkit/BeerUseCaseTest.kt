package co.touchlab.kampkit

import app.cash.turbine.test
import co.touchlab.kampkit.db.Beer
import co.touchlab.kampkit.mock.ClockMock
import co.touchlab.kampkit.mock.KtorApiMock
import co.touchlab.kampkit.models.BeerUseCase
import co.touchlab.kampkit.models.DataState
import co.touchlab.kampkit.models.ItemDataSummary
import co.touchlab.kermit.Logger
import com.russhwolf.settings.MockSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class BeerUseCaseTest : BaseTest() {

    private var model: BeerUseCase = BeerUseCase()
    private var kermit = Logger
    private var testDbConnection = testDbConnection()
    private var dbHelper = DatabaseHelper(
        testDbConnection,
        kermit,
        Dispatchers.Default
    )
    private val settings = MockSettings()
    private val ktorApi = KtorApiMock()

    // Need to start at non-zero time because the default value for db timestamp is 0
    private val clock = ClockMock(Clock.System.now())

    companion object {
        private val weissbier = Beer(1, "weissbier", 0L)
        private val punkIpaNoLike = Beer(2, "Punk Ipa", 0L)
        private val punkIpaLike = Beer(2, "Punk Ipa", 1L)
        val dataStateSuccessNoFavorite = DataState(
            data = ItemDataSummary(weissbier, listOf(weissbier, punkIpaNoLike))
        )
        private val dataStateSuccessFavorite = DataState(
            data = ItemDataSummary(weissbier, listOf(weissbier, punkIpaLike))
        )
    }

    @BeforeTest
    fun setup() {
        appStart(dbHelper, settings, ktorApi, kermit, clock)
    }

    @Test
    fun staleDataCheckTest() = runTest {
        val currentTimeMS = Clock.System.now().toEpochMilliseconds()
        settings.putLong(BeerUseCase.DB_TIMESTAMP_KEY, currentTimeMS)
        assertTrue(ktorApi.calledCount == 0)

        val expectedError = DataState<ItemDataSummary>(exception = "Unable to download breed list")
        val actualError = model.getBeersFromNetwork(0L)

        assertEquals(
            expectedError,
            actualError
        )
        assertTrue(ktorApi.calledCount == 0)
    }

    @OptIn(FlowPreview::class)
    @Test
    fun updateFavoriteTest() = runTest {
        ktorApi.prepareResult(ktorApi.successResult())

        flowOf(model.refreshBeerIfStale(), model.getBeerFromCache())
            .flattenMerge().test {
                // Loading
                assertEquals(DataState(loading = true), awaitItem())
                // No Favorites
                assertEquals(dataStateSuccessNoFavorite, awaitItem())
                // Add 1 favorite breed
                model.updateBeerFavorite(punkIpaNoLike)
                // Get the new result with 1 breed favorited
                assertEquals(dataStateSuccessFavorite, awaitItem())
            }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun fetchBeersFromNetworkPreserveFavorites() {
        ktorApi.prepareResult(ktorApi.successResult())

        runTest {
            flowOf(model.refreshBeerIfStale(), model.getBeerFromCache())
                .flattenMerge().test {
                    // Loading
                    assertEquals(DataState(loading = true), awaitItem())
                    assertEquals(dataStateSuccessNoFavorite, awaitItem())
                    // "Like" the Australian breed
                    model.updateBeerFavorite(punkIpaNoLike)
                    // Get the new result with the Australian breed liked
                    assertEquals(dataStateSuccessFavorite, awaitItem())
                    cancel()
                }
        }

        runTest {
            // Fetch beers from the network (no beers liked),
            // but preserved the liked beers in the database.
            flowOf(model.refreshBeerIfStale(true), model.getBeerFromCache())
                .flattenMerge().test {
                    // Loading
                    assertEquals(DataState(loading = true), awaitItem())
                    // Get the new result with the Australian breed liked
                    assertEquals(dataStateSuccessFavorite, awaitItem())
                    cancel()
                }
        }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun updateDatabaseTest() = runTest {
        val successResult = ktorApi.successResult()
        ktorApi.prepareResult(successResult)
        flowOf(model.refreshBeerIfStale(), model.getBeerFromCache()).flattenMerge()
            .test(timeout = Duration.seconds(30)) {
                assertEquals(DataState(loading = true), awaitItem())
                val oldBeer = awaitItem()
                val data = oldBeer.data
                assertTrue(data != null)
                assertEquals(
                    ktorApi.successResult().size,
                    data.allItems.size
                )
            }

        // Advance time by more than an hour to make cached data stale
        clock.currentInstant += Duration.hours(2)
        val resultWithExtraBeer = successResult.map { it.copy(tagline = it.tagline + ("extra" to emptyList<Beer>())) }

        ktorApi.prepareResult(resultWithExtraBeer)
        flowOf(model.refreshBeerIfStale(), model.getBeerFromCache()).flattenMerge()
            .test(timeout = Duration.seconds(30)) {
                assertEquals(DataState(loading = true), awaitItem())
                val updated = awaitItem()
                val data = updated.data
                assertTrue(data != null)
                assertEquals(resultWithExtraBeer.size, data.allItems.size)
            }
    }

    @Test
    fun notifyErrorOnException() = runTest {
        ktorApi.throwOnCall(RuntimeException())
        assertNotNull(model.getBeersFromNetwork(0L))
    }

    @AfterTest
    fun breakdown() = runTest {
        testDbConnection.close()
        appEnd()
    }
}
