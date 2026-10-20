# Reserve the 16 MB RTOS IPC region at 0xa0000000. The C7x, DM and MCU R5F
# firmware use it for core-to-core IPC, but PD25.1.x leaves it unreserved.
#
# Versionless on purpose: a bbappend named for one kernel version dangles
# silently on any other release ("No recipes in default available for"), and
# PD25.1.0 and PD25.1.1 ship 6.12.35-11.01.05-phy6 and 6.12.57-11.02.11-phy6
# respectively. Every PD25.1.x kernel is 6.12, so no version guard is needed
# here; the wrynose branch guards on PV because PD26 is 6.18.
FILESEXTRAPATHS:prepend := "${THISDIR}/linux-phytec-ti:"
SRC_URI:append:am62axx = " file://0001-arm64-dts-ti-k3-am62a-phycore-som-Reserve-RTOS-IPC-memory.patch"
