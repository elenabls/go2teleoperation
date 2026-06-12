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
MONDAY
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
TUESDAY
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
WEDNESDAY
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
THURSDAY
---

## 13. Wearable / MoRSE MQTT Investigation

In parallel with the Android-based prototype, the wearable-control direction was investigated. The long-term idea is to use smartwatch gestures as an alternative input method for controlling the Unitree Go2.

After discussing with the researcher responsible for the wearable system, the expected architecture was clarified as follows:

```text
Smartwatch gesture
    ↓
MoRSE application
    ↓ MQTT
MQTT broker
    ↓
Python gesture-to-command translator
    ↓
Go2 command interface
    ↓
Unitree Go2
```

The smartwatch/MoRSE system is expected to send MQTT messages. Therefore, the smartwatch does not need to communicate directly with the robot. Instead, an MQTT broker receives the gesture messages, and a Python program subscribes to these messages and translates them into Go2 commands.

The first wearable prototype will not attempt continuous robot teleoperation yet. Instead, the recognized gestures will be treated as high-level triggers, similar to pressing buttons in the Android app.

The current Go2 action commands are:

```python
COMMAND_MAP = {
    "stand": "1001",
    "lie_down": "1002",
    "hello": "1003",
    "sit": "1004",
    "heart": "1005",
}
```

A possible temporary gesture mapping is:

```python
GESTURE_TO_COMMAND = {
    "gesture_1": "stand",
    "gesture_2": "lie_down",
    "gesture_3": "hello",
    "gesture_4": "sit",
    "gesture_5": "heart",
}
```

This means the wearable input can initially replace the Android buttons:

```text
gesture_1 → stand     → 1001
gesture_2 → lie_down  → 1002
gesture_3 → hello     → 1003
gesture_4 → sit       → 1004
gesture_5 → heart     → 1005
```

The original MoRSE gestures were designed for first-responder communication rather than robot teleoperation. Therefore, their original meaning does not have to be preserved. For this prototype, each recognized gesture can simply act as an input trigger for one of the available robot actions.

---

## 14. Local MQTT Broker Setup

Since the Jetson Nano was not available yet, the MQTT communication layer was first tested locally.

A Mosquitto MQTT broker was set up using Docker. The broker configuration file was created with:

```conf
listener 1883
allow_anonymous true
```

This allows MQTT clients to connect to the broker on port `1883` without requiring a username or password.

The broker was started using Docker:

```bash
docker run -it --name mosquitto-test -p 1883:1883 -v ~/mosquitto/config/mosquitto.conf:/mosquitto/config/mosquitto.conf eclipse-mosquitto
```

The broker was tested locally using MQTT publish/subscribe commands.

Subscriber:

```bash
mosquitto_sub -h localhost -p 1883 -t "#" -v
```

Publisher:

```bash
mosquitto_pub -h localhost -p 1883 -t "morse/gesture" -m "gesture_1"
```

The subscriber successfully received:

```text
morse/gesture gesture_1
```

This confirmed that the MQTT broker was working locally.

---

## 15. Python MQTT-to-Go2 Command Translator

A Python script was created to subscribe to MQTT messages and translate received gesture labels into Go2 command IDs.

The script subscribes to all MQTT topics using:

```python
TOPIC = "#"
```

This is useful because the real MoRSE topic is not known yet. By listening to all topics, the script can print any message that arrives from the smartwatch/MoRSE system.

The script prints:

```text
Topic
Raw payload
Detected gesture
Mapped Go2 action
Command ID
```

The script was also updated to support both plain-text payloads and JSON-style payloads.

Supported plain-text example:

```text
gesture_1
```

Supported JSON examples:

```json
{"gesture": "gesture_1"}
```

```json
{"label": "gesture_1"}
```

```json
{"motion": "gesture_1"}
```

This makes the script more flexible, since the exact payload format used by MoRSE is not yet confirmed.

The current local test flow is:

```text
Fake gesture message
    ↓
Mosquitto MQTT broker
    ↓
Python MQTT listener
    ↓
Gesture extraction
    ↓
Gesture-to-command mapping
    ↓
Go2 command ID
```

