package co.touchlab.kampkit.android.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import co.touchlab.kampkit.android.BeerViewModel
import co.touchlab.kampkit.android.R
import co.touchlab.kampkit.db.Beer
import co.touchlab.kampkit.models.DataState
import co.touchlab.kampkit.models.ItemDataSummary
import co.touchlab.kermit.Logger
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun MainScreen(
    viewModel: BeerViewModel,
    log: Logger
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleAwareDogsFlow = remember(viewModel.beerStateFlow, lifecycleOwner) {
        viewModel.beerStateFlow.flowWithLifecycle(lifecycleOwner.lifecycle)
    }
    val dogsState by lifecycleAwareDogsFlow.collectAsState(viewModel.beerStateFlow.value)

    MainScreenContent(
        beerState = dogsState,
        onRefresh = { viewModel.refreshBeer(true) },
        onSuccess = { data -> log.v { "View updating with ${data.allItems.size} breeds" } },
        onError = { exception -> log.e { "Displaying error: $exception" } },
        onFavorite = { viewModel.updateBeerFavorite(it) }
    )
}
@Composable
fun MainScreenContent(
    beerState: DataState<ItemDataSummary>,
    onRefresh: () -> Unit = {},
    onSuccess: (ItemDataSummary) -> Unit = {},
    onError: (String) -> Unit = {},
    onFavorite: (Beer) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = beerState.loading),
            onRefresh = onRefresh
        ) {
            if (beerState.empty) {
                Empty()
            }
            val data = beerState.data
            if (data != null) {
                LaunchedEffect(data) {
                    onSuccess(data)
                }
                Success(successData = data, favoriteBeer = onFavorite)
            }
            val exception = beerState.exception
            if (exception != null) {
                LaunchedEffect(exception) {
                    onError(exception)
                }
                Error(exception)
            }
        }
    }
}

@Composable
fun Empty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.empty_beer))
    }
}

@Composable
fun Error(error: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = error)
    }
}

@Composable
fun Success(
    successData: ItemDataSummary,
    favoriteBeer: (Beer) -> Unit
) {
    DogList(beers = successData.allItems, favoriteBeer)
}

@Composable
fun DogList(beers: List<Beer>, onItemClick: (Beer) -> Unit) {
    LazyColumn {
        items(beers) { breed ->
            DogRow(breed) {
                onItemClick(it)
            }
            Divider()
        }
    }
}

@Composable
fun DogRow(beer: Beer, onClick: (Beer) -> Unit) {
    Row(
        Modifier
            .clickable { onClick(beer) }
            .padding(10.dp)
    ) {
        Text(beer.name, Modifier.weight(1F))
        FavoriteIcon(beer)
    }
}

@Composable
fun FavoriteIcon(beer: Beer) {
    Crossfade(
        targetState = beer.favorite == 0L,
        animationSpec = TweenSpec(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    ) { fav ->
        if (fav) {
            Image(
                painter = painterResource(id = R.drawable.ic_favorite_border_24px),
                contentDescription = stringResource(R.string.favorite_beer, beer.name)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_favorite_24px),
                contentDescription = stringResource(R.string.unfavorite_beer, beer.name)
            )
        }
    }
}

@Preview
@Composable
fun MainScreenContentPreview_Success() {
    MainScreenContent(
        beerState = DataState(
            data = ItemDataSummary(
                longestItem = null,
                allItems = listOf(
                    Beer(0, "appenzeller", 0),
                    Beer(1, "australian", 1)
                )
            )
        )
    )
}
