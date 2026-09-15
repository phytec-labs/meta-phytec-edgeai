#!/usr/bin/env python3
"""Check remote-core firmware against a DTB's reserved-memory.

Every DDR LOAD segment and allocated section of each firmware must fall inside
reserved memory that Linux will not hand out as general RAM (CMA and other
'reusable' pools excluded).

usage: carveout_check.py <decompiled.dts> <fw.elf> [<fw.elf> ...]
"""
import re
import subprocess
import sys

DDR_BASE = 0x80000000


def regions(dts):
    txt = open(dts).read()
    m = re.search(r'\n\treserved-memory \{', txt)
    i = j = m.end()
    depth = 1
    while depth:
        depth += (txt[j] == '{') - (txt[j] == '}')
        j += 1
    regs = []
    for nm in re.finditer(r'\n\t\t(\S+) \{(.*?)\n\t\t\};', txt[i:j - 1], re.S):
        name, body = nm.groups()
        r = re.search(r'\breg = <([^>]*)>', body)
        if not r:
            continue
        c = [int(x, 16) for x in r.group(1).split()]
        base, size = (c[0] << 32) | c[1], (c[2] << 32) | c[3]
        regs.append((base, base + size, name, bool(re.search(r'\breusable;', body))))
    return sorted(regs)


def fw_ranges(path):
    out = subprocess.run(['readelf', '-lSW', path], capture_output=True,
                         text=True, check=True).stdout
    rng = []
    for line in out.splitlines():
        s = line.split()
        if s and s[0] == 'LOAD' and int(s[5], 16):
            pa = int(s[3], 16)
            rng.append(('LOAD', pa, pa + int(s[5], 16)))
        m = re.match(r'\s*\[\s*\d+\]\s+(\S+)\s+\S+\s+([0-9a-f]{8,16})\s+[0-9a-f]+\s+'
                     r'([0-9a-f]+)\s+[0-9a-f]{2}\s+([A-Za-z]*)\s', line)
        if m and 'A' in m.group(4) and int(m.group(3), 16):
            a = int(m.group(2), 16)
            rng.append((m.group(1), a, a + int(m.group(3), 16)))
    return [r for r in rng if r[1] >= DDR_BASE]


def merged(regs):
    out = []
    for lo, hi, name, _ in (r for r in regs if not r[3]):
        if out and lo <= out[-1][1]:
            out[-1][1] = max(out[-1][1], hi)
            out[-1][2].append(name)
        else:
            out.append([lo, hi, [name]])
    return out


def main():
    regs = regions(sys.argv[1])
    print('reserved-memory:')
    for lo, hi, name, reusable in regs:
        print(f'  {lo:#011x}-{hi:#011x} {(hi - lo) / 2**20:7.1f} MB  {name}'
              + ('  [reusable: excluded]' if reusable else ''))
    bad = [(a, b) for a, b in zip(regs, regs[1:]) if b[0] < a[1]]
    for a, b in bad:
        print(f'  OVERLAP: {a[2]} / {b[2]}')
    print(f'  overlaps: {len(bad)}')
    spans = merged(regs)
    for fw in sys.argv[2:]:
        rng = fw_ranges(fw)
        miss, used = [], set()
        for what, lo, hi in rng:
            hit = next((s for s in spans if s[0] <= lo and hi <= s[1]), None)
            if hit:
                used.update(n for n in hit[2]
                            if any(r[2] == n and r[0] < hi and lo < r[1] for r in regs))
            else:
                miss.append((what, lo, hi))
        lo = min(r[1] for r in rng) if rng else 0
        hi = max(r[2] for r in rng) if rng else 0
        print(f'\n{fw}\n  DDR span {lo:#x}-{hi:#x}, {len(rng)} ranges, '
              f'{len(miss)} uncovered; regions used: {", ".join(sorted(used)) or "-"}')
        for what, a, b in miss:
            print(f'  UNCOVERED {what:<24} {a:#011x}-{b:#011x} ({(b - a) / 1024:.0f} KB)')


main()
