SUMMARY = "Camera / encoder bring-up and regression scripts for the AM62Ax Edge AI demo"
DESCRIPTION = "edgeai-camera-probe reports the camera, encoder and GStreamer \
environment. edgeai-camera-test exercises capture, hardware JPEG and H.264 \
encode, and the RTP/UDP and TCP streaming paths."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://edgeai-camera-probe \
    file://edgeai-camera-test \
    file://edgeai-tiovx-probe \
"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/edgeai-camera-probe ${D}${bindir}/
    install -m 0755 ${WORKDIR}/edgeai-camera-test  ${D}${bindir}/
    install -m 0755 ${WORKDIR}/edgeai-tiovx-probe  ${D}${bindir}/
}

# Both scripts are shell; the runtime dependencies are the tools they call.
RDEPENDS:${PN} = "v4l-utils gstreamer1.0 coreutils"

COMPATIBLE_MACHINE = "am62axx"
