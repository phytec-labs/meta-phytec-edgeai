SUMMARY = "C7x and TIDL test scripts for the AM62Ax Edge AI demo"
DESCRIPTION = "Installs the scripts from docs/tidl-test: TIDL against CPU \
inference, the evidence that work runs on the C7x, a TIOVX graph with a node on \
the C7x, the remote-core log reader, the carveout checker, and a ready-made \
224x224 test input."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# The scripts live in docs/tidl-test, next to the runbook that documents them,
# so the installed copies cannot drift from the documentation.
FILESEXTRAPATHS:prepend := "${THISDIR}/../../docs/tidl-test:"

SRC_URI = " \
    file://tidl_check.py \
    file://tidl_proof.py \
    file://c7x_log.py \
    file://c7x_kernel_test.sh \
    file://negative_control.sh \
    file://carveout_check.py \
    file://cat224.raw \
"

S = "${UNPACKDIR}"
TESTDIR = "${datadir}/edgeai-tidl-test"

do_install() {
    install -d ${D}${TESTDIR}
    install -m 0755 ${S}/tidl_check.py ${D}${TESTDIR}/
    install -m 0755 ${S}/tidl_proof.py ${D}${TESTDIR}/
    install -m 0755 ${S}/c7x_log.py ${D}${TESTDIR}/
    install -m 0755 ${S}/c7x_kernel_test.sh ${D}${TESTDIR}/
    install -m 0755 ${S}/negative_control.sh ${D}${TESTDIR}/
    # carveout_check.py runs on the build host, against a decompiled DTB.
    install -m 0644 ${S}/carveout_check.py ${D}${TESTDIR}/
    install -m 0644 ${S}/cat224.raw ${D}${TESTDIR}/
}

RDEPENDS:${PN} = "python3-core python3-numpy ti-vision-apps ti-tidl"
RRECOMMENDS:${PN} = "edgeai-tidl-models"

COMPATIBLE_MACHINE = "am62axx"
