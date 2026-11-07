#!/usr/bin/env python3
"""Checks that RetroArch can serve Digimon World 2003 RAM over its UDP network commands.

Usage: python3 probe_ram.py [host] [--port 55355] [--samples 40]
Exit codes: 0 readable, 2 no answer, 3 read refused, 4 data does not look like the game.
"""
import argparse
import socket
import statistics
import struct
import sys
import time

BITS = 0x48DA0
MAP_ID = 0x4B3F8
PREVIOUS_MAP_ID = 0x4B400
PARTY_SLOTS = (0x48DA4, 0x48DA8, 0x48DAC)
EMPTY_SLOT = 0xFF
RAM_SIZE = 0x200000

DIGIMON_BLOCKS = (
    ("Kotemon", 0x4949C),
    ("Kumamon", 0x49878),
    ("Monmon", 0x49C54),
    ("Agumon", 0x4A030),
    ("Veemon", 0x4A40C),
    ("Guilmon", 0x4A7E8),
    ("Renamon", 0x4ABC4),
    ("Patamon", 0x4AFA0),
)
DIGIMON_BLOCK_SIZE = 0x3DC
LEVEL_OFFSET = 0x1C
HP_OFFSET = 0x20
MP_OFFSET = 0x24

NO_REPLY_HELP = """Sin respuesta. Causas posibles:
  1. Comandos de red desactivados: Ajustes > Red > Comandos de red, y reinicia RetroArch.
  2. RetroArch estable: las versiones de Android hasta 1.22.2 no incluyen estos comandos.
     Hace falta una nightly del 25-10-2026 o posterior.
  3. IP o puerto incorrectos, o Wi-Fi con aislamiento de clientes."""

REFUSED_HELP = """RetroArch responde, pero no entrega la RAM. Comprueba:
  1. Que el juego está cargado y en marcha, no solo el menú de RetroArch.
  2. Que usas SwanStation.
  3. Que la nightly es del 25-10-2026 o posterior: las anteriores no leen núcleos sin mapa de memoria."""


class ReadRefused(Exception):
    pass


class ReadLost(Exception):
    pass


class Probe:
    def __init__(self, host, port, timeout):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.connect((host, port))
        self.socket.settimeout(timeout)

    def ask(self, command, expected_prefix=""):
        """Sends one command and returns its reply, skipping late replies to earlier requests."""
        deadline = time.monotonic() + self.socket.gettimeout()
        try:
            self.socket.send(command.encode("ascii"))
        except ConnectionRefusedError:
            return None
        while True:
            try:
                reply = self.socket.recv(65535).decode("ascii", "replace").strip()
            except (socket.timeout, ConnectionRefusedError):
                return None
            if reply.startswith(expected_prefix):
                return reply
            if time.monotonic() >= deadline:
                return None

    def read(self, address, size):
        """Returns the bytes at a RAM offset."""
        prefix = f"READ_CORE_MEMORY {address:x} "
        reply = self.ask(f"READ_CORE_MEMORY {address:X} {size}", prefix)
        if reply is None:
            raise ReadLost()
        first_line = reply.splitlines()[0]
        fields = first_line[len(prefix):].split()
        if fields[:1] == ["-1"]:
            raise ReadRefused(" ".join(fields[1:]))
        data = bytes.fromhex("".join(fields))
        if len(data) != size:
            raise ReadRefused(f"se pidieron {size} bytes y llegaron {len(data)}")
        return data


def percentile(values, fraction):
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(len(ordered) * fraction))]


def measure(probe, label, address, size, samples):
    """Prints the round-trip time of a read, as the app would repeat it while playing."""
    times = []
    for _ in range(samples):
        started = time.perf_counter()
        try:
            probe.read(address, size)
        except ReadLost:
            continue
        times.append((time.perf_counter() - started) * 1000)
    lost = samples - len(times)
    if not times:
        print(f"  {label:<30} todas perdidas ({samples})")
        return False
    print(
        f"  {label:<30} mediana {statistics.median(times):5.1f} ms"
        f"   p95 {percentile(times, 0.95):5.1f} ms   perdidas {lost}/{samples}"
    )
    return lost == 0


