# PD25.1.x (kernel 6.12) leaves the 16 MB RTOS IPC region at 0xa0000000
# unreserved, although the C7x, DM and MCU R5F firmware all use it. Add the
# reservation there. PD26 (kernel 6.18) gets the same region from TI's
# k3-am62a-ti-ipc-firmware.dtsi, so the patch is not applied on that line.
FILESEXTRAPATHS:prepend := "${THISDIR}/linux-phytec-ti:"
SRC_URI:append:am62axx = "${@' file://0001-arm64-dts-ti-k3-am62a-phycore-som-Reserve-RTOS-IPC-memory.patch' if d.getVar('PV').startswith('6.12') else ''}"
