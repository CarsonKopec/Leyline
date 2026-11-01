"""Draws Leyline's icon: glowing lines of power crossing at a node, on a dark tile.

Writes src/main/resources/assets/leyline/icon.png (128x128), used by Mod Menu and the Modrinth and CurseForge pages.
Run from the repository root: python tools/generate_icon.py (needs `pip install numpy pillow`).
"""

import math
from pathlib import Path

import numpy as np
from PIL import Image

SIZE = 128
SCALE = 4  # drawn this many times larger, then downsampled, for smooth edges
OUT = Path("src/main/resources/assets/leyline/icon.png")


def hex_rgb(value):
    value = value.lstrip("#")
    return np.array([int(value[i:i + 2], 16) / 255 for i in (0, 2, 4)])


def segment_distance(x, y, a, b):
    """Distance from every pixel to the segment a-b."""
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    t = np.clip(((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy), 0, 1)
    return np.hypot(x - (ax + t * dx), y - (ay + t * dy))


def main():
    n = SIZE * SCALE
    y, x = (np.mgrid[0:n, 0:n] + 0.5) / SCALE

    # A dark tile with rounded corners, lighter towards the middle.
    centre = SIZE / 2
    radial = np.hypot(x - centre, y - centre) / (SIZE * 0.7)
    rgb = hex_rgb("#1B1238") * (1 - radial[..., None]) + hex_rgb("#08060F") * radial[..., None]
    corner = 22
    qx = np.maximum(np.abs(x - centre) - (centre - corner), 0)
    qy = np.maximum(np.abs(y - centre) - (centre - corner), 0)
    alpha = np.clip(corner - np.hypot(qx, qy), 0, 1)

    # Three lines meeting at the node, each fading out towards the tile's edge.
    node = (centre, centre + 4)
    lines = [
        ((14, 30), "#5FE3FF"),
        ((116, 22), "#B58CFF"),
        ((70, 120), "#FFC861"),
    ]
    glow = np.zeros_like(rgb)
    for end, colour in lines:
        d = segment_distance(x, y, node, end)
        along = np.hypot(x - node[0], y - node[1]) / math.dist(node, end)
        fade = np.clip(1.15 - along, 0, 1)
        core = np.clip(2.4 - d, 0, 1)
        halo = np.exp(-(d / 7) ** 2) * 0.55
        glow += hex_rgb(colour) * ((core + halo) * fade)[..., None]

    # Faint rings round the node, and the node itself.
    r = np.hypot(x - node[0], y - node[1])
    for radius, strength in ((22, 0.35), (36, 0.18)):
        glow += hex_rgb("#B58CFF") * (np.exp(-((r - radius) / 1.2) ** 2) * strength)[..., None]
    glow += hex_rgb("#FFFFFF") * (np.exp(-(r / 6) ** 2) * 1.4)[..., None]
    glow += hex_rgb("#D8C2FF") * (np.exp(-(r / 16) ** 2) * 0.6)[..., None]

    rgb = 1 - (1 - rgb) * np.exp(-glow)  # glow adds light without clipping to flat white
    image = np.dstack([rgb, alpha])
    image = (np.clip(image, 0, 1) * 255).round().astype(np.uint8)
    icon = Image.fromarray(image, "RGBA").resize((SIZE, SIZE), Image.LANCZOS)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(OUT, optimize=True)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
