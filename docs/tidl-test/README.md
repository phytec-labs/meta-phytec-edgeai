# AM62Ax C7x / TIDL test record

What was tested on PD25.1.0, what had to be added to get there, and how to repeat
it on PD26. Everything below was measured on a phyBOARD-Lyra AM62A7 devkit on
2026-09-15.

## 1. The stack under test

| Part | Version |
|---|---|
| Image | `phytec-headless-image-phyboard-lyra-am62axx-2.rootfs-20260915174954.wic.xz` (PD25.1.0 + meta-phytec-edgeai) |
| Kernel | `6.12.35-11.01.05-phy6` plus one DT patch (section 3) |
| Remote-core firmware | ti-linux-firmware `92a31c8e`, frozen by meta-phytec (`ti-linux-fw-freeze.inc`) |
| C7x | `dsp_edgeai_c7x_1_release_strip.out`, 19 MB, TIDL network version 0x20250630 |
| DM (WKUP R5F) | `dm_edgeai_mcu1_0_release_strip.out`, packed into `tispl.bin` by U-Boot |
| TIOVX userspace | ti-vision-apps 11.01.00 (meta-edgeai tag `11.01.05.03`) |
| TIDL runtime | arm-tidl `d1fc5e3` = REL.TIDL.J7.11.01.00.00, `C7X_FIRMWARE_VERSION "11_01_00_00"` |
| OSRT | onnxruntime 1.15.0, tflite_runtime 2.12.0, dlr 1.13 (TI ARAGO wheels, `tidl-tools/11_00_00_00`) |
| Python | 3.12.11, numpy 1.26.4, opencv 4.9.0 |
| Models | TI Edge AI Model Zoo, `modelzoo/11_01_00_00/modelartifacts/AM62A/8bits` |

**The model artifacts must match the firmware's TIDL network version.** TI's
`11_01_00` folder now serves 0x20250821 artifacts (built with tools 11_01_06_00),
which this firmware rejects; the `11_01_00_00` folder serves 0x20250630, which it
accepts. Check with `python3 -c "import struct;print(hex(struct.unpack('<I',open('artifacts/<name>_tidl_net.bin','rb').read(4))[0]))"`.

## 2. What had to be added on top of stock PD25.1.0

All of it is recipe work in `meta-phytec-edgeai`; no BSP source changes besides the
DT patch.

| Added | Commits |
|---|---|
| TIOVX userspace: ti-vision-apps (needs `CROSS_COMPILE_LINARO=${TARGET_PREFIX}`), edgeai-apps-utils, edgeai-tiovx-kernels, edgeai-tiovx-modules, edgeai-gst-plugins | `8fdfc63`, `62fb286`, `046fbbc`, `8c90ddd`, `90badcd` |
| TIDL: ti-tidl (same `TARGET_PREFIX` fix, plus `FILES_SOLIBSDEV = ""` so the `.so` links stay in the runtime package), ti-tidl-osrt, edgeai-dl-inferer | `e24b40e`, `7db7e32`, `afd019e` |
| DL plugins re-enabled in edgeai-gst-plugins (back to upstream) | `cc73d33` |
| packagegroup-evolv-tiovx, then TIDL runtime added to it | `eeb71e7`, `1a2eda0` |
| Kernel DT patch reserving the IPC region, and the bbappend applying it | `2fab42b`, `f2cb304` |

Already present in stock PD25.1.0, nothing to do:
- C7x, DM and MCU R5F firmware, and the remoteproc/mailbox DT nodes
- the three EdgeAI carveouts, `carveout_edgeai_shared-memories` dma-heap, 576 MB CMA
- kernel config for remoteproc, rpmsg, dma-heap

Watch out on a minimal image:
- **numpy** reached the PD25 image through ROS (`cv-bridge`, `ros2topic`,
  `rosidl-*`, `sensor-msgs-py`, `tf2-*`) and `python3-opencv`. No TIDL recipe
  depends on it, but TI's wheels need it. Add `python3-numpy` explicitly.
