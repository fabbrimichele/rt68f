#!/usr/bin/env python3
from PIL import Image
import sys

from PIL import Image
import sys

def adjust_image(img):
    img = img.convert("RGB")  # Ensure RGB mode
    img = img.resize((320, 200), Image.Resampling.LANCZOS)
    # Quantize to 16 colors (Indexed mode)
    # 'P' mode is indexed, 'palette=Image.ADAPTIVE' finds the best 16 colors
    img = img.quantize(colors=16, method=Image.Quantize.MAXCOVERAGE)
    return img

def save_palette(img, filename):
    # 3. Extract the Palette and convert to 12-bit (444)
    # Pillow stores palette as [R0, G0, B0, R1, G1, B1...] (8-bit each)
    # 3. Handle the Palette (12-bit / 444 format)
    raw_palette = img.getpalette()[:48]  # Get R,G,B for 16 colors
    with open(filename, "wb") as f_pal:
        for i in range(0, len(raw_palette), 3):
            # Convert 8-bit color to 4-bit (0-255 -> 0-15)
            r = raw_palette[i] >> 4
            g = raw_palette[i+1] >> 4
            b = raw_palette[i+2] >> 4

            # Pack into 16-bit Big Endian word: 0000 RRRR GGGG BBBB
            color_16bit = (r << 8) | (g << 4) | b
            f_pal.write(color_16bit.to_bytes(2, 'big'))

def save_packed_img(img, filename):
    pixels = list(img.getdata())
    with open(filename, "wb") as f_pix:
        for i in range(0, len(pixels), 2):
            # Combine two 4-bit nibbles into one byte
            # Pixel A is high nibble, Pixel B is low nibble
            packed_byte = (pixels[i] << 4) | pixels[i+1]
            f_pix.write(packed_byte.to_bytes(1, 'big'))

def convert(in_filename, out_filename, palette_filename):
    """Convert a PNG image to 320x200 pixels 16 colors with palette"""
    img = Image.open(in_filename)
    img = adjust_image(img)
    save_palette(img, palette_filename)
    save_packed_img(img, out_filename)

if len(sys.argv) == 4:
    in_filename = sys.argv[1]
    out_filename = sys.argv[2]
    palette_filename = sys.argv[3]
    print(f"Converting '{in_filename}' to image '{out_filename}' and palette '{palette_filename}'...")
    convert(in_filename, out_filename, palette_filename)
    print(f"Done")
else:
    print(f"Converts an image to a 320x200 pixels image with 16 colors and 4096 colors palette")
    print(f"usage: imgconv input_filename output_filename palette_filename")
