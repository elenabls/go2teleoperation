from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class CommandHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            print("Health check received")
            
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status": "ok", "message": "server is running"}')
        else:
            self.send_response(404)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error": "not found"}')
        
        
    def do_POST(self):
        if self.path == "/command":
            content_length = int(self.headers["Content-Length"])
            body = self.rfile.read(content_length)

            data = json.loads(body)
            command = data.get("command")

            print(f"Received command: {command}")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            response = {"status": "ok", "command": command}
            self.wfile.write(json.dumps(response).encode())

        else:
            self.send_response(404)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error": "not found"}')


server = HTTPServer(("0.0.0.0", 8000), CommandHandler)
print("Relay running on port 8000...")
server.serve_forever()
