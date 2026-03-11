#!/usr/bin/env python3
import sys
import os

def calculate_fletcher16(data):
    sum_a = 0
    sum_b = 0

    for byte in data:
        sum_a = (sum_a + byte) % 256
        sum_b = (sum_b + sum_a) % 256

    # Combine into 16-bit value: (SumB << 8) | SumA
    return (sum_b << 8) | sum_a

if __name__ == "__main__":
    # Check if the user provided a filename
    if len(sys.argv) < 2:
        print("Usage: python checksum.py <your_binary_file.bin>")
    else:
        target_file = sys.argv[1]
        with open(target_file, "rb") as f:
            binary_data = f.read()
            payload = binary_data[8:]
            checksum = calculate_fletcher16(payload)
            print(f"File Checksum: {hex(checksum)}")