import json
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


def extract_gesture(payload):
    payload = payload.strip()

    #for plain text message
    if not payload.startswith("{"):
        return payload

    #for JSON message
    try:
        data = json.loads(payload)
    except json.JSONDecodeError:
        return payload

    possible_keys = ["gesture", "label", "motion", "class", "prediction", "result"]

    for key in possible_keys:
        if key in data:
            return str(data[key]).strip()

    return payload


def send_to_go2(command_id):
    """
    Temporary fake Go2 sender.
    Later, this function will be replaced with the real Go2 command interface.
    """
    print("Pretending to send command to Go2:", command_id)


def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT broker.")
        client.subscribe(TOPIC)
        print("Listening on topic:", TOPIC)
        print("-" * 50)
    else:
        print("Connection failed with code:", rc)


def on_message(client, userdata, message):
    global last_gesture

    raw_payload = message.payload.decode("utf-8", errors="ignore")
    gesture = extract_gesture(raw_payload)

    print("Topic:", message.topic)
    print("Raw payload:", raw_payload)
    print("Detected gesture:", gesture)

    if gesture == last_gesture:
        print("Duplicate gesture ignored:", gesture)
        print("-" * 50)
        return

    last_gesture = gesture

    if gesture in GESTURE_TO_COMMAND:
        command_name = GESTURE_TO_COMMAND[gesture]
        command_id = COMMAND_MAP[command_name]

        print("Mapped Go2 action:", command_name)
        print("Command ID:", command_id)

        send_to_go2(command_id)
    else:
        print("Unknown gesture. No Go2 command mapped yet.")

    print("-" * 50)


client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

print("Connecting to MQTT broker...")
client.connect(BROKER_HOST, BROKER_PORT, 60)
client.loop_forever()