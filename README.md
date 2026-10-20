# meta-phytec-edgeai

Edge AI demo layer for the PHYTEC phyCORE-AM62Ax on phyBOARD-Lyra: camera
capture, hardware encode and streaming, TI TIOVX hardware dewarp, and TIDL deep
learning on the C7x DSP. ROS 2 Jazzy is optional.

This branch targets **BSP-Yocto-Ampliphy-AM62Ax-PD26.1.0**: kernel
`6.18.38-12.01.00.05-phy6`, GStreamer 1.28.5, Python 3.14.7, wrynose.

## Branches

| Branch or tag | Targets | State |
|---|---|---|
| `wrynose` | PD26.1.0 | Builds and resolves; **not yet verified on hardware** |
| `scarthgap` | PD25.1.1 | Builds and resolves; not yet verified on hardware |
| tag `pd25.1.0-verified` | PD25.1.0 | The configuration proven end to end on the devkit |

Where more than one BSP release shares a Yocto series, the branch tracks the
latest. The TI EdgeAI userspace is pinned to the **C7x firmware**, not to the
Yocto series: PD25.1.1 and PD26.1.0 ship the same AM62A C7x blob and so share
one TI pin, while PD25.1.0 needs the older one kept at the tag.

## What it provides

| Piece | Contents |
|---|---|
| `phytec-edgeai-demo-image` | `phytec-headless-image` plus the packagegroups below |
| `packagegroup-edgeai-camera` | GStreamer capture, hardware JPEG and H.264 encode, RTP/UDP and TCP streaming, and the `edgeai-camera-probe` / `edgeai-camera-test` scripts |
| `packagegroup-edgeai-tiovx` | TI TIOVX and TIDL userspace: `tiovxldc`, `tiovxmultiscaler`, the TFLite delegate and ONNX Runtime provider that run models on the C7x, and `edgeai-tiovx-probe` |
| `packagegroup-edgeai-ros` | ROS 2 Jazzy `ros-base`, `gscam`. Parsed only when meta-ros is in `bblayers.conf` |
| kernel bbappend | Reserves the 16 MB RTOS IPC region at 0xa0000000 on 6.12 kernels only; PD26 already has it (see below) |
| `edgeai-tidl-test` | The scripts from `docs/tidl-test/`, installed in `/usr/share/edgeai-tidl-test` |
| `edgeai-tidl-models` | TI model-zoo artifacts matching this firmware (`11_02_02_00`), in `/opt/model_zoo` |
| `stb`, `cnpy` | Dependencies of the 11.02 TI recipes that no PHYTEC BSP provides |
| `docs/tidl-test/` | Test record, scripts and reference results for the C7x and TIDL |

No kernel configuration changes are needed. `phytec_ti_defconfig` already enables
`USB_VIDEO_CLASS`, `VIDEO_E5010_JPEG_ENC`, `VIDEO_WAVE_VPU` and
`VIDEO_TI_J721E_CSI2RX`, and the SoM device tree enables the C7x.

**Memory reservations differ between PD25 and PD26.**

- The 16 MB RTOS IPC region at 0xa0000000 is reserved on PD26 because
  `k3-am62a-phycore-som.dtsi` includes TI's `k3-am62a-ti-ipc-firmware.dtsi`. No
  PD25.1.x device tree does, which is what the kernel bbappend patches in; the
  append is guarded on `PV` so it applies to 6.12 only.
- The EdgeAI carveouts are **not in PD26's base device tree**. They live in the
  overlay `ti/k3-am62a-phycore-edgeai.dtbo`: `edgeai-dma-memory@a1000000`
  (32 MB), `dma-heap-carveout@a3000000` (172 MB) and
  `edgeai-core-heap-memory@adc00000` (292 MB). It must be enabled at boot, or
  boot the pre-composed `k3-am62a7-phyboard-lyra-edgeai.dtb`, which is the RDK
  device tree with that overlay already applied. Without it, TIDL has no
  memory to work in.

## Verified

