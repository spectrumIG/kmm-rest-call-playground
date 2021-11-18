package co.touchlab.kampkit.android.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.touchlab.kampkit.android.AppViewModel
import co.touchlab.kampkit.android.ViewState
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: AppViewModel, log: Logger) {
    var userName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val user by viewModel.authStateFlow.collectAsState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxSize()
            .clickable { focusManager.clearFocus() }
    ) {
        when (user) {
            ViewState.Loading -> {
                CircularProgressIndicator()
            }
            is ViewState.AuthSuccess -> {

                Text("Login successful", fontSize = MaterialTheme.typography.h3.fontSize)
                Text(
                    "User ${(user as ViewState.AuthSuccess).authValue?.username}",
                    fontSize = MaterialTheme.typography.h4.fontSize
                )
                Text(
                    "Token ${(user as ViewState.AuthSuccess).authValue?.token}",
                    fontSize = MaterialTheme.typography.h4.fontSize
                )
            }
            is ViewState.Unininitialized -> {
                LoginFields(
                    userName,
                    password,
                    onLoginClick = { viewModel.login(userName, password) },
                    onUserChange = { userName = it },
                    onPasswordChange = { password = it }
                )
            }
            is ViewState.Error -> {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                LoginFields(
                    userName,
                    password,
                    onLoginClick = { viewModel.login("ttiganik@gmail.com", password) },
                    onUserChange = { userName = it },
                    onPasswordChange = { password = it }
                )

                coroutineScope.launch {
                    val result = snackbarHostState
                        .showSnackbar(
                            "Login error: ${(user as ViewState.Error).message}",
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Indefinite
                        )
                    when (result) {
                        SnackbarResult.ActionPerformed -> viewModel.goToInitialState()
                        SnackbarResult.Dismissed -> TODO()
                    }
                }

                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }
}

@Composable
fun LoginFields(
    username: String,
    password: String,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please login")

        OutlinedTextField(
            value = username,
            placeholder = { Text(text = "user@email.com") },
            label = { Text(text = "email") },
            onValueChange = onUserChange,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        OutlinedTextField(
            value = password,
            placeholder = { Text(text = "password") },
            label = { Text(text = "password") },
            onValueChange = onPasswordChange,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        Button(onClick = {
            if (username.isNotBlank() && password.isNotBlank()) {
                onLoginClick(username)
                focusManager.clearFocus()
            } else {
                Log.d("Login", "Login fallita.")
            }
        }) {
            Text("Login")
        }
    }
}

@Preview
@Composable
fun MainScreenContentPreview_Success() {
//    MainScreen(
//        beerState = DataState(
//            data = ItemDataSummary(
//                longestItem = null,
//                allItems = listOf(
//                    Beer(0, "appenzeller", 0),
//                    Beer(1, "australian", 1)
//                )
//            )
//        )
//    )
}
