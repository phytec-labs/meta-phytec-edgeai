# meta-phytec-evolv

Camera, hardware-encode and streaming support for the Evolv pipeline on
phyBOARD-Lyra AM62Ax, built on **BSP-Yocto-Ampliphy-AM62Ax-PD25.1.0**
(kernel `6.12.35-11.01.05-phy6`, GStreamer 1.22.12, Scarthgap).

## What this layer does

Installs the GStreamer elements Evolv asked for, plus the pieces needed to
actually run and verify a camera → hardware-encode → stream pipeline, and two
scripts to prove it works on the board.

**No kernel changes are required.** Everything needed is already enabled in
`phytec_ti_defconfig`:

| Config | Provides |
|---|---|
| `CONFIG_USB_VIDEO_CLASS=m` | `uvcvideo` — USB webcams |
| `CONFIG_VIDEO_E5010_JPEG_ENC=m` | hardware JPEG encoder |
| `CONFIG_VIDEO_WAVE_VPU=m` | Wave5 H.264/H.265 codec |
| `CONFIG_VIDEO_TI_J721E_CSI2RX=m` | CSI-2 receiver (for MIPI cameras later) |

So this is purely an image-composition layer. That is also the answer to
Evolv's original report: six of their eight elements were never missing from
the BSP — they were simply not in `phytec-headless-image`.

## Setting it up

Add the layer to `build/conf/bblayers.conf`:

```
BBLAYERS += "${OEROOT}/../meta-phytec-evolv"
```

Then, inside the build container:

```bash
source sources/poky/oe-init-build-env
bitbake-layers show-layers        # confirm phytec-evolv is listed
bitbake phytec-headless-image
```

The `phytec-headless-image.bbappend` adds `packagegroup-evolv-camera` for
`am62axx` only, so other machines in the BSP are unaffected.

## Verifying on the board

Plug in the USB camera, then:

```
evolv-camera-probe          # environment check — run this first
evolv-camera-test           # capture, encode and stream tests
```

`evolv-camera-probe` reports which `/dev/video*` node is the camera, the JPEG
encoder and the H.264 encoder — it detects them by driver name rather than
assuming numbering, because the numbering shifts when a USB camera is plugged
in. It also confirms all six requested elements resolve.

`evolv-camera-test` runs five tests:

1. `videotestsrc → v4l2jpegenc → multifilesink` — hardware JPEG with no camera
2. camera capture to a raw file
3. camera → `v4l2jpegenc` → `rtpjpegpay` → `udpsink`
4. camera → `v4l2h264enc` → `rtph264pay` → `udpsink`
5. camera → `v4l2h264enc` → `tcpserversink`

Between them these exercise all six elements plus both hardware encoders.

Receive the UDP stream on a host on the same network:

```bash
# JPEG (test 3)
gst-launch-1.0 udpsrc port=5000 \
  ! application/x-rtp,encoding-name=JPEG,payload=26 \
  ! rtpjpegdepay ! jpegdec ! autovideosink

# H.264 (test 4)
gst-launch-1.0 udpsrc port=5002 \
  ! application/x-rtp,encoding-name=H264,payload=96 \
  ! rtph264depay ! avdec_h264 ! autovideosink
```

Pass `--host <your-ip>` to `evolv-camera-test` to send there instead of
loopback.

### Test 1 is also a regression test

It encodes a static `checkers-8` pattern and checks that every frame hashes
identically and that the e5010 interrupt count advances by exactly the frame
count. That is a deliberate guard against the stale-TLB defect Evolv found in
the SDK 11.02 e5010 driver, where the MMU TLB is not invalidated between
buffers — frames come back corrupted and then the encoder hangs.

**PD25.1.0 is not affected.** Its 6.12.35 driver has no MMU layer at all; that
code arrived with SDK 11.02 (PD25.1.1, kernel 6.12.57). Keeping the check here
means an upgrade to a 11.02-based BSP cannot reintroduce the bug silently.

