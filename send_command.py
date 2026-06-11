import zmq
import sys

try:
    import termios
except ImportError:
    termios = None


RECEIVER_IP = "192.168.10.6"
PORT = 5555


def flush_stdin():
    """Discard any buffered keystrokes so stale input cannot queue commands."""
    if termios is not None:
        termios.tcflush(sys.stdin, termios.TCIFLUSH)


COMMANDS = {
    "1": ("1001", "Stand up"),
    "2": ("1002", "Lie down"),
    "3": ("1003", "Hello (wave)"),
    "4": ("1004", "Sit"),
    "5": ("1005", "Heart"),
}


context = zmq.Context()
socket = context.socket(zmq.REQ)
socket.setsockopt(zmq.LINGER, 0)
socket.connect(f"tcp://{RECEIVER_IP}:{PORT}")

print(f"Connected to {RECEIVER_IP}:{PORT}")


while True:
    flush_stdin()

    print("\nSelect a command:")
    for key, (cmd, label) in COMMANDS.items():
        print(f" {key}) [{cmd}] {label}")

    print(" q) quit")

    choice = input("Choice: ").strip().lower()

    if choice == "q":
        break

    if choice not in COMMANDS:
        print("Invalid choice, try again.")
        continue

    command, label = COMMANDS[choice]

    print(f"Sending command: {command} ({label})")
    socket.send_string(command)

    reply = socket.recv_string()
    print(f"Confirmation received: {reply}")

    flush_stdin()


socket.close()
context.term()