**On hardware, at tag `pd25.1.0-verified`** (AM62Ax devkit, USB Logitech C920):

| Check | Result |
|---|---|
| `edgeai-camera-probe` / `edgeai-camera-test` / `edgeai-tiovx-probe` | 12/12, 7/7, 6/6 |
| camera to `tiovxldc` to e5010 JPEG | 30/30 frames |
| camera to `tiovxmultiscaler` to Wave5 H.264 to RTP/UDP | pass |
| TIOVX graph with a node on the C7x (`vx_app_c7x_kernel`) | 57600/57600 pixels correct |
| MobileNetV1 (TFLite + TIDL) on the C7x | 6.4 ms against 70.0 ms on four A53 cores |
| RegNetX-200MF (ONNX Runtime + TIDL) on the C7x | 6.5 ms against 61.4 ms |
| 4000 inferences during a memory stress test | bit-identical outputs |

**On PD26.1.0, this branch so far:** parse exit 0, `bitbake -n
phytec-edgeai-demo-image` 12187 tasks exit 0, and `stb`, `cnpy` and
`ti-tidl-osrt` build. `ti-vision-apps` and `ti-tidl` are unbuilt, and nothing
has run on a board. Treat a passing dry run as necessary and not sufficient:
every porting bug found so far parsed cleanly and failed only at build time.

See `docs/tidl-test/README.md` for how each hardware result was measured.

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
source sources/oe-core/oe-init-build-env     # PD25 used sources/poky
bitbake-layers show-layers                   # confirm phytec-edgeai is listed
bitbake phytec-edgeai-demo-image
```

`ti-vision-apps`, `ti-tidl` and `do_rootfs` are memory-hungry. On a 32 GB host
running a desktop, default parallelism was OOM-killed; `BB_NUMBER_THREADS = "8"`
and `PARALLEL_MAKE = "-j 8"` fixed it.

`ti-vision-apps` fetches from `git.ti.com` through the `repo://` fetcher, which
has **no timeout**: if that server is unreachable the task hangs indefinitely
and bitbake reports only "still alive (no events for N seconds)". Wrap
unattended builds in a watchdog. After an interrupted fetch, run `bitbake -c
clean ti-vision-apps` before rebuilding.

### 4. Enable the EdgeAI overlay

TIDL needs the carveouts described above. Either add
`ti/k3-am62a-phycore-edgeai.dtbo` to the overlays applied at boot, or boot
`k3-am62a7-phyboard-lyra-edgeai.dtb`.

### 5. Optional: ROS 2 Jazzy

Add `meta-ros-common`, `meta-ros2` and `meta-ros2-jazzy` to `bblayers.conf`. The
ROS packagegroup is then parsed and added to the demo image automatically;
without them the layer builds unchanged. This was verified with meta-ros on
scarthgap and **has not been tried on wrynose**, where ROS 2 Jazzy may not have
a matching meta-ros branch.

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

**Model artifacts must match the firmware's TIDL network version.** A model
carries the version it was compiled for in the first word of its `net.bin`, and
the C7x refuses anything else, logging `Network version - 0x..., Expected
version - 0x...`. This branch pins model-zoo folder `11_02_02_00`
(0x20251208); the tag pins `11_01_00_00` (0x20250630) for PD25.1.0.

**The Python test scripts do not run on PD26.** TI publishes its TFLite and ONNX
Runtime wheels built for Python 3.12 (`cp312`) and PD26 ships 3.14.7, so the
interpreter cannot load them; `tidl_check.py` and `tidl_proof.py` are therefore
scarthgap-only. On PD26 use the C and GStreamer paths, which are unaffected:

```bash
/opt/tidl_test/TI_DEVICE_armv8_test_dl_algo_host_rt.out   # TI's standalone TIDL test
gst-inspect-1.0 tidlinferer                               # TIDL GStreamer element
cd /usr/share/edgeai-tidl-test && ./c7x_kernel_test.sh    # TIOVX graph on the C7x
```

