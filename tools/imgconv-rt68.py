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

def save_with_header(filename, data, load_address):
    # Total length = 8 bytes header + data length
    total_length = 8 + len(data)

    with open(filename, "wb") as f:
        # Write Load Address (Big Endian)
        f.write(load_address.to_bytes(4, 'big'))
        # Write Total Content Length (Big Endian)
        f.write(total_length.to_bytes(4, 'big'))
        # Write Actual Data
        f.write(data)

def save_palette(img, filename):
    raw_palette = img.getpalette()[:48]
    pal_data = bytearray()
    for i in range(0, len(raw_palette), 3):
        r, g, b = raw_palette[i] >> 4, raw_palette[i+1] >> 4, raw_palette[i+2] >> 4
        color_16bit = (r << 8) | (g << 4) | b
        pal_data.extend(color_16bit.to_bytes(2, 'big'))

    # Using your fixed address for palette
    save_with_header(filename, pal_data, 0x00013000)

def save_packed_img(img, filename):
    pixels = list(img.getdata())
    pix_data = bytearray()
    for i in range(0, len(pixels), 2):
        packed_byte = (pixels[i] << 4) | pixels[i+1]
        pix_data.append(packed_byte)

    # FIXED: Added the missing save call here
    save_with_header(filename, pix_data, 0x00008000)

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
