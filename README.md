# meta-phytec-edgeai

Camera capture, hardware dewarp, hardware encode and streaming for the Evolv
pipeline on phyBOARD-Lyra AM62Ax.

Built on **BSP-Yocto-Ampliphy-AM62Ax-PD25.1.0**: kernel
`6.12.35-11.01.05-phy6`, GStreamer 1.22.12, Scarthgap.

## What it provides

| Packagegroup | Contents |
|---|---|
| `packagegroup-evolv-camera` | `v4l2jpegenc`, `v4l2h264enc`, `tcpserversink`, `udpsink`, `rtpjpegpay`, `rtph264pay`, plus the supporting plugins the pipelines need |
| `packagegroup-evolv-tiovx` | TI TIOVX userspace: `tiovxldc` (dewarp) and `tiovxmultiscaler`, running on the C7x DSP and VPAC |

`phytec-headless-image` installs both on `am62axx` only. Other machines in the
BSP are unaffected.

No kernel changes are needed. `phytec_ti_defconfig` already enables
`USB_VIDEO_CLASS`, `VIDEO_E5010_JPEG_ENC`, `VIDEO_WAVE_VPU` and
`VIDEO_TI_J721E_CSI2RX`, and the SoM device tree already enables the C7x and
its EdgeAI memory carveouts.

## Verified on hardware

On the AM62Ax devkit with a USB Logitech C920:

| Check | Result |
|---|---|
| `evolv-camera-probe` | 12/12 |
| `evolv-camera-test` | 7/7 |
| `evolv-tiovx-probe` | 6/6 |
| camera → `tiovxldc` → e5010 JPEG | 30/30 frames |
| camera → `tiovxmultiscaler` → Wave5 H.264 → RTP/UDP | pass |
| camera → LDC → multiscaler → JPEG → RTP/UDP | pass |

## Setup

### 1. Set `DL_DIR` outside the BSP tree — required

Add to `build/conf/local.conf`:

```
DL_DIR = "/path/outside/the/bsp/downloads"
```

**Do not skip this.** The BSP root is a `repo` client (it has a `.repo/`), and
`ti-vision-apps` fetches with bitbake's `repo://` fetcher. With the default
`DL_DIR` inside the BSP, `repo` walks up from the download directory, finds the
BSP's own `.repo`, re-points it at TI's manifest, and `repo sync` **deletes
every PHYTEC layer from `sources/`**.

It fails almost silently: the fetch reports success, writes a 119-byte tarball,
and the first error is an unrelated `do_unpack` failure. Check that no `.repo`
exists at your chosen path or any directory above it.

### 2. Add the layer

In `build/conf/bblayers.conf`:

```
BBLAYERS += "${OEROOT}/../meta-phytec-evolv"
```

### 3. Build

Inside the build container:

```bash
source sources/poky/oe-init-build-env
bitbake-layers show-layers        # confirm phytec-evolv is listed
bitbake phytec-headless-image
```

`ti-vision-apps` and `do_rootfs` are memory-hungry. On a 32 GB host running a
desktop, the default parallelism was OOM-killed; capping it fixed this:

```
BB_NUMBER_THREADS = "8"
PARALLEL_MAKE = "-j 8"
```

If a `ti-vision-apps` fetch is ever interrupted, run
`bitbake -c cleanall ti-vision-apps` before rebuilding. A stale fetch stamp
otherwise makes `do_unpack` fail on a missing tarball.

## Verifying on the board

Plug in the USB camera, then:

```
evolv-camera-probe     # camera, encoders and the six elements
evolv-camera-test      # capture, encode and stream
evolv-tiovx-probe      # TIOVX plugin, libraries, C7x, dewarp element
```

`evolv-camera-test` runs:

1. `videotestsrc → v4l2jpegenc → multifilesink`: hardware JPEG, no camera
2. camera capture to a raw file
3. camera → `v4l2jpegenc` → `rtpjpegpay` → `udpsink`, port 5000
4. camera → `v4l2h264enc` → `rtph264pay` → `udpsink`, port 5002
5. camera → `v4l2h264enc` → `tcpserversink`, port 5004

Pass `--host <your-ip>` to stream to another machine, then receive with:

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

`evolv-tiovx-probe` checks in stages, because each failure means something
different:

1. plugin registers: the TIOVX userspace built correctly
2. libraries resolve: no missing `.so`
3. C7x remoteproc is `running`: firmware and carveouts are correct
4. `tiovxldc` instantiates: the graph builds

A typical dewarp pipeline:

```bash
gst-launch-1.0 v4l2src device=/dev/videoN \
  ! video/x-raw,format=YUY2,width=1280,height=720 \
  ! videoconvert ! video/x-raw,format=NV12 \
  ! tiovxldc ! tiovxmultiscaler ! video/x-raw,width=640,height=360 \
  ! v4l2jpegenc ! rtpjpegpay ! udpsink host=<ip> port=5000
```

