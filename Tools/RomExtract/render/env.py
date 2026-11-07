"""Sets where the scripts of this folder find their inputs; see ../README.md for the variables."""

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

for name in ("DW3_ISO", "DW3_DECOMP", "DW3_DDW3", "DW3_OUT"):
    if name not in os.environ:
        raise SystemExit(f"Falta la variable {name}; ver Tools/RomExtract/README.md")

S = os.environ["DW3_OUT"]
