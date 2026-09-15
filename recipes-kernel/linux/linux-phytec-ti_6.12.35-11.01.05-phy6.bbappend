# Reserve the 16 MB RTOS IPC region at 0xa0000000. The C7x, DM and MCU R5F
# firmware use it for core-to-core IPC, but PD25.1.x leaves it unreserved.
FILESEXTRAPATHS:prepend := "${THISDIR}/linux-phytec-ti:"
SRC_URI:append:am62axx = " file://0001-arm64-dts-ti-k3-am62a-phycore-som-Reserve-RTOS-IPC-memory.patch"