### Test 1 is an e5010 regression test

It encodes a static `checkers-8` pattern and requires every frame to hash
identically and the e5010 interrupt count to advance by exactly the frame
count. This guards against the stale-TLB defect in the SDK 11.02 e5010 driver,
which corrupts frames and then hangs the encoder.

PD25.1.0 is not affected, because its 6.12.35 driver has no MMU layer. The
test is there so a move to an 11.02-based BSP cannot bring the bug back
silently. Don't use the `smpte` pattern: its noise block changes every frame
and would hide the corruption.

## TIOVX integration

The TIOVX recipes come from TI's `meta-edgeai`, but the layer itself is not
added. It cannot be used alongside a PHYTEC BSP:

- `LAYERDEPENDS` requires four Arago layers that the BSP does not ship.
- Its `layer.conf` requires an Arago distro include, so without them bitbake
  fails at parse time.
- `BBFILE_PRIORITY = "13"` would override `meta-ampliphy` and `meta-phytec`.

Instead, five recipes are copied from `meta-edgeai` tag **`11.01.05.03`**, which
matches PD25.1.0's SDK 11.01.05. TIOVX userspace, the C7x firmware and the
kernel are ABI-coupled, so don't pull from the scarthgap branch head, which
targets SDK 11.02.

| Recipe | Change from upstream |
|---|---|
| `ti-vision-apps` | `CROSS_COMPILE_LINARO=${TARGET_PREFIX}`. Upstream hardcodes Arago's `aarch64-oe-linux-`, which fails under any other distro. |
| `edgeai-gst-plugins` | Drops `edgeai-dl-inferer`/`ti-tidl-osrt` and sets `-Ddl-plugins=disabled`. Dewarp does not need the TIDL runtime. |
| `edgeai-apps-utils`, `edgeai-tiovx-kernels`, `edgeai-tiovx-modules` | none |

The C7x firmware is not in this layer. `meta-ti-bsp`'s `ti-rtos-echo-test-fw`
installs `dsp_edgeai_c7x_1_release_strip.out` as `am62a-c71_0-fw` on
`am62axx`, and it reaches the image through `packagegroup-coprocessor`. Despite
the recipe name, on this SoC it is the real EdgeAI firmware, not a test stub.

### Licensing

The TIOVX components are licensed `TI-TFL`, the same TI firmware licence as the
C7x firmware already in the stock image. They are kept in their own
packagegroup so they can be left out: comment out the
`packagegroup-evolv-tiovx` line in
`recipes-core/images/phytec-headless-image.bbappend`.

## Gotchas

- **`/dev/videoN` numbering changes between boots.** On the same image the
  camera has been `video1` on one boot and `video0` on the next, and the e5010
  has moved from `video0` to `video2`. Find devices by driver name with
  `v4l2-ctl --info`. For the codecs, use the stable `/dev/video-enc0` and
  `/dev/video-dec0` symlinks. The Wave5 driver reports itself as
  `wave5-enc`/`wave5-dec`.
- **The UVC metadata node also reports driver `uvcvideo`.** Also check that
  `v4l2-ctl --list-formats` lists capture formats.
- **Encoder elements need `/dev/video*` access.** The nodes are
  `root:video 0660`, and gst-v4l2 only registers encoders it can open while
  probing. A user outside the `video` group sees no encoder elements at all.
- **There is no `gstreamer1.0-tools` package in OE-core.** `gst-inspect-1.0`
  and `gst-launch-1.0` are in the `gstreamer1.0` package.
- **`PACKAGECONFIG[v4l2]` is enabled only by OE-core's default list.** A layer
  that sets `PACKAGECONFIG` for `gstreamer1.0-plugins-good`, instead of
  appending to it, silently removes `v4l2jpegenc` and `v4l2h264enc`.

## Known limits

- **Dewarp output is not verified as geometrically correct.** `tiovxldc`
  remaps using a lens-distortion LUT generated for one specific lens. A C920 is
  rectilinear, so it has nothing to correct. The tests show that the plugin
  loads, the C7x runs and frames flow through the pipeline. Checking correctness
  needs the target fisheye module and its LUT.
- **ROS 2 is not included.** On Yocto that means `meta-ros` (`meta-ros-common`,
  `meta-ros2`, `meta-ros2-jazzy`, branch `scarthgap`), which is a separate and
  large layer addition.

## Layout

```
conf/layer.conf                                     priority 25
recipes-core/images/phytec-headless-image.bbappend  installs both packagegroups
recipes-images/packagegroups/
    packagegroup-evolv-camera.bb                    GStreamer stack
    packagegroup-evolv-tiovx.bb                     TIOVX userspace
recipes-support/evolv-camera-tools/                 probe and test scripts
recipes-tisdk/                                      recipes from meta-edgeai
```
