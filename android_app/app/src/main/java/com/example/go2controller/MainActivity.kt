package com.example.go2controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2controller.ui.theme.Go2ControllerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEFAULT_SERVER_BASE_URL = "http://192.168.1.128:8000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Go2ControllerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Go2ControllerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Go2ControllerScreen(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Waiting for command") }
    var lastCommand by remember { mutableStateOf("No command sent yet") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var serverBaseUrl by remember { mutableStateOf(DEFAULT_SERVER_BASE_URL) }

    val scope = rememberCoroutineScope()

    fun sendButtonCommand(command: String, code: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        status = "Sending command..."
        lastCommand = "$command → $code"

        commandHistory = listOf("$time   $command → $code") + commandHistory
        commandHistory = commandHistory.take(20)

        scope.launch {
            val result = sendCommandToServer(serverBaseUrl, command)
            status = result
        }
    }

    fun testConnection() {
        status = "Testing connection..."

        scope.launch {
            val result = testServerConnection(serverBaseUrl)
            status = result
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF064275))
    ) {
        if (maxWidth > 600.dp) {
            LandscapeDashboardLayout(
                status = status,
                lastCommand = lastCommand,
                serverBaseUrl = serverBaseUrl,
                onServerBaseUrlChange = { serverBaseUrl = it },
                commandHistory = commandHistory,
                onCommandSelected = { command, code ->
                    sendButtonCommand(command, code)
                },
                onTestConnection = {
                    testConnection()
                }
            )
        } else {
            PortraitDashboardLayout(
                status = status,
                lastCommand = lastCommand,
                serverBaseUrl = serverBaseUrl,
                onServerBaseUrlChange = { serverBaseUrl = it },
                commandHistory = commandHistory,
                onCommandSelected = { command, code ->
                    sendButtonCommand(command, code)
                },
                onTestConnection = {
                    testConnection()
                }
            )
        }
    }
}

