# meta-phytec-edgeai

Edge AI demo layer for the PHYTEC phyCORE-AM62Ax on phyBOARD-Lyra: camera
capture, hardware encode and streaming, TI TIOVX hardware dewarp, and TIDL deep
learning on the C7x DSP. ROS 2 Jazzy is optional.

Built on **BSP-Yocto-Ampliphy-AM62Ax-PD25.1.0**: kernel
`6.12.35-11.01.05-phy6`, GStreamer 1.22.12, scarthgap.

## What it provides

| Piece | Contents |
|---|---|
| `phytec-edgeai-demo-image` | `phytec-headless-image` plus the packagegroups below |
| `packagegroup-edgeai-camera` | GStreamer capture, hardware JPEG and H.264 encode, RTP/UDP and TCP streaming, and the `edgeai-camera-probe` / `edgeai-camera-test` scripts |
| `packagegroup-edgeai-tiovx` | TI TIOVX and TIDL userspace: `tiovxldc`, `tiovxmultiscaler`, the TFLite delegate and ONNX Runtime provider that run models on the C7x, and `edgeai-tiovx-probe` |
| `packagegroup-edgeai-ros` | ROS 2 Jazzy `ros-base`, `gscam`. Parsed only when meta-ros is in `bblayers.conf` |
| kernel bbappend | Reserves the 16 MB RTOS IPC region at 0xa0000000, which no PD25.1.x device tree reserves |
| `docs/tidl-test/` | Test record, scripts and reference results for the C7x and TIDL |

No kernel configuration changes are needed. `phytec_ti_defconfig` already enables
`USB_VIDEO_CLASS`, `VIDEO_E5010_JPEG_ENC`, `VIDEO_WAVE_VPU` and
`VIDEO_TI_J721E_CSI2RX`, and the SoM device tree already enables the C7x and its
EdgeAI carveouts.

## Verified on hardware

On the AM62Ax devkit with a USB Logitech C920:

| Check | Result |
|---|---|
| `edgeai-camera-probe` / `edgeai-camera-test` / `edgeai-tiovx-probe` | 12/12, 7/7, 6/6 |
| camera to `tiovxldc` to e5010 JPEG | 30/30 frames |
| camera to `tiovxmultiscaler` to Wave5 H.264 to RTP/UDP | pass |
| TIOVX graph with a node on the C7x (`vx_app_c7x_kernel`) | 57600/57600 pixels correct |
| MobileNetV1 (TFLite + TIDL) on the C7x | 6.4 ms against 70.0 ms on four A53 cores |
| RegNetX-200MF (ONNX Runtime + TIDL) on the C7x | 6.5 ms against 61.4 ms |
| 4000 inferences during a memory stress test | bit-identical outputs |

See `docs/tidl-test/README.md` for how each was measured and how to repeat it.

## Setup

### 1. Set `DL_DIR` outside the BSP tree — required

```
DL_DIR = "/path/outside/the/bsp/downloads"
```

**Do not skip this.** The BSP root is a `repo` client and `ti-vision-apps`
fetches with bitbake's `repo://` fetcher. With the default `DL_DIR` inside the
BSP, `repo` finds the BSP's own `.repo`, re-points it at TI's manifest, and
`repo sync` **deletes every PHYTEC layer from `sources/`**. It fails almost
silently: the fetch reports success and writes a 119-byte tarball.

### 2. Add the layer

```
BBLAYERS += "${OEROOT}/../meta-phytec-edgeai"
```

### 3. Build

```bash
source sources/poky/oe-init-build-env
bitbake-layers show-layers        # confirm phytec-edgeai is listed
bitbake phytec-edgeai-demo-image
```

`ti-vision-apps`, `ti-tidl` and `do_rootfs` are memory-hungry. On a 32 GB host
running a desktop, default parallelism was OOM-killed; `BB_NUMBER_THREADS = "8"`
and `PARALLEL_MAKE = "-j 8"` fixed it. If a `ti-vision-apps` fetch is
interrupted, run `bitbake -c cleanall ti-vision-apps` before rebuilding.

### 4. Optional: ROS 2 Jazzy

Add `meta-ros-common`, `meta-ros2` and `meta-ros2-jazzy` (branch `scarthgap`) to
`bblayers.conf`. The ROS packagegroup is then parsed and added to the demo image
automatically. Without them the layer builds unchanged.

## On the board

```
edgeai-camera-probe     # camera, encoders and the GStreamer elements
edgeai-camera-test      # capture, hardware encode, RTP/UDP and TCP streaming
edgeai-tiovx-probe      # TIOVX plugin, libraries, C7x state, dewarp element
```

A dewarp pipeline:

