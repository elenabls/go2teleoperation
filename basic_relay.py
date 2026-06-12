from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import zmq


RECEIVER_IP = "192.168.1.128" #change this to the actual IP address of the Go2 receiver: 192.168.10.6
PORT = 5555

COMMAND_MAP = {
    "stand": "1001",
    "lie_down": "1002",
    "hello": "1003",
    "sit": "1004",
    "heart": "1005",
} 


context = zmq.Context()


def send_robot_command(command_name):
    if command_name not in COMMAND_MAP:
        return {
            "status": "error",
            "message": f"Unknown command: {command_name}"
        }

    robot_code = COMMAND_MAP[command_name]

    socket = context.socket(zmq.REQ)
    socket.setsockopt(zmq.LINGER, 0)

    # Timeout so it does not hang forever
    socket.setsockopt(zmq.RCVTIMEO, 3000)
    socket.setsockopt(zmq.SNDTIMEO, 3000)

    try:
        socket.connect(f"tcp://{RECEIVER_IP}:{PORT}")

        print(f"Sending robot code: {robot_code} for command: {command_name}")

        socket.send_string(robot_code)
        reply = socket.recv_string()

        return {
            "status": "ok",
            "command": command_name,
            "robot_code": robot_code,
            "robot_reply": reply
        }

    except Exception as e:
        return {
            "status": "error",
            "message": str(e)
        }

    finally:
        socket.close()


class CommandHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()

            response = {
                "status": "ok",
                "message": "server is running"
            }

            self.wfile.write(json.dumps(response).encode())

        else:
            self.send_response(404)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error": "not found"}')

    def do_POST(self):
        if self.path == "/command":
            try:
                content_length = int(self.headers["Content-Length"])
                body = self.rfile.read(content_length)

                data = json.loads(body)
                command = data.get("command")

                print(f"Received command from Android/PowerShell: {command}")

                result = send_robot_command(command)

                if result["status"] == "ok":
                    self.send_response(200)
                else:
                    self.send_response(500)

                self.send_header("Content-Type", "application/json")
                self.end_headers()

                self.wfile.write(json.dumps(result).encode())

            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json")
                self.end_headers()

                response = {
                    "status": "error",
                    "message": str(e)
                }

                self.wfile.write(json.dumps(response).encode())

        else:
            self.send_response(404)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error": "not found"}')


server = HTTPServer(("0.0.0.0", 8000), CommandHandler)
print("HTTP-to-ZMQ relay running on port 8000...")
server.serve_forever()