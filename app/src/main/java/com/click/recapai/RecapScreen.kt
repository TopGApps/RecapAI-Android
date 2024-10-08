package com.click.recapai

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapScreen(viewModel: GeminiAPIViewModel = viewModel()) {
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var showQuiz by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf(TextFieldValue("")) }
    var urlList by remember { mutableStateOf(mutableListOf<String>()) }
    var imageUris by remember { mutableStateOf(mutableListOf<Uri>()) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var responseText by remember { mutableStateOf("") }
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris != null) {
                imageUris = uris.take(5).toMutableList() // Limit to 5 images
            }
        }

    if (showQuiz) {
        quiz?.let {
            QuizScreen(
                it,
                onFinish = { showQuiz = false },
                onBack = { showQuiz = false }, // Pass the onBack parameter
                geminiAPIViewModel = viewModel
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Recap AI") },
                    actions = {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState
                    ) {
                        SettingsPanel(viewModel)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (showAlertDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlertDialog = false },
                        confirmButton = {
                            Button(onClick = { showAlertDialog = false }) { Text("OK") }
                        },
                        text = { Text(responseText) }
                    )
                }

                Text(
                    text = "Upload Images (up to 5)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Button(onClick = { imageLauncher.launch("image/*") }) {
                        Text(text = "Select Images")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageUris) { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).border(BorderStroke(1.dp, Color.Gray)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter Text") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = "Enter your notes here") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add URLs (up to 5)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                urlList.forEachIndexed { index, url ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        TextField(
                            value = url,
                            onValueChange = { newUrl -> urlList[index] = newUrl },
                            label = { Text("URL ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { urlList.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove URL")
                        }
                    }
                }

                if (urlList.size < 5) {
                    Button(onClick = { urlList.add("") }) { Text(text = "Add URL") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.sendMessage(
                            userInput = textInput.text,
                            imageUris = imageUris,
                            context = context,
                            generateQuiz = true
                        ) { response ->
                            Log.d("RecapScreen", "Response: $response")
                            responseText = response
                            try {
                                quiz = parseQuizJson(response)
                                if (quiz != null) {
                                    showQuiz = true
                                } else {
                                    showAlertDialog = true
                                }
                            } catch (e: SerializationException) {
                                Log.e("RecapScreen", "Error parsing quiz: ${e.message}")
                                responseText = "Failed to parse quiz. Please try again."
                                showAlertDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(text = "Send", fontSize = 18.sp) }
            }
        }
    }
}
@Composable
fun ModelButton(modelName: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Button(
            onClick = onClick,
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor =
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                    )
    ) {
        Icon(
                imageVector = icon,
                contentDescription = modelName,
                tint =
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = modelName)
    }
}

@Composable
fun SettingsPanel(viewModel: GeminiAPIViewModel) {
    var apiKey by remember { mutableStateOf(viewModel.getAPIKey()) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(viewModel.getModelName()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
        apiKey = viewModel.getAPIKey()
        selectedModel = viewModel.getModelName()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = if (showApiKey) apiKey else "*".repeat(apiKey.length),
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                                imageVector =
                                        if (showApiKey) Icons.Filled.Info else Icons.Filled.Info,
                                contentDescription =
                                        if (showApiKey) "Hide API Key" else "Show API Key"
                        )
                    }
                }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Choose AI Model")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            ModelButton(
                    modelName = "gemini-1.5-pro",
                    icon = Icons.Filled.Info,
                    isSelected = selectedModel == "gemini-1.5-pro",
                    onClick = { selectedModel = "gemini-1.5-pro" }
            )
            ModelButton(
                    modelName = "gemini-1.5-flash",
                    icon = Icons.Filled.Info,
                    isSelected = selectedModel == "gemini-1.5-flash",
                    onClick = { selectedModel = "gemini-1.5-flash" }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.updateSettings(apiKey, selectedModel)
                    viewModel.saveSettings()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Settings saved successfully")
                    }
                }
        ) { Text("Save") }
    }

    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(snackbarData = data, modifier = Modifier.padding(8.dp))
    }

    Spacer(modifier = Modifier.height(16.dp))
}
