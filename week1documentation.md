# Week 1 Prototype Documentation: Android-Based Teleoperation Interface for Unitree Go2

## 1. Project Goal

The final goal of the project is to teleoperate the Unitree Go2 robot using physical human motion. Initially, the intended input device was the Meta Quest 3, which would be used to read motion signals from the user. Another possible input device is a wearable, such as a smartwatch, which could collect hand-motion or gesture data.

The long-term system would therefore follow this idea:

```text
Human motion / gesture input
        ↓
Wearable device / Meta Quest 3
        ↓
Input processing / command mapping
        ↓
Communication layer
        ↓
Robot control interface
        ↓
Unitree Go2
```

For the first sprint, the goal was reduced to a smaller and testable prototype: sending basic robot commands from an Android application. Instead of using physical gestures immediately, the user presses buttons in the app, and the selected command is sent to a relay server.

The purpose of this prototype is to verify the communication chain step by step before connecting the system to the actual robot control layer.

---

## 2. Week 1 System Architecture

The Week 1 prototype is composed of four main parts:

```text
Android App
    ↓ HTTP
Communication Bridge / Relay Server
    ↓ ZeroMQ
Robot Receiver / Control Interface
    ↓
Unitree Go2
```

During local testing, the real robot receiver was replaced by a fake receiver running on the laptop:

```text
Android App
    ↓ HTTP POST /command
Python Relay Server
    ↓ ZeroMQ
Fake Robot Receiver
```

This allowed the communication pipeline to be tested safely without sending commands to the physical robot.

---

## 3. Android App

The Android app acts as the user interface. It was built using Android Studio, Kotlin, and Jetpack Compose. The user interface is written directly in Kotlin code instead of XML layout files.

The main application code is located in:

```text
app/src/main/java/com/example/go2controller/MainActivity.kt
```

The Android SDK is installed at:

```text
C:\Android\Sdk
```

The project directory is:

```text
C:\elena\AndroidStudioProjects\Go2Controller
```

The app sends HTTP requests to the relay server using the laptop IP address and port `8000`:

```kotlin
private const val SERVER_BASE_URL = "http://192.168.1.119:8000"
private const val COMMAND_URL = "$SERVER_BASE_URL/command"
private const val HEALTH_URL = "$SERVER_BASE_URL/health"
```

The phone does not communicate directly with the robot. It only communicates with the laptop running the relay server. The relay then forwards the command to the robot-side receiver.

---

## 4. Cleaned Android Interface

The initial app interface included movement-style buttons such as forward, left, right, back, stop, sit, and stand. However, after connecting the app to the available robot command receiver, the interface was cleaned so that it only contains commands currently supported by the relay.

The current supported commands are:

```text
stand
lie_down
hello
sit
heart
```

The displayed button labels are more user-friendly:

```text
Stand up  → stand
Lie down  → lie_down
Hello     → hello
Sit       → sit
Heart     → heart
```

This keeps the Android side readable, while the relay is responsible for translating these high-level command names into the command codes expected by the robot receiver.

The current app includes:

- a title: `✦ Unitree Go2 Controller ✦`
- a status display box
- five supported command buttons
- a `Test Connection` button
- timeout handling for connection errors

The current main color choices are:

```text
Background: dark blue #064275
Status box: green #98D278
Command buttons: pink #FFCAE9
Test button: cyan #5CE7FF
```

---

## 5. Android-to-Relay Communication

The app communicates with the relay using HTTP.

There are two endpoints:

```text
POST /command
GET  /health
```

### 5.1 Command Endpoint

The `/command` endpoint is used to send commands.

When the user presses a button, the app creates a JSON message such as:

```json
{"command": "sit"}
```

The message is sent as an HTTP POST request to:

```text
http://<laptop-ip>:8000/command
```

For example, during testing the laptop IP was:

```text
192.168.1.119
```

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
Relay maps the command to a robot command code
    ↓
Relay forwards the code through ZeroMQ
```

### 5.2 Health Check Endpoint

The `/health` endpoint is used for debugging. It checks whether the relay server is reachable without sending a robot command.

The Test button sends a GET request to:

```text
http://<laptop-ip>:8000/health
```

If the relay server is reachable, the app displays:

```text
Server connected
```

This is useful because it separates basic network testing from robot command testing.

---

## 6. Relay Server

The relay server is written in Python. It receives HTTP requests from the Android app and forwards commands to the robot-side receiver using ZeroMQ.

The relay has two responsibilities:

```text
1. Receive high-level command names from the Android app.
2. Translate those names into robot command codes and send them through ZeroMQ.
```

The relay maps commands as follows:

```python
COMMAND_MAP = {
    "stand": "1001",
    "lie_down": "1002",
    "hello": "1003",
    "sit": "1004",
    "heart": "1005",
}
```

The relay receives commands from the phone on port `8000` and forwards them to a ZeroMQ receiver on port `5555`.

For local testing, the receiver IP was set to:

```python
RECEIVER_IP = "127.0.0.1"
PORT = 5555
```

For real robot testing, this IP should be changed to the robot-side receiver IP, for example:

```python
RECEIVER_IP = "192.168.10.6"
PORT = 5555
```

The Android app should still point to the laptop IP. Only the relay server should point to the robot receiver IP.

---

## 7. ZeroMQ Communication

The existing robot command script provided by the supervisor uses ZeroMQ. It sends command codes such as `1001`, `1002`, `1003`, `1004`, and `1005` to a receiver running on another machine.

The original keyboard-based workflow was:

```text
Keyboard input
    ↓
