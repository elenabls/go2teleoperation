package com.example.go2controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.example.go2controller.ui.theme.Go2ControllerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.shadow.ShadowContext
import java.util.concurrent.ExecutionException


private const val SERVER_BASE_URL = "http://192.168.1.119:8000"
private const val COMMAND_URL = "$SERVER_BASE_URL/command"
private const val HEALTH_URL =  "$SERVER_BASE_URL/health"

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
    val scope = rememberCoroutineScope()

    fun sendButtonCommand(command: String) {
        status = "Sending command: $command"

        scope.launch {
            val result = sendCommandToServer(command)
            status = result
        }
    }

    fun testConnection() {
        status = "Testing connection..."

        scope.launch {
            val result = testServerConnection()
            status = result
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF064275))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✦ Unitree Go2 Controller ✦",
            color= Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    Color(0xFF064275),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Status: $status",
            color = Color.Black,
            modifier = Modifier
                .background(
                    Color(0xFF98D278),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
            )

        Spacer(modifier = Modifier.height(32.dp))

        CommandButton("↑", "go") {
            sendButtonCommand("go")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CommandButton("←", "left") {
                sendButtonCommand("left")
            }

            StopButton {
                sendButtonCommand("stop")
            }

            CommandButton("→", "right") {
                sendButtonCommand("right")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CommandButton("↓", "back") {
            sendButtonCommand("back")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SitButton {
                sendButtonCommand("sit")
            }

            StandButton {
                sendButtonCommand("stand")
            }
        }
        Spacer(modifier = Modifier.height(32.dp) )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { TestButton {
            testConnection()
        }
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
            .height(48.dp)
            .width(200.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5CE7FF),
            contentColor = Color.Black
        )
    ) {
        Text(
            text = "Test Connection",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun CommandButton(
    label: String,
    command: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp).width(110.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFFFCAE9),
            contentColor = androidx.compose.ui.graphics.Color.Black
        )
    ) {
        Text(
            text = label,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StopButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Red,
            contentColor = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Text("Stop")
    }
}

@Composable
fun StandButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFFBF249),
            contentColor = androidx.compose.ui.graphics.Color.Black
        )
    ) {
        Text("Stand")
    }
}

@Composable
fun SitButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFFBF249),
            contentColor = androidx.compose.ui.graphics.Color.Black
        )
    ) {
        Text("Sit")
    }
}

suspend fun sendCommandToServer(command: String): String {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL(COMMAND_URL)
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
                "Server error $responseCode: $responseText"
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

suspend fun testServerConnection(): String {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL(HEALTH_URL)
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