- **GStreamer** is absent from `phytec-headless-image`; it came from our camera
  packagegroup. Only needed to preprocess images, and `cat224.raw` here avoids that.
- ONNX Runtime also declares coloredlogs, flatbuffers, protobuf and sympy. They are
  not in the image and `import onnxruntime` works without them.

## 3. Device tree

**One change**: reserve the 16 MB RTOS IPC region, which no PD25.1.x device tree
reserves although all three firmwares place `.bss:ipc_vring_mem` there.

```
&{/reserved-memory} {
	rtos_ipc_memory_region: ipc-memories@a0000000 {
		compatible = "shared-dma-pool";
		reg = <0x00 0xa0000000 0x00 0x01000000>;
		no-map;
	};
};
```

Shipped as `recipes-kernel/linux/linux-phytec-ti/0001-arm64-dts-ti-k3-am62a-phycore-som-Reserve-RTOS-IPC-memory.patch`.
The 6.12 SoM's reserved-memory node has no label, hence the path reference.

Everything else was already correct in PD25.1.0: C7x 1 MB + 31 MB, MCU R5F 1 + 15,
WKUP R5F 1 + 30, TF-A, OP-TEE, EdgeAI 32 MB at 0xa1000000, 172 MB dma-heap at
0xa3000000, 292 MB at 0xadc00000, CMA 576 MB at 0xc0000000, and `main_rti4` and
`main_timer2` left reserved for the C7x.

**Honest limit:** we added the reservation before testing and never ran TIDL
without it, so it is not proven to be required. It closes a real gap.

**On PD26** the IPC region comes from TI's `k3-am62a-ti-ipc-firmware.dtsi`. The
EdgeAI carveouts and the 30 MB WKUP R5F region need the two PD26 kernel fixes
("Add EdgeAI carveouts" and "Restore 30 MB WKUP R5F carveout"), verified on
hardware 2026-09-14. Review asked for the EdgeAI carveouts to move into an
overlay; if they do, make sure that overlay is applied before running these tests.

## 4. Procedure

Copy `tidl_check.py`, `tidl_proof.py`, `c7x_log.py`, `c7x_kernel_test.sh`,
`negative_control.sh`, `cat224.raw` and a model directory to the target.

1. **Memory map.** On target: `dmesg | grep -i "reserved mem"`, `grep "System RAM" /proc/iomem`,
   `cat /sys/class/remoteproc/remoteproc*/state`, `ls /dev/dma_heap`.
   On the host, check every firmware section against the DTB:
   `python3 carveout_check.py <decompiled.dts> <c7x.out> <dm.out> <mcu.fw>` (expect 0 uncovered).
2. **TIOVX graph on the C7x.** `./c7x_kernel_test.sh` — expects 57600/57600 pixels equal to a+b.
3. **Model version.** Run one TIDL inference, then `python3 c7x_log.py c7x 100` and look for
   `tivxKernelTIDLCreate ... Expected version - 0x...`. Pick the model-zoo folder whose
   `net.bin` first word matches.
4. **Inference.** `python3 tidl_check.py cat224.raw <models> tfl_cpu,tfl_tidl,onnx_cpu,onnx_tidl 0 20`
5. **Proof it ran on the C7x.**
   - `python3 tidl_proof.py cat224.raw <models> onnx tidl 100` — ORT profile must show all node
     kernels on TIDLExecutionProvider, and TI benchmark data must show subgraph proc time.
   - `python3 tidl_proof.py cat224.raw <models> tfl tidl 100 12` and, while it holds the session,
     `/opt/vision_apps/vx_app_heap_stats.out` — the C7x's L1, L3 and DDR heaps should be in use.
   - Compare host CPU time against wall time: ~0.25 with TIDL, ~1.0 or ~4.0 on CPU.
6. **Negative control.** `./negative_control.sh <models>` — TIDL must fail with the C7x stopped,
   CPU must still work, and TIDL must work again after restart.