Example result:

```text
Topic: morse/gesture
Raw payload: gesture_1
Detected gesture: gesture_1
Mapped Go2 action: stand
Command ID: 1001
Pretending to send command to Go2: 1001
```

This confirms that the MQTT-to-command translation logic works locally.

---

## 16. Ubuntu Setup and Testing

An Ubuntu system was prepared for the MQTT and robot-control work.

The following tools were installed or checked:

```text
Docker
Mosquitto MQTT clients
Python 3
paho-mqtt
colcon
Git
VS Code / development tools
```

Docker was tested successfully using:

```bash
docker run hello-world
```

The MQTT broker was then started in Docker and tested using local MQTT messages.

The Python MQTT listener was also tested successfully. Fake gesture messages were published locally, and the Python script correctly received and mapped them to Go2 command IDs.

Tested examples:

```text
gesture_1              → stand     → 1001
{"gesture": "gesture_2"} → lie_down  → 1002
```

Unknown gestures are printed but not mapped to Go2 commands yet. This is intentional, because the real gesture labels from MoRSE still need to be observed.

Example:

```text
Raw payload: {"gesture": "distress"}
Detected gesture: distress
Unknown gesture. No Go2 command mapped yet.
```

Once the real MoRSE payload is known, the gesture mapping dictionary can be updated.

---

## 17. Current Wearable Testing Limitation

The actual smartwatch/MoRSE connection could not be fully tested yet because the available Ubuntu PC was connected through Ethernet and did not have Wi-Fi access.

The smartwatch needs to reach the MQTT broker over a reachable local network. If the smartwatch is connected over Wi-Fi and the PC is connected over Ethernet, the two devices may not be able to communicate, depending on the lab network configuration.

The current limitation is therefore not the MQTT broker or Python script, but the network path between the smartwatch and the broker.

Current confirmed status:

```text
Mosquitto broker works locally        ✅
MQTT publish/subscribe works locally  ✅
Python listener receives messages     ✅
Gesture-to-command mapping works      ✅
Real MoRSE smartwatch connection      not tested yet
```

The real smartwatch test should be repeated once the MQTT broker is running on a device reachable by the smartwatch, such as:

```text
Jetson Nano on the same network
Ubuntu PC with Wi-Fi
Laptop connected to the same Wi-Fi
Dedicated router / hotspot setup
```

---
FRIDAY
---

Today I continued working on the smartwatch-to-robot command pipeline. I tried connecting to the smartwatch again, but I still could not read the messages sent by it. To isolate the issue, I tested each part of the pipeline individually. I also tested the same MQTT communication flow using my phone instead of the smartwatch, and that worked correctly. Based on this, the issue seems to be specifically in the smartwatch → MQTT broker part of the pipeline.

At the moment, I still do not know why the smartwatch messages are not reaching the broker correctly. I contacted Chris and asked him to let me know when he is in the office, so he can help me check the smartwatch setup.

Since continuing to debug the smartwatch without access to additional help was becoming inefficient, I decided to use the rest of the day to improve the Android app. The goal was to make the app more useful as a manual testing interface for the currently supported robot commands.

The app was updated with the following improvements:

* Added a command history box to keep track of the commands pressed during testing.
* Improved the status display.
* Improved the displayed error messages so that relay/receiver errors are easier to understand.
* Added an editable Server URL input field, so the relay IP address can be changed directly from the Android device without modifying the code.
* Rearranged the command buttons to make better use of the available screen space.
* Added support for a more dashboard-like layout, especially in landscape mode.
* Added a Camera / Monitor Feed placeholder for future video integration.

The app currently supports the following command mapping:

| Action   | Command Code |
| -------- | ------------ |
| stand    | 1001         |
| lie_down | 1002         |
| hello    | 1003         |
| sit      | 1004         |
| heart    | 1005         |

I also started looking into possible ways of adding a camera display from the robot to the app. Two possible communication paths are being considered for this stage:

1. **ROS2-based integration:**
   Android app → HTTP relay → ROS2 node → Unitree Go2

