import paho.mqtt.client as mqtt

BROKER_HOST = "localhost"
BROKER_PORT = 1883
TOPIC = "#"

GESTURE_TO_COMMAND = {
    "gesture_1": "stand",
    "gesture_2": "lie_down",
    "gesture_3": "hello",
    "gesture_4": "sit",
    "gesture_5": "heart",
}

COMMAND_MAP = {
    "stand": "1001",
    "lie_down": "1002",
    "hello": "1003",
    "sit": "1004",
    "heart": "1005",
}

last_gesture = None

def send_to_go2(command_id):
    print("Pretending to send command to Go2:", command_id)

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT broker.")
        client.subscribe(TOPIC)
        print("Listening on topic:", TOPIC)
    else:
        print("Connection failed with code:", rc)

def on_message(client, userdata, message):
    global last_gesture

    gesture = message.payload.decode("utf-8", errors="ignore").strip()

    print("Topic:", message.topic)
    print("Received gesture:", gesture)

    if gesture == last_gesture:
        print("Duplicate gesture ignored:", gesture)
        print("-" * 40)
        return

    last_gesture = gesture

    if gesture in GESTURE_TO_COMMAND:
        command_name = GESTURE_TO_COMMAND[gesture]
        command_id = COMMAND_MAP[command_name]

        print("Mapped to Go2 command:", command_name)
        print("Command ID:", command_id)

        send_to_go2(command_id)

    else:
        print("Unknown gesture. No command sent.")

    print("-" * 40)

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect(BROKER_HOST, BROKER_PORT, 60)
client.loop_forever()