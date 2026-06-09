# Week 1 Prototype Documentation: Android-Based Teleoperation Interface for Unitree Go2

## 1. Project Goal

The final goal of the project is to teleoperate the Unitree Go2 robot using physical human motion. Initially, the intended input device was the Meta Quest 3, which would be used to read motion signals from the user. However, another possible approach is to use a wearable device, such as a smartwatch, to collect hand-motion or gesture data.

The long-term system would therefore follow this idea:

```text
Human motion / gesture input
        ↓
Wearable device / Meta Quest 3
        ↓
Communication layer
        ↓
Robot control interface
        ↓
Unitree Go2
```

For the first sprint, the focus is reduced to a simpler and testable goal: sending basic commands to the robot from an Android application. Instead of using physical gestures immediately, the user presses buttons in the app, such as “Sit” or “Stop,” and the selected command is sent to a relay server.

The purpose of this first prototype is to verify that the Android app can communicate reliably with an intermediate server before connecting the system to the actual robot control layer.

## 2. Week 1 System Architecture

The Week 1 prototype is composed of three main parts:

```text
Android App
    ↓
Communication Bridge / Relay Server
    ↓
Robot Control Interface
    ↓
Unitree Go2
```

### 2.1 Android App

The Android app acts as the user interface. It displays command buttons that the user can press to send instructions such as:

```text
stand
sit
go
stop
left
right
back
```

Each button is mapped to a command string. When the user presses a button, the app sends the corresponding command to the relay server using an HTTP request.

### 2.2 Communication Bridge / Relay Server

The relay server acts as the middle layer between the Android app and the robot. For the current prototype, it is implemented as a simple Python HTTP server running locally on port `8000`.

The relay receives JSON messages from the Android app, extracts the command, and prints it in the Python terminal. This confirms that the app is successfully sending commands to the server.

At this stage, the relay does not yet execute robot actions directly. Its role is to validate the communication pipeline.

### 2.3 Robot Control Interface

The next step will be to connect the relay server to the actual Unitree Go2 control interface. This may be done through ROS2, WebRTC, or another Wi-Fi-based communication method, depending on the available robot-side API and project requirements.

## 3. Android App Implementation

The Android application was built using Android Studio and Kotlin. The Android SDK is installed at:

```text
C:\Android\Sdk
```

The project directory is:

```text
C:\elena\AndroidStudioProjects\Go2Controller
```

The main application code is located in:

```text
app/src/main/java/com/example/go2controller/MainActivity.kt
```

The app uses Jetpack Compose, meaning that the user interface is written directly in Kotlin code instead of XML layout files.

## 4. Initial App Prototype

The first version of the app was a basic interface with simple buttons. Pressing a button only changed the status message displayed on the screen. No communication with an external server was implemented at that stage.

The initial workflow was:

```text
App opens
    ↓
MainActivity runs
    ↓
Go2ControllerScreen is displayed
    ↓
Status starts as "Waiting for command"
    ↓
User presses a button
    ↓
Status text updates on the screen
```

This version was useful for understanding the basic structure of a Compose app:

* `MainActivity` starts the app.
* `setContent` defines what is displayed on the screen.
* `Go2ControllerScreen` contains the UI elements.
* `Column`, `Row`, `Button`, `Text`, and `Spacer` are used to arrange the interface.
* A `status` variable stores and updates the current app message.

## 5. Updated App Prototype

The app was later improved both visually and functionally.

The current UI is arranged more like a remote controller instead of a vertical list of buttons. Directional commands are placed in an intuitive layout:

```text
        ↑

←     Stop     →

        ↓

Sit        Stand
```

This makes the interface easier to understand, especially for movement-based robot control.

The app also includes:

* a custom title: `✦ Unitree Go2 Controller ✦`
* styled command buttons
* a separate red Stop button
* separate Sit and Stand buttons
* a status display box
* a Test button for checking server connectivity
* custom colors and larger button text

The current main color choices are:

```text
Background: dark blue #064275
Status box: green #98D278
Movement buttons: pink #FFCAE9
Posture buttons: yellow #FBF249
Stop button: red
```

The updated interface is still simple, but it is clearer, more usable, and easier to debug.

## 6. Communication Layer

The communication layer uses HTTP requests between the Android app and the Python relay server.

There are currently two endpoints:

```text
POST /command
GET  /health
```

### 6.1 Command Endpoint

The `/command` endpoint is used to send robot commands.

When a user presses a command button, the Android app creates a JSON message such as:

```json
{"command": "sit"}
```

The message is sent as an HTTP POST request to:

```text
http://192.168.1.119:8000/command
```

The Python server receives the request, extracts the command, and prints it in the terminal.

Example terminal output:

```text
Received command: sit
```

This confirms that the command was successfully sent from the Android app to the relay server.

The command flow is:

```text
User presses Sit
    ↓
Android app calls sendCommandToServer("sit")
    ↓
App sends POST request to /command
    ↓
Python relay receives JSON command
    ↓
Relay prints the command in the terminal
    ↓
App updates the status message
```

### 6.2 Health Check Endpoint

A separate `/health` endpoint was added for debugging. This endpoint allows the app to check whether the server is reachable without sending an actual robot command.

The Test button in the app sends a GET request to:

```text
http://192.168.1.119:8000/health
```

If the relay server is running and reachable, it responds with a success message. The app then displays a status such as:

```text
Server connected
```

This is useful because it separates connection testing from robot commands.

The debugging flow is:

```text
User presses Test
    ↓
Android app sends GET request to /health
    ↓
Python relay responds
    ↓
App displays connection status
```

This helped identify and fix a URL path issue during development, where the app was accidentally sending requests to:

```text
/command/command
/command/health
```

instead of:

```text
/command
/health
```

After correcting the base URL, the server responded correctly.

## 7. Timeout Handling

Timeouts were added to the Android communication functions to make debugging easier and prevent the app from waiting indefinitely.

The app uses:

```kotlin
connection.connectTimeout = 5000
connection.readTimeout = 5000
```

The connection timeout limits how long the app waits while trying to reach the server. The read timeout limits how long the app waits for the server to respond after the connection has been established.

This means that if the server is not running, the IP address is wrong, or the devices are not on the same network, the app will stop waiting after 5 seconds and display an error message.

This improves reliability and makes connection problems easier to detect.

## 8. Current Prototype Status

At the current stage, the Android app can:

* display a styled control interface
* send command strings to the Python relay server
* send JSON data using HTTP POST
* test server connectivity using a separate health check endpoint
* display success, error, timeout, or connection messages on the screen
* confirm received commands through the Python terminal

The current system is therefore:

```text
Android App
    ↓ HTTP POST /command
Python Relay Server
    ↓
Terminal output confirming received command
```

and for debugging:

```text
Android App
    ↓ HTTP GET /health
Python Relay Server
    ↓
Connection status returned to app
```

## 9. Next Steps

The next development step is to connect the Python relay server to the actual Unitree Go2 robot control layer. This requires identifying the correct robot communication method, such as ROS2 or WebRTC.

After the relay can trigger real robot actions, the system can be expanded toward the final goal: replacing button input with gesture input from a wearable device or Meta Quest 3.

Possible next steps include:

* mapping each app command to an actual robot action
* testing communication with the Go2 robot
* investigating ROS2 or WebRTC control options
* integrating smartwatch gesture data
* defining gesture-to-command mappings
* improving feedback from the robot to the app
* optionally adding camera or robot state information to the app
