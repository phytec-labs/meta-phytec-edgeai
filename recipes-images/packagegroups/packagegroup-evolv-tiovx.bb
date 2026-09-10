DESCRIPTION = "TI TIOVX / OpenVX userspace for hardware dewarp on AM62Ax"
LICENSE = "MIT"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

# Kept separate from packagegroup-evolv-camera so the standard GStreamer stack
# (Bucket 1) can ship without pulling TI's TIOVX userspace, which carries the
# TI-TFL licence and is only needed for tiovxldc / tiovxmultiscaler.
#
# The C7x EdgeAI firmware is NOT listed here: meta-ti-bsp's
# ti-rtos-echo-test-fw already overrides C7X_1_FW:am62axx to
# dsp_edgeai_c7x_1_release_strip.out and installs it as am62a-c71_0-fw (the
# firmware-name in k3-am62a-main.dtsi), and it reaches the image through
# packagegroup-coprocessor, which phytec-headless-image already installs.

RDEPENDS:${PN} = " \
    ti-vision-apps \
    edgeai-apps-utils \
    edgeai-tiovx-kernels \
    edgeai-tiovx-modules \
    edgeai-gst-plugins \
"

COMPATIBLE_MACHINE = "am62axx"