Note `smpte` is unsuitable as the test pattern — its noise block changes every
frame, which would mask exactly the corruption being looked for.

## What this layer deliberately does not do

### TIOVX dewarp (`tiovxldc`, `tiovxmultiscaler`)

The other two elements Evolv asked for. They are not stock GStreamer — they
come from TI's `edgeai-gst-plugins`, packaged only in
`meta-edgeai` (`git.ti.com/edgeai/meta-edgeai`), and integrating that layer is
a separate piece of work:

- `LAYERDEPENDS_meta-edgeai` requires `meta-arago-distro`, `meta-arago-extras`,
  `meta-arago-test` and `meta-ti-foundational` — none of which ship in the
  PHYTEC BSP.
- Its `layer.conf` does `require conf/distro/include/branding-core.inc`, so
  without the Arago layers bitbake fails at *parse* time, not build time.
- It sets `BBFILE_PRIORITY_meta-edgeai = "13"` ("higher than all others"),
  which overrides `meta-ampliphy` and `meta-phytec` wherever they overlap.
- Its kernel and u-boot bbappends target `linux-ti-staging` and
  `u-boot-ti-staging`, not PHYTEC's recipes, so they dangle silently here.

The likely cheaper path is to extract just `edgeai-tiovx-modules`,
`edgeai-gst-plugins` and `ti-vision-apps` into a local layer, without the Arago
distro machinery. AM62A is supported upstream — `edgeai-gst-plugins.bb` has
`COMPATIBLE_MACHINE = "...|am62axx"` and `PLAT_SOC:am62axx = "am62a"` — so no
SoC porting is needed.

Two things already in place on the PHYTEC side, worth knowing before starting:

- The **C7x EdgeAI firmware already ships**. `meta-ti-bsp`'s
  `ti-rtos-echo-test-fw` overrides `C7X_1_FW:am62axx` to
  `dsp_edgeai_c7x_1_release_strip.out` and registers it as `am62a-c71_0-fw`,
  which is the `firmware-name` in the device tree. Despite the recipe name it
  is not an echo-test stub on this SoC.
- The **EdgeAI DDR carveouts are already in the SoM device tree**
  (`k3-am62a-phycore-som.dtsi`) at byte-identical addresses to TI's
  `k3-am62a7-sk-edgeai.dtso`, and `c7x_0` is `status = "okay"`. PHYTEC bakes
  these in; TI ships them as an optional overlay.

The remaining gap for TIOVX is therefore userspace only.

**The USB camera cannot validate dewarp anyway.** A Logitech HD1080P has a
rectilinear lens with no meaningful distortion to correct, and `tiovxldc`
requires a lens-distortion LUT generated for a specific lens. Dewarp
verification needs the real fisheye module.

### ROS 2

Not addressed here. On Yocto that means `meta-ros` (`meta-ros-common`,
`meta-ros2`, `meta-ros2-jazzy`, branch `scarthgap`) — Jazzy and Scarthgap are
the intended pairing. That is a separate layer addition and a large one:
recipes are auto-generated by `superflore`, so expect a long first build and
some individual recipe failures to work through.

## Layout

```
conf/layer.conf                                    priority 25, above meta-phytec
recipes-core/images/phytec-headless-image.bbappend adds the packagegroup for am62axx
recipes-images/packagegroups/
    packagegroup-evolv-camera.bb                  the GStreamer set
recipes-support/evolv-camera-tools/               probe + test scripts
```

`packagegroup-evolv-camera.bb` separates the six elements Evolv named from the
supporting plugins the pipelines need (`h264parse`, `multifilesink`,
`videoconvert`, `matroskamux`), so it stays obvious which is which.

There is no `gstreamer1.0-tools` package in OE-core — `gst-inspect-1.0` and
`gst-launch-1.0` live in the plain `gstreamer1.0` package.
