#!/usr/bin/env python3
"""Print the remote-core log that the DM and C7x firmware write to shared memory.

usage (on target, as root): c7x_log.py [c7x|dm] [min_timestamp_s]

The EdgeAI firmware keeps a text log in app_log_mem at 0xa1000000 (256 KB): the
DM (MCU1_0) section starts near +0x4000, the C7x_1 section near +0x8000.
Linux never maps this region (no-map), so a plain read of /dev/mem fails with
EFAULT; mmap it and read with aligned 8-byte loads instead.
"""
import mmap
import os
import re
import sys

core = sys.argv[1] if len(sys.argv) > 1 else 'c7x'
since = float(sys.argv[2]) if len(sys.argv) > 2 else 0.0
off = {'dm': 0x4000, 'c7x': 0x8000}[core]

fd = os.open('/dev/mem', os.O_RDONLY | os.O_SYNC)
m = mmap.mmap(fd, 0x40000, mmap.MAP_SHARED, mmap.PROT_READ, offset=0xa1000000)
mv = memoryview(m).cast('Q')
data = b''.join(mv[i].to_bytes(8, 'little') for i in range(len(mv)))

for chunk in re.findall(rb'[\x20-\x7e\n]{6,}', data[off:off + 0x4000]):
    for line in chunk.decode().splitlines():
        t = re.match(r'\s*(\d+\.\d+) s:', line)
        if line.strip() and (t is None and since == 0 or t and float(t.group(1)) >= since):
            print(line.rstrip())
