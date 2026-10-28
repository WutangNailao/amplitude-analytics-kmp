package com.amplitude.kmp.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    // Initialize Amplitude once when app starts
    LaunchedEffect(Unit) {
        AmplitudeInitializer.initializeAndAwait(
            apiKey = amplitudeApiKey,
            platformContext = platformContext,
            userId = "kmp-sample-user-${getPlatformName()}",
        )
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            var currentScreen by remember { mutableStateOf(Screen.HOME) }

            when (currentScreen) {
                Screen.HOME ->
                    HomeScreen(
                        onBasicClick = { currentScreen = Screen.BASIC },
                        onAdvancedClick = { currentScreen = Screen.ADVANCED },
                    )
                Screen.BASIC ->
                    BasicSampleScreen(
                        onBack = { currentScreen = Screen.HOME },
                    )
                Screen.ADVANCED ->
                    AdvancedSampleScreen(
                        onBack = { currentScreen = Screen.HOME },
                    )
            }
        }
    }
}

enum class Screen {
    HOME,
    BASIC,
    ADVANCED,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBasicClick: () -> Unit,
    onAdvancedClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amplitude KMP Sample") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Welcome to Amplitude KMP!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Text(
                text = "This sample demonstrates Amplitude SDK usage in Kotlin Multiplatform",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Button(
                onClick = onBasicClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Basic Events")
            }

            Button(
                onClick = onAdvancedClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
            ) {
                Text("Advanced Features")
            }
        }
    }
}