Python ZMQ client
    ↓
Receiver at 192.168.10.x:5555
    ↓
Robot command execution
```

The updated workflow replaces keyboard input with the Android app:

```text
Android app button
    ↓ HTTP POST
Python relay
    ↓ ZMQ command code
Robot receiver
    ↓
Unitree Go2
```

The ZeroMQ socket uses a request-reply pattern:

```text
REQ socket sends one command
    ↓
REP socket receives command
    ↓
REP socket sends confirmation
    ↓
REQ socket receives confirmation
```

This means the relay sends a command and then waits for a confirmation message before responding to the Android app.

---

## 8. Fake Receiver for Safe Local Testing

To avoid interfering with the real robot, a fake receiver was created and run locally on the laptop. This fake receiver listens on:

```text
127.0.0.1:5555
```

The fake receiver acts like the robot-side receiver. It receives command codes and sends back a fake confirmation.

Example output from the fake receiver:

```text
Fake robot receiver running on 127.0.0.1:5555
Fake robot received command: 1004
```

This confirmed that the app, relay, command mapping, and ZeroMQ forwarding all work before using the physical robot.

The local test chain was:

```text
Android app
    ↓
HTTP relay on laptop
    ↓
Command mapping: sit → 1004
    ↓
ZeroMQ message
    ↓
Fake receiver
    ↓
Confirmation returned to relay and app
```

All five supported commands were successfully tested locally:

```text
stand      → 1001
lie_down   → 1002
hello      → 1003
sit        → 1004
heart      → 1005
```

---

## 9. Timeout and Error Handling

Timeouts were added to the Android communication functions to prevent the app from waiting indefinitely.

The app uses:

```kotlin
connection.connectTimeout = 5000
connection.readTimeout = 5000
```

The connection timeout limits how long the app waits while trying to reach the server. The read timeout limits how long the app waits for the server to respond after a connection has been established.

The relay also uses ZeroMQ timeouts so that it does not wait forever if the receiver does not answer. If the receiver is not running or unreachable, the app may display an error such as:

```text
Server error 500: {"status": "error", "message": "resource temporarily unavailable"}
```

This means the Android app reached the relay, but the relay did not receive a reply from the ZeroMQ receiver.

---

## 10. Network Setup and IP Address Notes

A key debugging point was that the phone, laptop, and robot receiver may be on different networks.

During local app testing, the laptop was connected to Wi-Fi with:

```text
Laptop Wi-Fi IP: 192.168.1.119
```

The Android app used this IP to reach the relay:

```text
http://192.168.1.119:8000
```

However, the robot-side receiver is on a different network, using an address such as:

```text
192.168.10.x
```

Therefore, the laptop must be able to reach both the phone network and the robot receiver network.

A useful setup is:

```text
Phone → Laptop over Wi-Fi
Laptop → Robot receiver over Ethernet or robot network
```

In this setup:

```text
Android app URL = laptop IP on the phone-accessible network
Relay receiver IP = robot receiver IP on the robot network
```

So the Android app should not be changed to the robot IP. The phone does not connect to the robot directly.

---

## 11. Current Prototype Status

At the current stage, the prototype can:

- display a cleaned Android command interface
- test server connectivity using `/health`
- send command strings using HTTP POST
- receive commands in a Python relay server
- map high-level app commands to robot command codes
- forward command codes through ZeroMQ
- receive confirmations from a fake receiver
- safely test the full communication path locally

The confirmed local system is:

```text
Android App
    ↓ HTTP POST /command
Python Relay Server
    ↓ Command mapping
ZeroMQ REQ socket
    ↓
Fake Robot Receiver
    ↓
Confirmation returned to app
```

This confirms that the Android app is no longer just a user interface. It is connected to a working communication bridge.

---

## 12. Real Robot Test Plan

Once the robot is free and the network is correctly configured, the real receiver can be used instead of the fake receiver.

The real test procedure is:

```text
1. Stop the fake receiver.
2. Change the relay receiver IP from 127.0.0.1 to the real receiver IP.
3. Keep the Android app pointing to the laptop IP.
4. Start the relay server.
5. Press Test Connection in the Android app.
6. Send one safe command first, preferably Hello.
7. Wait for the robot response before sending another command.
```

For example:

```python
RECEIVER_IP = "192.168.10.6"
PORT = 5555
```

The expected real command chain is:

```text
Android app
    ↓
Laptop relay
    ↓
ZeroMQ command code
    ↓
Robot receiver
    ↓
Unitree Go2
```

Commands should be tested one at a time, with pauses between them, to avoid unexpected robot behavior.

---

## 13. Next Steps

The next development steps are:

- complete a controlled real-robot test using the current Android-to-ZMQ pipeline
- confirm which command codes are available for additional actions
- add movement commands only after their corresponding robot codes are known
- improve robot-side feedback to the Android app
- investigate ROS2 as the long-term robot communication architecture
- compare how smartwatch and Meta Quest input data could be converted into commands
- define gesture-to-command mappings
- eventually replace button input with wearable or VR-based motion input

For now, the most important milestone has been achieved: the Android app can send commands through the relay and reach a ZeroMQ receiver safely during local testing.