@Composable
fun PortraitDashboardLayout(
    status: String,
    lastCommand: String,
    serverBaseUrl: String,
    onServerBaseUrlChange: (String) -> Unit,
    commandHistory: List<String>,
    onCommandSelected: (String, String) -> Unit,
    onTestConnection: () -> Unit
) {
    val panelModifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 430.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        DashboardTitle()

        Spacer(modifier = Modifier.height(14.dp))

        StatusCard(
            status = status,
            lastCommand = lastCommand,
            modifier = panelModifier
        )

        Spacer(modifier = Modifier.height(12.dp))

        ServerUrlField(
            serverBaseUrl = serverBaseUrl,
            onServerBaseUrlChange = onServerBaseUrlChange,
            modifier = panelModifier
        )

        Spacer(modifier = Modifier.height(18.dp))

        CommandGrid(
            modifier = panelModifier,
            onCommandSelected = onCommandSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        TestButton {
            onTestConnection()
        }

        Spacer(modifier = Modifier.height(16.dp))

        CameraPlaceholder(
            modifier = panelModifier
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        CommandHistoryCard(
            commandHistory = commandHistory,
            modifier = panelModifier
                .height(140.dp)
        )
    }
}

@Composable
fun LandscapeDashboardLayout(
    status: String,
    lastCommand: String,
    serverBaseUrl: String,
    onServerBaseUrlChange: (String) -> Unit,
    commandHistory: List<String>,
    onCommandSelected: (String, String) -> Unit,
    onTestConnection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        DashboardTitle()

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusCard(
                    status = status,
                    lastCommand = lastCommand,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ServerUrlField(
                    serverBaseUrl = serverBaseUrl,
                    onServerBaseUrlChange = onServerBaseUrlChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                CommandGrid(
                    modifier = Modifier.fillMaxWidth(),
                    buttonHeight = 42,
                    labelSize = 13,
                    codeSize = 8,
                    onCommandSelected = onCommandSelected
                )

                Spacer(modifier = Modifier.height(8.dp))

                TestButton {
                    onTestConnection()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CameraPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                CommandHistoryCard(
                    commandHistory = commandHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardTitle() {
    Text(
        text = "✦ Unitree Go2 Controller ✦",
        color = Color.White,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun StatusCard(
    status: String,
    lastCommand: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color(0xFF98D278),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Status",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = status,
            color = Color.Black,
            fontSize = 13.sp
        )

        Text(
            text = "Last: $lastCommand",
            color = Color.Black,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ServerUrlField(
    serverBaseUrl: String,
    onServerBaseUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = serverBaseUrl,
        onValueChange = onServerBaseUrlChange,
        label = {
            Text(
                text = "Server URL",
                color = Color.White
            )
        },
        textStyle = LocalTextStyle.current.copy(
            color = Color.White,
            fontSize = 13.sp
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri
        ),
        modifier = modifier
    )
}

@Composable
fun CommandGrid(
    modifier: Modifier = Modifier,
    buttonHeight: Int = 54,
    labelSize: Int = 16,
    codeSize: Int = 10,
    onCommandSelected: (String, String) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommandButton(
                label = "Stand up",
                code = "1001",
                buttonHeight = buttonHeight,
                labelSize = labelSize,
                codeSize = codeSize,
                modifier = Modifier.weight(1f),
                onClick = {
                    onCommandSelected("stand", "1001")
                }
            )

            CommandButton(
                label = "Lie down",
                code = "1002",
                buttonHeight = buttonHeight,
                labelSize = labelSize,
                codeSize = codeSize,
                modifier = Modifier.weight(1f),
                onClick = {
                    onCommandSelected("lie_down", "1002")
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommandButton(
                label = "Hello",
                code = "1003",
                buttonHeight = buttonHeight,
                labelSize = labelSize,
                codeSize = codeSize,
                modifier = Modifier.weight(1f),
                onClick = {
                    onCommandSelected("hello", "1003")
                }
            )

            CommandButton(
                label = "Sit",
                code = "1004",
                buttonHeight = buttonHeight,
                labelSize = labelSize,
                codeSize = codeSize,
                modifier = Modifier.weight(1f),
                onClick = {
                    onCommandSelected("sit", "1004")
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        CommandButton(
            label = "Heart",
            code = "1005",
            buttonHeight = buttonHeight,
            labelSize = labelSize,
            codeSize = codeSize,
            modifier = Modifier.fillMaxWidth(0.5f),
            onClick = {
                onCommandSelected("heart", "1005")
            }
        )
    }
}

@Composable
fun CommandButton(
    label: String,
    code: String,
    modifier: Modifier = Modifier,
    buttonHeight: Int = 54,
    labelSize: Int = 16,
    codeSize: Int = 10,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(buttonHeight.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFCAE9),
            contentColor = Color.Black
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = labelSize.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "code: $code",
                fontSize = codeSize.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun TestButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 220.dp, max = 280.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5CE7FF),
            contentColor = Color.Black
        )
    ) {
        Text(
            text = "Test Connection",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CameraPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color(0xFF0D5C96),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Camera / Monitor Feed",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Waiting for ROS2 video source...",
            color = Color.White,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Prepared for future Go2 camera stream integration.",
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
fun CommandHistoryCard(
    commandHistory: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.White,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Command history",
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (commandHistory.isEmpty()) {
                Text(
                    text = "No commands yet",
                    color = Color.Black,
                    fontSize = 13.sp
                )
            } else {
                commandHistory.forEach { item ->
                    Text(
                        text = item,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

suspend fun sendCommandToServer(
    serverBaseUrl: String,
    command: String
): String {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL("${serverBaseUrl.trim()}/command")
            connection = url.openConnection() as HttpURLConnection

            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = """{"command": "$command"}"""

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode

            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "No error message from the server"
            }

            if (responseCode in 200..299) {
                "Command sent: $command"
            } else {
                makeFriendlyErrorMessage(responseCode, responseText)
            }

        } catch (e: java.net.SocketTimeoutException) {
            "Connection timeout"
        } catch (e: java.net.ConnectException) {
            "Could not connect to server"
        } catch (e: Exception) {
            "Failed: ${e.message}"
        } finally {
            connection?.disconnect()
        }
    }
}

fun makeFriendlyErrorMessage(
    responseCode: Int,
    responseText: String
): String {
    return when {
        responseText.contains("Resource temporarily unavailable", ignoreCase = true) -> {
            "Relay received the command, but robot receiver is not available"
        }

        responseText.contains("Unknown command", ignoreCase = true) -> {
            "Unknown command. Check command mapping"
        }

        responseCode == 500 -> {
            "Relay error. Command was received, but could not reach the robot"
        }

        responseCode == 404 -> {
            "Relay endpoint not found. Check /command URL"
        }

        else -> {
            "Server error $responseCode"
        }
    }
}

suspend fun testServerConnection(serverBaseUrl: String): String {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL("${serverBaseUrl.trim()}/health")
            connection = url.openConnection() as HttpURLConnection

            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.requestMethod = "GET"

            val responseCode = connection.responseCode

            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "No error message from the server"
            }

            if (responseCode in 200..299) {
                "Server connected"
            } else {
                "Health check failed $responseCode: $responseText"
            }

        } catch (e: java.net.SocketTimeoutException) {
            "Connection timeout"
        } catch (e: java.net.ConnectException) {
            "Could not connect to server"
        } catch (e: Exception) {
            "Connection failed: ${e.message}"
        } finally {
            connection?.disconnect()
        }
    }
}