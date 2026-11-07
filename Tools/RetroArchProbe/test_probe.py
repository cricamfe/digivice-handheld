#!/usr/bin/env python3
"""Runs probe_ram.py against fake_retroarch.py with every RAM snapshot in the repository.

Usage: python3 Tools/RetroArchProbe/test_probe.py
"""
import contextlib
import io
import json
import socket
import struct
import sys
import threading
import unittest
from pathlib import Path
from unittest import mock

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import fake_retroarch
import probe_ram

ROOT = HERE.parents[1]
DEFINITIONS = ROOT / "Backend/Memory/Definitions"
SNAPSHOTS = sorted((ROOT / "Tools/MemoryScanner/Snapshots").glob("*.bin"))


def run_probe(ram, mode):
    """Starts the fake server and returns the probe's exit code and output."""
    ready = threading.Event()
    stop = threading.Event()
    port = []

    def on_ready(bound_port):
        port.append(bound_port)
        ready.set()

    server = threading.Thread(target=fake_retroarch.serve, args=(ram, mode, 0, on_ready, stop))
    server.start()
    ready.wait()
    output = io.StringIO()
    try:
        arguments = ["probe_ram.py", "127.0.0.1", "--port", str(port[0]), "--timeout", "0.3", "--samples", "3"]
        with mock.patch.object(sys, "argv", arguments), contextlib.redirect_stdout(output):
            code = probe_ram.main()
    finally:
        stop.set()
        server.join()
    return code, output.getvalue()


class AddressesTest(unittest.TestCase):
    def test_match_backend_definitions(self):
        player = json.loads((DEFINITIONS / "PlayerAddresses.json").read_text())
        party = json.loads((DEFINITIONS / "PartyAddresses.json").read_text())
        digimons = json.loads((DEFINITIONS / "Parties/DigimonsAddresses.json").read_text())
        status = json.loads((DEFINITIONS / "Parties/DigimonStatusAddresses.json").read_text())

        self.assertEqual(probe_ram.BITS, int(player["Bits"], 16))
        self.assertEqual(probe_ram.MAP_ID, int(player["MapId"], 16))
        self.assertEqual(probe_ram.PREVIOUS_MAP_ID, int(player["PreviousMapId"], 16))
        self.assertEqual(probe_ram.PARTY_SLOTS, tuple(int(slot["Address"], 16) for slot in party["Slots"]))
        self.assertEqual(probe_ram.EMPTY_SLOT, int(party["EmptySlotId"], 16))
        self.assertEqual(
            probe_ram.DIGIMON_BLOCKS,
            tuple((digimons[str(i)]["Name"], int(digimons[str(i)]["MemoryBlockAddress"], 16)) for i in range(8)),
        )
        self.assertEqual(probe_ram.LEVEL_OFFSET, int(status["Level"], 16))
        self.assertEqual(probe_ram.HP_OFFSET, int(status["HP"]["Current"], 16))
        self.assertEqual(probe_ram.MP_OFFSET, int(status["MP"]["Current"], 16))


class SnapshotsTest(unittest.TestCase):
    def test_every_snapshot_reads_as_the_game(self):
        self.assertEqual(len(SNAPSHOTS), 18)
        for snapshot in SNAPSHOTS:
            with self.subTest(snapshot.name):
                ram = snapshot.read_bytes()
                code, output = run_probe(ram, "nightly")
                self.assertEqual(code, 0, output)
                self.assertIn(f"Bits:          {struct.unpack_from('<i', ram, probe_ram.BITS)[0]}\n", output)
                self.assertIn(f"MapId:         {struct.unpack_from('<h', ram, probe_ram.MAP_ID)[0]:#x}", output)
                self.assertEqual(output.count("perdidas 0/3"), 3, output)

    def test_known_snapshot_values(self):
        name = "antes-patrulheiro-opcional-1-centro-admin-subsolo-andar-1.bin"
        code, output = run_probe((SNAPSHOTS[0].parent / name).read_bytes(), "nightly")
        self.assertEqual(code, 0, output)
        self.assertIn("Bits:          45846\n", output)
        self.assertIn("MapId:         0x21a", output)
        self.assertIn("Kotemon        Nv 25  HP 1862/2079", output)


class FailureTest(unittest.TestCase):
    ram = SNAPSHOTS[0].read_bytes() if SNAPSHOTS else b""

    def test_stable_build_stays_silent(self):
        code, output = run_probe(self.ram, "silent")
        self.assertEqual(code, 2)
        self.assertIn("nightly", output)

    def test_nothing_listening(self):
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as free:
            free.bind(("127.0.0.1", 0))
            port = free.getsockname()[1]
        output = io.StringIO()
        arguments = ["probe_ram.py", "127.0.0.1", "--port", str(port), "--timeout", "0.2"]
        with mock.patch.object(sys, "argv", arguments), contextlib.redirect_stdout(output):
            self.assertEqual(probe_ram.main(), 2)

    def test_build_without_fallback(self):
        code, output = run_probe(self.ram, "old-map")
        self.assertEqual(code, 3)
        self.assertIn("no memory map defined", output)

    def test_ram_without_a_game(self):
        code, output = run_probe(bytes(probe_ram.RAM_SIZE), "nightly")
        self.assertEqual(code, 4)
        self.assertIn("no parecen", output)


if __name__ == "__main__":
    unittest.main(verbosity=1)
