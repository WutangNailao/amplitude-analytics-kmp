package com.amplitude.kmp.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amplitude.kmp.events.EventOptions
import com.amplitude.kmp.events.Plan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicSampleScreen(onBack: () -> Unit) {
    var eventCount by remember { mutableStateOf(0) }
    var lastEventName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Basic Events") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
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
                text = "Basic Event Tracking",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Events sent: $eventCount",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (lastEventName.isNotEmpty()) {
                Text(
                    text = "Last event: $lastEventName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 32.dp),
                )
            }

            Button(
                onClick = {
                    // Simple event
                    AmplitudeManager.amplitude.track("button_clicked")
                    eventCount++
                    lastEventName = "button_clicked"
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Send Simple Event")
            }

            Button(
                onClick = {
                    // Event with properties
                    AmplitudeManager.amplitude.track(
                        eventType = "test_event_properties",
                        eventProperties =
                            mapOf(
                                "platform" to getPlatformName(),
                                "button_name" to "properties_button",
                                "event_count" to eventCount,
                            ),
                    )
                    eventCount++
                    lastEventName = "test_event_properties"
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
            ) {
                Text("Send Event with Properties")
            }

            Button(
                onClick = {
                    // Event with options
                    val options =
                        EventOptions().apply {
                            plan = Plan(branch = "test", source = "kmp-sample")
                        }
                    AmplitudeManager.amplitude.track(
                        eventType = "test_event_options",
                        eventProperties = mapOf("has_options" to true),
                        options = options,
                    )
                    eventCount++
                    lastEventName = "test_event_options"
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
            ) {
                Text("Send Event with Options")
            }
        }
    }
}
