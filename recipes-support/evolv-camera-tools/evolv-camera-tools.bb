SUMMARY = "Camera / encoder bring-up and regression scripts for the Evolv pipeline"
DESCRIPTION = "evolv-camera-probe reports the camera, encoder and GStreamer \
environment. evolv-camera-test exercises capture, hardware JPEG and H.264 \
encode, and the RTP/UDP and TCP streaming paths."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://evolv-camera-probe \
    file://evolv-camera-test \
    file://evolv-tiovx-probe \
"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/evolv-camera-probe ${D}${bindir}/
    install -m 0755 ${WORKDIR}/evolv-camera-test  ${D}${bindir}/
    install -m 0755 ${WORKDIR}/evolv-tiovx-probe  ${D}${bindir}/
}

# Both scripts are shell; the runtime dependencies are the tools they call.
RDEPENDS:${PN} = "v4l-utils gstreamer1.0 coreutils"

COMPATIBLE_MACHINE = "am62axx"
