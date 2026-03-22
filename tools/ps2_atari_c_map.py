#!/usr/bin/env python3
import csv

def convertAndPrint(fileName, mapName):
    code_map = {}
    with open(fileName) as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            # Convert the first two items in the list
            idx = int(row[0], 16)
            value = int(row[1], 16)
            code_map[idx] = value

    print(f"const UBYTE {mapName}[256] = {{")
    print("    // 0     1     2     3     4     5     6     7     8     9     A     B     C     D     E     F")
    for row_start in range(0,256,16):
        line_values = []
        for col in range(16):
            idx = row_start + col
            value = code_map.get(idx, 0)
            line_values.append(f"0x{value:02x}")

        # Join the values with commas and add the row comment (e.g., // 10)
        line_str = ", ".join(line_values)
        print(f"    {line_str}, // {row_start:02X}")
    print("};")

convertAndPrint('ps2_atarist_code_mapping.csv', 'ps2_to_idkb_map')
convertAndPrint('ps2_atarist_code_mapping_ext.csv', 'ps2_to_idkb_ext_map')