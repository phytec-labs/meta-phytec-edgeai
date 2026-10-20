# Extracted verbatim from TI meta-edgeai at tag REL.PSDK.ANALYTICS.11.02.00.03.
# Its arm-tidl (67eba49d) expects C7x firmware 11_02_02_00, TIDL network
# version 0x20251208, which is what PD25.1.1 ships (ti-linux-firmware
# 8ec0c42b). Newer meta-edgeai tags expect 11_02_20_00 (0x20260819) and that
# C7x would reject their networks. Taking the recipes directly avoids
# meta-edgeai's four meta-arago layer deps.

SUMMARY = "EdgeAI DL Inferer"
DESCRIPTION = "A Linux based CPP library for abstracting Open Source Runtime (OSRT) API"
HOMEPAGE = "https://git.ti.com/cgit/edgeai/edgeai-dl-inferer/about/"

LICENSE = "TI-TFL"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4309553a9d3611cdf7a78bd169ec583c"

PV = "1.0.0"
BRANCH = "main"
SRC_URI = "git://git.ti.com/git/edgeai/edgeai-dl-inferer.git;protocol=https;branch=${BRANCH}"
SRCREV = "d06b07d3058b1859df0e32c3ca545b70b8cf3ec9"

PLAT_SOC = ""
PLAT_SOC:j721e = "j721e"
PLAT_SOC:j721s2 = "j721s2"
PLAT_SOC:j784s4 = "j784s4"
PLAT_SOC:j722s = "j722s"
PLAT_SOC:am62axx = "am62a"

S = "${WORKDIR}/git"

DEPENDS = "edgeai-apps-utils ti-tidl-osrt yaml-cpp opencv ti-vision-apps"
RDEPENDS:${PN} += "ti-tidl-osrt-staticdev"
RDEPENDS:${PN}-source = "bash python3-core cmake python3-yamlloader python3-numpy opencv opencv-dev"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j722s|am62axx"

export SOC = "${PLAT_SOC}"

EXTRA_OECMAKE = "-DTARGET_FS=${WORKDIR}/recipe-sysroot -DCMAKE_SKIP_RPATH=TRUE -DCMAKE_OUTPUT_DIR=${WORKDIR}/out"

inherit python3-dir

FILES:${PN} += " ${PYTHON_SITEPACKAGES_DIR}/*"
PACKAGES += "${PN}-source"
FILES:${PN}-source += "/opt/"

inherit cmake

do_install:append() {
    CP_ARGS="-Prf --preserve=mode,timestamps --no-preserve=ownership"

    mkdir -p ${D}/opt/edgeai-dl-inferer
    cp ${CP_ARGS} ${S}/* ${D}/opt/edgeai-dl-inferer
    cp ${CP_ARGS} ${WORKDIR}/out/bin ${D}/opt/edgeai-dl-inferer
    rm -rf ${D}/usr/cmake
}

INSANE_SKIP:${PN} += "dev-deps"
INSANE_SKIP:${PN}-source += "dev-deps"

PR:append = "_edgeai_2"
