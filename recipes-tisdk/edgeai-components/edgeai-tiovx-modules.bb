# Extracted verbatim from TI meta-edgeai at tag REL.PSDK.ANALYTICS.11.02.00.03.
# Its arm-tidl (67eba49d) expects C7x firmware 11_02_02_00, TIDL network
# version 0x20251208, which is what PD25.1.1 ships (ti-linux-firmware
# 8ec0c42b). Newer meta-edgeai tags expect 11_02_20_00 (0x20260819) and that
# C7x would reject their networks. Taking the recipes directly avoids
# meta-edgeai's four meta-arago layer deps.

SUMMARY = "EdgeAI TIOVX modules"
DESCRIPTION = "EdgeAI TIOVX modules implements simple APIs to create and use OpenVX nodes"
HOMEPAGE = "https://git.ti.com/cgit/edgeai/edgeai-tiovx-modules"

LICENSE = "TI-TFL"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1f7721ee7d288457c5a70d0c8ff44b87"

PV = "1.0.0"
BRANCH = "main"
SRC_URI = "git://git.ti.com/git/edgeai/edgeai-tiovx-modules.git;branch=${BRANCH};protocol=https"
SRCREV = "11399cffa14dfb0bff65bf35dcbe04b701391eb8"

PLAT_SOC = ""
PLAT_SOC:j721e = "j721e"
PLAT_SOC:j721s2 = "j721s2"
PLAT_SOC:j784s4 = "j784s4"
PLAT_SOC:j722s = "j722s"
PLAT_SOC:j742s2 = "j742s2"
PLAT_SOC:am62axx = "am62a"

S = "${WORKDIR}/git"

DEPENDS = "ti-vision-apps edgeai-tiovx-kernels"
RDEPENDS:${PN}-source = "cmake"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j722s|j742s2|am62axx"

export SOC = "${PLAT_SOC}"

EXTRA_OECMAKE = "-DTARGET_FS=${WORKDIR}/recipe-sysroot -DINSTALL_SRC=on -DCMAKE_SKIP_RPATH=TRUE -DCMAKE_OUTPUT_DIR=${WORKDIR}/out"

PACKAGES += "${PN}-source"
FILES:${PN}-source += "/opt/"

inherit cmake

PR:append = "_edgeai_0"
