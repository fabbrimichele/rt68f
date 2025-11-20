from PIL import Image
import sys

# --- CONFIG ---
input_png = "img4col.png"    # your 4-color PNG
output_raw = "img.tmp"       # output packed 2-bit file

# Define your 4 colors and their 2-bit values
# Format: (R,G,B): value
palette_map = {
    (0,115,179): 0,  # #0073B3
    (0,159,116): 1,  # #009F74
    (214,94,0): 2,   # #D65E00
    (240,229,64): 3  # #F0E540
}

# --- Load image ---
img = Image.open(input_png)
img = img.convert("RGB")  # ensure RGB mode
w, h = img.size
pixels = list(img.getdata())

# --- Pack pixels 4 per byte ---
out_bytes = bytearray()
for i in range(0, len(pixels), 4):
    byte = 0
    for j in range(4):
        if i+j < len(pixels):
            rgb = pixels[i+j]
            if rgb not in palette_map:
                raise ValueError(f"Pixel color {rgb} not in palette_map")
            value = palette_map[rgb] & 0b11
        else:
            value = 0  # padding for last byte
        byte = (byte << 2) | value
    out_bytes.append(byte)

# --- Write output ---
with open(output_raw, "wb") as f:
    f.write(out_bytes)

print(f"Packed 2-bit raw written to {output_raw} ({len(out_bytes)} bytes)")
