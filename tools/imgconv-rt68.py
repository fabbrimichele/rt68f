#!/usr/bin/env python3
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

def save_with_header(filename, data, load_address):
    # Total length = data length (headers don't need to be counted)
    total_length = len(data)

    with open(filename, "wb") as f:
        # Write Load Address (Big Endian)
        f.write(load_address.to_bytes(4, 'big'))
        # Write Total Content Length (Big Endian)
        f.write(total_length.to_bytes(4, 'big'))
        # Write Actual Data
        f.write(data)

def save_palette(img, filename):
    raw_palette = img.getpalette()[:COLORS * 3]
    pal_data = bytearray()
    for i in range(0, len(raw_palette), 3):
        r, g, b = raw_palette[i] >> 4, raw_palette[i+1] >> 4, raw_palette[i+2] >> 4
        color_16bit = (r << 8) | (g << 4) | b
        pal_data.extend(color_16bit.to_bytes(2, 'big'))

    save_with_header(filename, pal_data, 0x00402000)

def save_packed_img(img, filename):
    pixels = list(img.getdata())
    pix_data = bytearray()

    if COLORS == 16:
        for i in range(0, len(pixels), 2):
            packed_byte = (pixels[i] << 4) | pixels[i+1]
            pix_data.append(packed_byte)
    elif COLORS == 4:
        # 2 bits per pixel: 4 pixels per byte
        for i in range(0, len(pixels), 4):
            # Ensure we don't go out of bounds if pixels count isn't multiple of 4
            p1 = pixels[i] & 0x03
            p2 = pixels[i+1] & 0x03 if i+1 < len(pixels) else 0
            p3 = pixels[i+2] & 0x03 if i+2 < len(pixels) else 0
            p4 = pixels[i+3] & 0x03 if i+3 < len(pixels) else 0

            packed_byte = (p1 << 6) | (p2 << 4) | (p3 << 2) | p4
            pix_data.append(packed_byte)
    else:
        # Fallback for 256 colors or others (1 byte per pixel)
        pix_data = bytearray(pixels)

    save_with_header(filename, pix_data, 0x00200000)

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
    print(f"Converts an image to a {WIDTH}x{HEIGHT} pixels image with {COLORS} colors and 4096 colors palette")
    print(f"usage: imgconv input_filename output_filename palette_filename")