7. **Stress.** `stress-ng --vm 1 --vm-bytes 50% --vm-method all --verify --timeout 90s &` then
   `python3 tidl_check.py cat224.raw <models> tfl_tidl 0 3000` — outputs must stay bit-identical.

## 5. Reference results (PD25.1.0)

| Measurement | Result |
|---|---|
| TIOVX `img_add` on DSP_C7-1 | 57600/57600 pixels correct |
| MobileNetV1 TFLite, CPU (4 threads / 1 thread) | 70.04 ms / 223.68 ms |
| MobileNetV1 TFLite, TIDL | 6.41 ms, top-1 285 Egyptian cat 74.2% |
| RegNetX-200MF ONNX, CPU | 61.44 ms, top-1 282 tiger cat |
| RegNetX-200MF ONNX, TIDL | 6.53 ms, same top-5 classes |
| ORT profile, TIDL mode | 101 kernels, all TIDLExecutionProvider, one op `TIDL_0` |
| TI benchmark, ONNX TIDL | subgraph proc 5.28 ms of 6.26 ms; DDR read 0.55 MB, write 7.56 MB |
| Host CPU time per inference | 1.80-1.90 ms with TIDL against ~270 ms on CPU |
| C7x heaps with a model loaded | DDR_LOCAL -11.6 MB, DDR_SCRATCH -4.2 MB, L3 1 MB and L1 16 KB fully used |
| Negative control | TIDL fails with C7x stopped; CPU unaffected; recovers on restart |
| Stress | 4000 inferences bit-identical; stress-ng passed 1, failed 0 |

## 6. Repeating this on PD26

- **Device tree:** handled by the two PD26 kernel fixes in section 3; if the
  EdgeAI carveouts land as an overlay, confirm it is applied. The IPC region comes
  from TI's dtsi.
- **Firmware:** PD26 ships the 2025-12-10 C7x build, byte-identical to PD25.1.1's
  (`8ec0c42b`), and different from PD25.1.0's. Its TIDL network version is unknown;
  get it from step 3.
- **Userspace must match that firmware.** meta-edgeai's 11.02.11.0x tags pin
  vision-apps REL.PSDK.ANALYTICS.11.02.00.05; that pairing is unverified. The
  version map we do have: arm-tidl `d1fc5e3` expects 0x20250630;
  REL.PSDK.ANALYTICS.AM62A.11.01.00.05 and REL.TIDL.11.01.06.00 expect 0x20250821.
- **Layer port:** meta-phytec-edgeai is `LAYERSERIES_COMPAT = "scarthgap"`; PD26 is
  wrynose.
- **Image contents:** PD26 headless has no ROS and no GStreamer, so add
  `python3-numpy`, and use the `cat224.raw` in this directory.

## 7. Gotchas

- `/dev/mem` reads of no-map regions fail with EFAULT; mmap with 8-byte aligned
  reads (see `c7x_log.py`).
- The TFLite TIDL delegate segfaults on a network-version mismatch; ONNX Runtime
  raises an exception instead.
- BusyBox `dmesg` has no `--level`; use `journalctl -k -p warning`. BusyBox `diff`
  writes unified output, so grep `^[-+]`, not `^[<>]`.
- The image has no rpm database, so on-target `rpm -q` answers are meaningless.
  Query pkgdata on the build host instead.

## 8. Files here

| File | Purpose |
|---|---|
| `tidl_check.py` | CPU against TIDL for TFLite and ONNX; reports latency, top-5, and identical-output count |
| `tidl_proof.py` | ORT per-node provider profile, TI benchmark counters, host CPU vs wall time |
| `c7x_log.py` | Reads the DM and C7x firmware log from shared memory |
| `c7x_kernel_test.sh` | TIOVX graph with a node on the C7x, verified pixel by pixel |
| `negative_control.sh` | TIDL with the C7x stopped, then restarted |
| `carveout_check.py` | Checks firmware sections against a DTB's reserved memory |
| `cat224.raw` | 224x224 RGB uint8 test input (a cat), ready to use |