2. **WebRTC-based integration:**
   Android app → HTTP relay → WebRTC interface → Unitree Go2

Since the robot is a Unitree Go2 EDU and is expected to communicate over Wi-Fi, WebRTC is especially relevant. However, ROS2 remains attractive because it is a standard robotics framework and would make the system easier to extend later with topics, nodes, sensor data, and logging.

I also found a video showing a teleoperation architecture using both ROS2 and WebRTC:
https://youtu.be/Jun333B-k2c?is=M1iyrwFQpXxk1yXy

### ROS2 Route

Architecture:

```text
Android app → HTTP relay → ROS2 node → Unitree Go2
```

Advantages:

* Standard robotics framework.
* Easier to expand using topics and nodes.
* Local support is available in the lab.
* Easier to document and integrate into a robotics project.

Disadvantages:

* The available Go2 ROS2 topics still need to be checked once the robot is available.
* The camera feed may require an additional bridge or custom message handling.

### WebRTC Route

Architecture:

```text
Android app → HTTP relay → WebRTC Go2 interface → Unitree Go2
```

Advantages:

* Relevant for Go2 communication over Wi-Fi.
* Likely useful for live video and low-latency control.
* May be closer to how the official mobile interface communicates with the robot.

Disadvantages:

* Harder to understand and implement.
* Less local support is available.
* Direct WebRTC integration inside the Android app would make the app more complex.

For now, I think the most suitable direction is to focus on a ROS2-based integration. The Android app can already send HTTP commands to the Python relay, so the relay can later be extended to communicate with a ROS2 node. This keeps the Android app independent from the robot-specific communication layer.

WebRTC is still relevant, especially because the Go2 communicates over Wi-Fi and the live video stream may rely on WebRTC. However, instead of implementing WebRTC directly inside the Android app at this stage, the preferred architecture is to keep ROS2/WebRTC communication on the backend side and expose a simpler HTTP-based interface to the Android app.


## 18. Updated Next Steps

The next development steps are:

* continue using the Android app as the manual testing interface for the currently supported Go2 commands;
* keep the editable Server URL field so the relay address can be changed directly from the Android device;
* use the command history and status messages to debug whether commands are reaching the relay correctly;
* continue investigating the smartwatch connection issue, especially the smartwatch → MQTT broker part;
* once the smartwatch is available again, subscribe to all MQTT topics using `#` and inspect the real topic and payload format;
* update the Python MQTT parser based on the real MoRSE message format;
* map the real MoRSE gesture labels to the existing Go2 command set;
* test one safe command first, preferably `hello`;
* use ROS2 as the main direction for the next robot integration stage;
* check the available Go2 ROS2 topics once the robot is available;
* investigate whether the camera feed is exposed through ROS2 topics, WebRTC, or another Unitree interface;
* keep WebRTC as a possible supporting layer, especially for Wi-Fi communication and video streaming;
* avoid implementing WebRTC directly inside the Android app unless it becomes necessary.

The current practical result is that both input directions are now partially prepared.

Android manual control path:

```text
Android button input
    ↓
HTTP relay
    ↓
ZeroMQ command interface / future ROS2 interface
    ↓
Go2 command code
```

Smartwatch gesture control path:

```text
MQTT gesture input
    ↓
Python MQTT listener
    ↓
Gesture-to-command mapping
    ↓
Go2 command code
```

The Android app has also been improved into a more complete testing dashboard. It now includes an editable Server URL field, a command history box, clearer status messages, friendlier error messages, and a Camera / Monitor Feed placeholder for future video integration.

The next major integration step is to connect the relay to the real robot communication layer. For now, the preferred direction is:

```text
Android app / smartwatch input
    ↓
Python relay
    ↓
ROS2 node
    ↓
Unitree Go2 EDU
```

WebRTC is still considered relevant because the Go2 EDU is expected to communicate over Wi-Fi and the camera stream may rely on WebRTC. However, the current plan is to keep ROS2/WebRTC communication on the backend side and expose a simpler interface to the Android app.
