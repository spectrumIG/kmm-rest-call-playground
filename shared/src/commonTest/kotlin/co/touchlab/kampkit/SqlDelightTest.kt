package co.touchlab.kampkit

import co.touchlab.kampkit.db.Beer
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightTest : BaseTest() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var beers: List<Beer>

    private suspend fun DatabaseHelper.insertBeer(name: String) {
        insertBeers(listOf(Beer(id = 1, name = name, favorite = 0L)))
    }

    @BeforeTest
    fun setup() = runTest {
        dbHelper = DatabaseHelper(
            testDbConnection(),
            Logger,
            Dispatchers.Default
        )
        dbHelper.deleteAll()

        beers = listOf(
            Beer(0L, "PunkIPA", 0L),
            Beer(0L, "PunkIPA", 0L),
            Beer(0L, "PunkIPA", 0L),
        )

        dbHelper.insertBeers(beers)
    }

    @Test
    fun `Select All Items Success`() = runTest {
        val beers = dbHelper.selectAllItems().first()
        assertNotNull(
            beers.find { it.name == "PunkIPA" },
            "Could not retrieve Beer"
        )
    }

    @Test
    fun `Select Item by Id Success`() = runTest {
        val beers = dbHelper.selectAllItems().first()
        val firstBeer = beers.first()
        assertNotNull(
            dbHelper.selectById(firstBeer.id),
            "Could not retrieve Beer by Id"
        )
    }

    @Test
    fun `Update Favorite Success`() = runTest {
        val beers = dbHelper.selectAllItems().first()
        val firstBeer = beers.first()
        dbHelper.updateFavorite(firstBeer.id, true)
        val newBeer = dbHelper.selectById(firstBeer.id).first().first()
        assertNotNull(
            newBeer,
            "Could not retrieve Beer by Id"
        )
        assertTrue(
            newBeer.isFavorited(),
            "Favorite Did Not Save"
        )
    }

    @Test
    fun `Delete All Success`() = runTest {
        dbHelper.insertBeer("Poodle")
        dbHelper.insertBeer("Schnauzer")
        assertTrue(dbHelper.selectAllItems().first().isNotEmpty())
        dbHelper.deleteAll()

        assertTrue(
            dbHelper.selectAllItems().first().count() == 0,
            "Delete All did not work"
        )
    }
}