def read_game_state(probe):
    """Reads what the player can compare on screen. Returns (fields, problems)."""
    bits = struct.unpack("<i", probe.read(BITS, 4))[0]
    map_id = struct.unpack("<h", probe.read(MAP_ID, 2))[0]
    previous_map_id = struct.unpack("<h", probe.read(PREVIOUS_MAP_ID, 2))[0]
    slots = [probe.read(address, 1)[0] for address in PARTY_SLOTS]

    problems = []
    if bits < 0:
        problems.append(f"Bits negativos ({bits})")
    if map_id <= 0:
        problems.append(f"MapId no válido ({map_id})")

    members = []
    for slot in slots:
        if slot == EMPTY_SLOT:
            continue
        if slot >= len(DIGIMON_BLOCKS):
            problems.append(f"Hueco de equipo no válido ({slot:#x})")
            continue
        name, block = DIGIMON_BLOCKS[slot]
        level = struct.unpack("<h", probe.read(block + LEVEL_OFFSET, 2))[0]
        hp, hp_max = struct.unpack("<hh", probe.read(block + HP_OFFSET, 4))
        mp, mp_max = struct.unpack("<hh", probe.read(block + MP_OFFSET, 4))
        members.append((name, level, hp, hp_max, mp, mp_max))
        if not 1 <= level <= 99:
            problems.append(f"{name}: nivel no válido ({level})")
        if hp_max <= 0 or not 0 <= hp <= hp_max:
            problems.append(f"{name}: HP no válidos ({hp}/{hp_max})")
        if mp_max < 0 or not 0 <= mp <= mp_max:
            problems.append(f"{name}: MP no válidos ({mp}/{mp_max})")
    if not members:
        problems.append("Equipo vacío")

    fields = {
        "bits": bits,
        "map_id": map_id,
        "previous_map_id": previous_map_id,
        "members": members,
    }
    return fields, problems


def run(probe, args):
    print(f"Destino: {args.host}:{args.port}\n")

    version = probe.ask("VERSION")
    if version is None:
        print(NO_REPLY_HELP)
        return 2
    print(f"RetroArch:  {version}")
    print(f"Estado:     {probe.ask('GET_STATUS', 'GET_STATUS') or 'sin respuesta'}")
    memory_map = probe.ask("GET_MEMORY_MAP")
    print("Mapa:       " + (memory_map.replace("\t", " ").replace("\n", " | ") if memory_map else "sin respuesta"))

    try:
        probe.read(BITS, 4)
    except ReadLost:
        print("\nVERSION responde pero READ_CORE_MEMORY no.\n" + REFUSED_HELP)
        return 3
    except ReadRefused as refused:
        print(f"\nLectura rechazada: {refused}\n{REFUSED_HELP}")
        return 3

    try:
        fields, problems = read_game_state(probe)
        probe.read(RAM_SIZE - 4, 4)
    except ReadLost:
        print("\nSe perdió una respuesta durante la lectura. Repite la prueba.")
        return 2
    except ReadRefused as refused:
        print(f"\nLectura rechazada a mitad de prueba: {refused}\n{REFUSED_HELP}")
        return 3

    print("\nCompara con la pantalla del juego:")
    print(f"  Bits:          {fields['bits']}")
    print(f"  MapId:         {fields['map_id']:#x}   (anterior {fields['previous_map_id']:#x})")
    for name, level, hp, hp_max, mp, mp_max in fields["members"]:
        print(f"  {name:<14} Nv {level:<3} HP {hp}/{hp_max}   MP {mp}/{mp_max}")

    if problems:
        print("\nLos datos no parecen de Digimon World 2003 PAL (SLES-03936) en una partida:")
        for problem in problems:
            print(f"  - {problem}")
        print("Carga una partida y vuelve a probar. Si sigue igual, comprueba la versión del juego.")
        return 4

    print(f"\nVelocidad ({args.samples} lecturas de cada):")
    single_ok = measure(probe, "Valor suelto (4 bytes)", BITS, 4, args.samples)
    block_ok = measure(probe, "Un Digimon (988 bytes)", DIGIMON_BLOCKS[0][1], DIGIMON_BLOCK_SIZE, args.samples)
    measure(probe, "Los ocho (7904 bytes)", DIGIMON_BLOCKS[0][1], DIGIMON_BLOCK_SIZE * 8, args.samples)

    print("\nOK: RetroArch entrega la RAM. Comprueba que los valores coinciden con la pantalla.")
    if not (single_ok and block_ok):
        print("Aviso: se perdieron lecturas pequeñas. Anota la red usada; la app trabajará en la misma consola.")
    return 0

def main():
    parser = argparse.ArgumentParser(description="Comprueba la lectura de RAM en RetroArch.")
    parser.add_argument("host", nargs="?", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=55355)
    parser.add_argument("--timeout", type=float, default=1.0)
    parser.add_argument("--samples", type=int, default=40)
    args = parser.parse_args()

    probe = Probe(args.host, args.port, args.timeout)
    try:
        return run(probe, args)
    finally:
        probe.socket.close()

if __name__ == "__main__":
    sys.exit(main())