```bash
gst-launch-1.0 v4l2src device=/dev/videoN \
  ! video/x-raw,format=YUY2,width=1280,height=720 \
  ! videoconvert ! video/x-raw,format=NV12 \
  ! tiovxldc ! tiovxmultiscaler ! video/x-raw,width=640,height=360 \
  ! v4l2jpegenc ! rtpjpegpay ! udpsink host=<ip> port=5000
```

For deep learning, follow `docs/tidl-test/README.md`. **Model artifacts must
match the firmware's TIDL network version.** On PD25.1.0 that is TI's model-zoo
folder `11_01_00_00` (network version 0x20250630); the default `11_01_00` folder
now serves newer artifacts that this firmware rejects.

Test 1 of `edgeai-camera-test` is an e5010 regression test: a static pattern must
hash identically every frame, with the interrupt count advancing by exactly the
frame count. PD25.1.0 is not affected, but the test keeps the SDK 11.02 stale-TLB
defect from returning silently.

## TI components

TI's `meta-edgeai` cannot be added as a layer here: it depends on four Arago
layers the BSP does not ship, requires an Arago distro include at parse time, and
its priority 13 would override `meta-ampliphy` and `meta-phytec`. Instead the
recipes are copied from tag **`11.01.05.03`**, which matches PD25.1.0's SDK
11.01.05. TIOVX userspace, the TIDL runtime, the C7x firmware and the kernel are
version-coupled, so do not pull from the branch head.

| Recipe | Change from upstream |
|---|---|
| `ti-vision-apps`, `ti-tidl` | `CROSS_COMPILE_LINARO=${TARGET_PREFIX}`; upstream hardcodes Arago's triple |
| `ti-tidl` | `FILES_SOLIBSDEV = ""`, so the `.so` links stay in the runtime package. ONNX Runtime, DLR and the TFLite delegate load the TIDL libraries by their unversioned names |
| `edgeai-gst-plugins` | none; DL plugins enabled |
| `edgeai-apps-utils`, `edgeai-tiovx-kernels`, `edgeai-tiovx-modules`, `ti-tidl-osrt`, `edgeai-dl-inferer` | none |

The C7x firmware is not in this layer. `meta-ti-bsp`'s `ti-rtos-echo-test-fw`
installs `dsp_edgeai_c7x_1_release_strip.out` as `am62a-c71_0-fw`, and it reaches
the image through `packagegroup-coprocessor`. Despite the recipe name, on this
SoC it is the real EdgeAI firmware.

**Licensing:** the TI components are `TI-TFL`, the same licence as the C7x
firmware in the stock image. They are in their own packagegroup, so dropping the
`packagegroup-edgeai-tiovx` line from the demo image leaves a camera-only build.

## Gotchas

- **`/dev/videoN` numbering changes between boots.** Find devices by driver name
  with `v4l2-ctl --info`, and use `/dev/video-enc0` and `/dev/video-dec0` for the
  codecs. The Wave5 driver reports itself as `wave5-enc` / `wave5-dec`.
- **The UVC metadata node also reports driver `uvcvideo`.** Also check that
  `v4l2-ctl --list-formats` lists capture formats.
- **Encoder elements need `/dev/video*` access.** The nodes are `root:video 0660`
  and gst-v4l2 only registers encoders it can open while probing.
- **There is no `gstreamer1.0-tools` package in OE-core.** `gst-inspect-1.0` and
  `gst-launch-1.0` are in the `gstreamer1.0` package.
- **`PACKAGECONFIG[v4l2]` is enabled only by OE-core's default list.** A layer
  that sets `PACKAGECONFIG` instead of appending silently removes `v4l2jpegenc`
  and `v4l2h264enc`.
- **On the target, BusyBox `dmesg` has no `--level`** (use `journalctl -k -p
  warning`) and BusyBox `diff` writes unified output.

## Known limits

- **Dewarp output is not verified as geometrically correct.** `tiovxldc` remaps
  using a lens-distortion LUT for one specific lens; a C920 is rectilinear.
  Checking correctness needs the target fisheye module and its LUT.
- **PD26 is not covered yet.** Its C7x firmware differs from PD25.1.0's, so the
  TIDL userspace and model artifacts have to be matched to it first. See section
  6 of `docs/tidl-test/README.md`.

## Layout

```
conf/layer.conf                                       priority 25
recipes-images/images/phytec-edgeai-demo-image.bb     the demo image
recipes-images/packagegroups/
    packagegroup-edgeai-camera.bb                     GStreamer stack
    packagegroup-edgeai-tiovx.bb                      TIOVX + TIDL userspace
recipes-support/edgeai-camera-tools/                  probe and test scripts
recipes-tisdk/                                        recipes from meta-edgeai
recipes-kernel/linux/                                 RTOS IPC reservation patch
dynamic-layers/ros2-jazzy-layer/                      ROS 2, when meta-ros is present
docs/tidl-test/                                       C7x and TIDL test record
```
