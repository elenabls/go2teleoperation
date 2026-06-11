import zmq

PORT = 5555

context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind(f"tcp://127.0.0.1:{PORT}")

print(f"Fake robot receiver running on 127.0.0.1:{PORT}")

while True:
    command = socket.recv_string()
    print(f"Fake robot received command: {command}")

    reply = f"fake confirmation for command {command}"
    socket.send_string(reply)