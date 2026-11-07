#!/usr/bin/env python3
"""Answers RetroArch's UDP commands from a RAM snapshot, to test probe_ram.py without a device.

Reply formats follow RetroArch's command.c. Modes:
  nightly   current build: answers commands and reads the RAM with no memory map
  old-map   build with commands but without the fallback: answers "no memory map defined"
  silent    official stable Android build: the port stays silent
"""
import argparse
import socket
from pathlib import Path


def reply_to(command, ram, mode):
    if command == "VERSION":
        return "1.22.2\n"
    if command == "GET_STATUS":
        return "GET_STATUS PLAYING playstation,Digimon World 2003\n"
    if command == "GET_MEMORY_MAP":
        return f"0\t{len(ram):x}\t0\t0\tsystem\trw\tlittle\tcore\n" if mode == "nightly" else None

    parts = command.split()
    if parts[:1] != ["READ_CORE_MEMORY"] or len(parts) != 3:
        return None
    address = int(parts[1], 16)
    size = int(parts[2])
    prefix = f"READ_CORE_MEMORY {address:x}"
    if mode == "old-map":
        return f"{prefix} -1 no memory map defined\n"
    if address >= len(ram):
        return f"{prefix} -1 no descriptor for address\n"
    return f"{prefix} {ram[address:address + size].hex(' ').upper()}\n"


def serve(ram, mode, port, ready=None, stop=None):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as server:
        # macOS caps datagrams at 9 KB by default; large reads need up to 49 KB.
        server.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 65535)
        server.bind(("127.0.0.1", port))
        server.settimeout(0.05)
        if ready:
            ready(server.getsockname()[1])
        while not (stop and stop.is_set()):
            try:
                data, source = server.recvfrom(4096)
            except socket.timeout:
                continue
            if mode == "silent":
                continue
            reply = reply_to(data.decode("ascii").strip(), ram, mode)
            if reply:
                server.sendto(reply.encode("ascii"), source)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("snapshot", type=Path)
    parser.add_argument("--mode", choices=("nightly", "old-map", "silent"), default="nightly")
    parser.add_argument("--port", type=int, default=55355)
    args = parser.parse_args()
    serve(args.snapshot.read_bytes(), args.mode, args.port)
