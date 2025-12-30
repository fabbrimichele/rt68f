#!/usr/bin/env python3
from PIL import Image
import sys

from PIL import Image
import sys

WIDTH = 320
HEIGHT = 200
COLORS = 256

def adjust_image(img):
    img = img.convert("RGB")  # Ensure RGB mode
    img = img.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
    # Quantize to 16 colors (Indexed mode)
    # 'P' mode is indexed, 'palette=Image.ADAPTIVE' finds the best 16 colors
    img = img.quantize(colors=COLORS, method=Image.Quantize.MAXCOVERAGE)
    return img

def convert(in_filename, out_filename):
    """Convert a PNG image to 320x200 pixels 16 colors with palette"""
    img = Image.open(in_filename)
    img = adjust_image(img)
    img.save(out_filename)

if len(sys.argv) == 3:
    in_filename = sys.argv[1]
    out_filename = sys.argv[2]
    print(f"Converting to PNG 320x200 pixel 16 colors...")
    convert(in_filename, out_filename)
    print(f"Done")
else:
    print(f"Converts an image to a PNG image [320x200 pixels, 16 colors]")
    print(f"usage: imgconv-png input_filename output_filename")
