package com.immich.autodownloader.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immich.autodownloader.AppContainer
import com.immich.autodownloader.data.local.SyncLogEntity
import com.immich.autodownloader.data.repo.HomeSummary
import com.immich.autodownloader.data.repo.SyncSettingsInput
import com.immich.autodownloader.data.settings.DownloadFolder
import com.immich.autodownloader.data.settings.StoredSettings
import com.immich.autodownloader.device.buildDeviceInfoSnapshot
import com.immich.autodownloader.device.toPrettyJson
import com.immich.autodownloader.sync.ImmichSyncWorker
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class RootTab {
    HOME,
    SETTINGS,
    LOGS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmichSyncRoot(container: AppContainer) {
    val context = LocalContext.current
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = StoredSettings())
    val summary by container.repository.observeHomeSummary().collectAsStateWithLifecycle(initialValue = HomeSummary())
    val logs by container.repository.logs.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.HOME) }

    LaunchedEffect(settings.autoSync, settings.intervalMinutes, settings.wifiOnly, settings.chargeOnly) {
        if (settings.autoSync) {
            ImmichSyncWorker.schedule(
                context = context,
                wifiOnly = settings.wifiOnly,
                chargeOnly = settings.chargeOnly,
            )
        } else {
            ImmichSyncWorker.cancel(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Immich Auto Downloader") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == RootTab.HOME,
                    onClick = { selectedTab = RootTab.HOME },
                    icon = { Icon(Icons.Default.CloudDownload, null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = selectedTab == RootTab.SETTINGS,
                    onClick = { selectedTab = RootTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") },
                )
                NavigationBarItem(
                    selected = selectedTab == RootTab.LOGS,
                    onClick = { selectedTab = RootTab.LOGS },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, null) },
                    label = { Text("Logs") },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            RootTab.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                summary = summary,
                onSyncNow = {
                    scope.launch {
                        container.repository.runSync()
                    }
                },
            )

            RootTab.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(padding),
                settings = settings,
                onSave = { input ->
                    scope.launch {
                        container.repository.saveSettings(input)
                    }
                },
                onTestConnection = { url, apiKey, onResult ->
                    scope.launch {
                        val result = container.repository.testConnection(url, apiKey)
                        onResult(result)
                    }
                },
                onClearDatabase = {
                    scope.launch {
                        container.repository.clearLocalDatabase()
                    }
                },
            )

            RootTab.LOGS -> LogScreen(
                modifier = Modifier.padding(padding),
                logs = logs,
                onRetry = {
                    scope.launch {
                        container.repository.runSync(trigger = "retry")
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    summary: HomeSummary,
    onSyncNow: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Sync status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Total assets: ${summary.totalAssets}")
                    Text("Downloaded count: ${summary.downloadedCount}")
                    Text("Pending count: ${summary.pendingCount}")
                    Text("Last sync: ${summary.lastSyncTime?.toReadableTime() ?: "Never"}")
                    Text(summary.lastMessage ?: "Ready to sync")
                    Button(onClick = onSyncNow) {
                        Text("Sync Now")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: StoredSettings,
    onSave: (SyncSettingsInput) -> Unit,
    onTestConnection: (String, String, (Result<Int>) -> Unit) -> Unit,
    onClearDatabase: () -> Unit,
) {
    val context = LocalContext.current
    var serverUrl by rememberSaveable(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var apiKey by rememberSaveable(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var wifiOnly by rememberSaveable(settings.wifiOnly) { mutableStateOf(settings.wifiOnly) }
    var chargeOnly by rememberSaveable(settings.chargeOnly) { mutableStateOf(settings.chargeOnly) }
    var autoSync by rememberSaveable(settings.autoSync) { mutableStateOf(settings.autoSync) }
    var folder by rememberSaveable(settings.downloadFolder.storageValue) { mutableStateOf(settings.downloadFolder) }
    var connectionMessage by remember { mutableStateOf<String?>(null) }
    var copyMessage by remember { mutableStateOf<String?>(null) }
    var deviceJson by remember(context) { mutableStateOf(buildDeviceInfoSnapshot(context).toPrettyJson()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("SetupScreen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Immich Server URL") },
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
            )
        }
        item {
            Text("Choose download folder")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadFolder.entries.forEach { option ->
                    FilterChip(
                        selected = folder == option,
                        onClick = { folder = option },
                        label = { Text(option.relativePath) },
                    )
                }
            }
        }
        item {
            SettingsToggle("Wi-Fi only", wifiOnly) { wifiOnly = it }
        }
        item {
            SettingsToggle("Charge only", chargeOnly) { chargeOnly = it }
        }
        item {
            SettingsToggle("Auto sync", autoSync) { autoSync = it }
        }
        item {
            Text("Sync interval: fixed 1 minute")
        }
        item {
            Button(
                onClick = {
                    onTestConnection(serverUrl, apiKey) { result ->
                        connectionMessage = result.fold(
                            onSuccess = { "Connected. Server total preview: $it" },
                            onFailure = { "Connection failed: ${it.message}" },
                        )
                    }
                },
            ) {
                Text("Test Connection")
            }
        }
        if (connectionMessage != null) {
            item {
                Text(connectionMessage!!)
            }
        }
        item {
            Button(
                onClick = {
                    onSave(
                        SyncSettingsInput(
                            serverUrl = serverUrl,
                            apiKey = apiKey,
                            downloadFolder = folder,
                            wifiOnly = wifiOnly,
                            chargeOnly = chargeOnly,
                            autoSync = autoSync,
                            intervalMinutes = 1,
                        ),
                    )
                },
            ) {
                Text("Save")
            }
        }
        item {
            OutlinedButton(onClick = onClearDatabase) {
                Text("Clear local database")
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Device info JSON", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Doc thong tin may nhu device, product, manufacturer, fingerprint va xuat nhanh ra JSON.")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                deviceJson = buildDeviceInfoSnapshot(context).toPrettyJson()
                                copyMessage = "Device info refreshed"
                            },
                        ) {
                            Text("Refresh Device Info")
                        }
                        OutlinedIconButton(
                            onClick = {
                                copyToClipboard(context, "device_info.json", deviceJson)
                                copyMessage = "Device info JSON copied"
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy device JSON")
                        }
                    }
                    if (copyMessage != null) {
                        Text(copyMessage!!, color = MaterialTheme.colorScheme.primary)
                    }
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp),
                        ) {
                            Text(deviceJson, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
private fun LogScreen(
    modifier: Modifier = Modifier,
    logs: List<SyncLogEntity>,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
        items(logs) { log ->
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(log.level, fontWeight = FontWeight.Bold)
                    Text(log.message)
                    Text(log.createdAt.toReadableTime())
                }
            }
        }
    }
}

private fun Long.toReadableTime(): String {
    return DateFormat.getDateTimeInstance().format(Date(this))
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
