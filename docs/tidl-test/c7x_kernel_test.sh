#!/bin/sh
# TIOVX graph with a node on the C7x: TI's vx_app_c7x_kernel adds two 8-bit
# images with the user kernel app_c7x_kernel.img_add, which is registered only
# for target DSP_C7-1. Inputs stay <= 127 so saturating and wrapping addition
# give the same result, and the output is checked pixel by pixel against a + b.
# Run on target as root. Needs /opt/vision_apps from ti-vision-apps and python3-numpy.
set -e
W=${1:-/home/root/tidl/c7x}
mkdir -p "$W" && cd "$W" && rm -f out.bmp

python3 - <<'EOF'
import struct, numpy as np
W, H = 320, 180
y, x = np.mgrid[0:H, 0:W]
a = ((x + 2 * y) % 128).astype(np.uint8)
b = ((3 * x + y) % 128).astype(np.uint8)
def bmp24(path, g):
    px = np.repeat(g[::-1, :, None], 3, axis=2).tobytes()     # bottom-up rows, B=G=R
    open(path, 'wb').write(b'BM' + struct.pack('<IHHI', 54 + len(px), 0, 0, 54)
                           + struct.pack('<IiiHHIIiiII', 40, W, H, 1, 24, 0, len(px), 2835, 2835, 0, 0) + px)
bmp24('img_1.bmp', a)
bmp24('img_2.bmp', b)
(a.astype(np.uint16) + b).astype(np.uint8).tofile('expected.raw')
EOF

printf 'input_file_1 %s/img_1.bmp\ninput_file_2 %s/img_2.bmp\noutput_file %s/out.bmp\nwidth 320\nheight 180\n' "$W" "$W" "$W" > app.cfg
timeout 60 /opt/vision_apps/vx_app_c7x_kernel.out --cfg app.cfg > run.log 2>&1
echo "vx_app_c7x_kernel.out exit=$?"

python3 - <<'EOF'
import struct, numpy as np
d = open('out.bmp', 'rb').read()
off = struct.unpack_from('<I', d, 10)[0]
w, h = struct.unpack_from('<ii', d, 18)
bpp = struct.unpack_from('<H', d, 28)[0]
row = ((w * bpp // 8) + 3) & ~3
g = np.frombuffer(d[off:off + row * abs(h)], np.uint8).reshape(abs(h), row)[:, :w * bpp // 8][:, ::bpp // 8]
g = g[::-1] if h > 0 else g
e = np.fromfile('expected.raw', np.uint8).reshape(180, 320)
ok = int((g == e).sum())
print(f'out.bmp {w}x{h} {bpp}bpp: {ok}/{e.size} pixels equal a+b -> {"PASS" if ok == e.size else "FAIL"}')
EOF
