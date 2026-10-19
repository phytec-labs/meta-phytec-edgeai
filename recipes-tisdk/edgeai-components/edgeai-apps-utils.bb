# Extracted verbatim from TI meta-edgeai at tag REL.PSDK.ANALYTICS.11.02.00.03.
# Its arm-tidl (67eba49d) expects C7x firmware 11_02_02_00, TIDL network
# version 0x20251208: the generation PD25.1.1 (ti-linux-firmware 8ec0c42b) and
# PD26.1.0 (9dffeff8) ship. Newer meta-edgeai tags expect 11_02_20_00
# (0x20260819) and that C7x would reject their networks. Taking the recipes
# directly avoids meta-edgeai's four meta-arago layer deps.

SUMMARY = "EdgeAI Apps Utils"
DESCRIPTION = "EdgeAI Apps Utils implements ARMv8 neon optimized utility functions and also NV12 post process utility functions "
HOMEPAGE = "https://git.ti.com/cgit/edgeai/edgeai-apps-utils"

LICENSE = "TI-TFL"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1f7721ee7d288457c5a70d0c8ff44b87"

PV = "1.0.0"
BRANCH = "main"
SRC_URI = "git://git.ti.com/git/edgeai/edgeai-apps-utils.git;protocol=https;branch=${BRANCH}"
SRCREV = "5a5a694ae02f0d2e4e39028b847e1c777c465cbf"

PLAT_SOC = ""
PLAT_SOC:j721e = "j721e"
PLAT_SOC:j721s2 = "j721s2"
PLAT_SOC:j784s4 = "j784s4"
PLAT_SOC:j722s = "j722s"
PLAT_SOC:j742s2 = "j742s2"
PLAT_SOC:am62axx = "am62a"

S = "${WORKDIR}/git"

DEPENDS = "ti-vision-apps"
RDEPENDS:${PN}-source += "python3-core cmake"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j722s|j742s2|am62axx"

export SOC = "${PLAT_SOC}"

EXTRA_OECMAKE = "-DTARGET_FS=${WORKDIR}/recipe-sysroot -DCMAKE_SKIP_RPATH=TRUE -DCMAKE_OUTPUT_DIR=${WORKDIR}/out"

PACKAGES += "${PN}-source"
FILES:${PN}-source += "/opt/"

inherit cmake

do_install:append() {
    CP_ARGS="-Prf --preserve=mode,timestamps --no-preserve=ownership"

    mkdir -p ${D}/opt/edgeai-apps-utils
    cp ${CP_ARGS} ${S}/* ${D}/opt/edgeai-apps-utils
    cd ${D}/opt/edgeai-apps-utils
    rm -rf build lib
}

PR:append = "_edgeai_0"
