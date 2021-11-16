package co.touchlab.kampkit

import co.touchlab.kampkit.db.Beer
import co.touchlab.kampkit.db.KaMPKitDb
import co.touchlab.kampkit.sqldelight.transactionWithContext
import co.touchlab.kermit.Logger
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class DatabaseHelper(
    sqlDriver: SqlDriver,
    private val log: Logger,
    private val backgroundDispatcher: CoroutineDispatcher
) {
    private val dbRef: KaMPKitDb = KaMPKitDb(sqlDriver)

    fun selectAllItems(): Flow<List<Beer>> =
        dbRef.tableQueries
            .selectAll()
            .asFlow()
            .mapToList()
            .flowOn(backgroundDispatcher)

    suspend fun insertBeers(beer: List<Beer>) {
        log.d { "Inserting ${beer.size} beers into database" }
        dbRef.transactionWithContext(backgroundDispatcher) {
            beer.forEach { beer ->
                dbRef.tableQueries.insertBeer(null, beer.name)
            }
        }
    }

    suspend fun selectById(id: Long): Flow<List<Beer>> =
        dbRef.tableQueries
            .selectById(id)
            .asFlow()
            .mapToList()
            .flowOn(backgroundDispatcher)

    suspend fun deleteAll() {
        log.i { "Database Cleared" }
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.tableQueries.deleteAll()
        }
    }

    suspend fun updateFavorite(beerId: Long, favorite: Boolean) {
        log.i { "Beer $beerId: Favorited $favorite" }
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.tableQueries.updateFavorite(favorite.toLong(), beerId)
        }
    }
}

fun Beer.isFavorited(): Boolean = this.favorite != 0L
internal fun Boolean.toLong(): Long = if (this) 1L else 0L