Test 1 of `edgeai-camera-test` is an e5010 regression test: a static pattern must
hash identically every frame, with the interrupt count advancing by exactly the
frame count. It guards against the SDK 11.02 stale-TLB defect.

## TI components

TI's `meta-edgeai` cannot be added as a layer here: it depends on four Arago
layers the BSP does not ship, requires an Arago distro include at parse time, and
its priority 13 would override `meta-ampliphy` and `meta-phytec`. The recipes are
copied instead, from tag **`REL.PSDK.ANALYTICS.11.02.00.03`**.

**Choose that tag by firmware, never by recency.** Its `arm-tidl` (67eba49d)
expects C7x firmware `11_02_02_00` / network version 0x20251208, which is the
generation PD25.1.1 and PD26.1.0 ship. TI's newer tags expect `11_02_20_00`
(0x20260819) and this board's C7x would reject their networks. `arm-tidl`'s
`rt/inc/itidl_ti.h` states both values. The AM62A vision-apps manifest is
`REL.PSDK.ANALYTICS.AM62A.11.02.01.02`.

| Recipe | Change from upstream |
|---|---|
| `ti-vision-apps`, `ti-tidl` | `CROSS_COMPILE_LINARO=${TARGET_PREFIX}`; upstream hardcodes Arago's triple |
| `ti-tidl` | `FILES_SOLIBSDEV = ""`, so the `.so` links stay in the runtime package. ONNX Runtime, DLR and the TFLite delegate load the TIDL libraries by their unversioned names |
| `ti-tidl-osrt` | `S = "${UNPACKDIR}/src"`; its wheels unpack with `subdir=${S}/...`, which bitbake requires to be inside the unpack root |
| `cnpy` | From meta-edgeai, plus `-DCMAKE_POLICY_VERSION_MINIMUM=3.5`: wrynose ships CMake 4, which dropped support for cnpy's `cmake_minimum_required` |
| `stb` | From meta-arago, with TI's `S` dropped; oe-core sets it for git recipes and errors if a recipe sets it |
| `edgeai-gst-plugins`, `edgeai-apps-utils`, `edgeai-tiovx-kernels`, `edgeai-tiovx-modules`, `edgeai-dl-inferer` | none |

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
- **Porting between series touches paths, not logic.** wrynose unpacks into
  `${WORKDIR}/sources` and rejects `S = "${WORKDIR}"`; scarthgap has no
  `UNPACKDIR` and unpacks into `${WORKDIR}`. `conf/layer.conf` sets
  `UNPACKDIR ??= "${WORKDIR}"` so recipes can name `${UNPACKDIR}` on both.
  Version-named bbappends dangle silently on a BSP that moved on, so the kernel
  append is `linux-phytec-ti_%.bbappend` with a `PV` guard.

## Known limits

- **Nothing on this branch has run on a board.** The TIDL network version is
  inferred from `arm-tidl`; PD26's C7x blob is stripped, so only its log line
  settles it.
- **Dewarp output is not verified as geometrically correct.** `tiovxldc` remaps
  using a lens-distortion LUT for one specific lens; a C920 is rectilinear.
  Checking correctness needs the target fisheye module and its LUT.
- **ROS 2 on wrynose is untried**, as noted above.

## Layout

```
conf/layer.conf                                       priority 25
recipes-images/images/phytec-edgeai-demo-image.bb     the demo image
recipes-images/packagegroups/
    packagegroup-edgeai-camera.bb                     GStreamer stack
    packagegroup-edgeai-tiovx.bb                      TIOVX + TIDL userspace
recipes-support/edgeai-camera-tools/                  probe and test scripts
recipes-support/stb/                                  ti-vision-apps dependency
recipes-tisdk/                                        recipes from meta-edgeai
recipes-kernel/linux/                                 RTOS IPC reservation patch
dynamic-layers/ros2-jazzy-layer/                      ROS 2, when meta-ros is present
docs/tidl-test/                                       C7x and TIDL test record
```
