# Extracted verbatim from TI meta-edgeai at tag 11.01.05.03 (SDK 11.01),
# to match this BSP's PD25.1.0 kernel (6.12.35-11.01.05-phy6). Taking the
# recipes directly avoids pulling in meta-edgeai's four meta-arago layer deps.

SUMMARY = "EdgeAI GST plugins"
DESCRIPTION = "EdgeAI GST plugins implements custom elements to offload compute to HW accelerators and DSPs on TI devices"
HOMEPAGE = "https://github.com/TexasInstruments/edgeai-gst-plugins"

LICENSE = "TI-TFL"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1f7721ee7d288457c5a70d0c8ff44b87"

PV = "1.0.0"
BRANCH = "main"
SRC_URI = "git://github.com/TexasInstruments/edgeai-gst-plugins.git;branch=${BRANCH};protocol=https"
SRCREV = "fea12213b449fc1ea117ad5f0016c08f417ab46d"

PLAT_SOC = ""
PLAT_SOC:j721e = "j721e"
PLAT_SOC:j721s2 = "j721s2"
PLAT_SOC:j784s4 = "j784s4"
PLAT_SOC:j742s2 = "j742s2"
PLAT_SOC:j722s = "j722s"
PLAT_SOC:am62axx = "am62a"

S = "${WORKDIR}/git"

# Deep-learning plugins dropped: we need tiovxldc/tiovxmultiscaler only.
# Upstream drops these via the :adas override, which does not apply here.
DEPENDS = "edgeai-tiovx-modules edgeai-apps-utils gstreamer1.0-plugins-base"
RDEPENDS:${PN}-source = "bash meson ninja"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j742s2|j722s|am62axx"

export SOC = "${PLAT_SOC}"

PACKAGES += "${PN}-source"
FILES:${PN}-source += "/opt/"
FILES:${PN} += "${libdir}/gstreamer-1.0/*.so"

EXTRA_OEMESON = "--prefix=/usr -Dpkg_config_path=${S}/pkgconfig -Ddl-plugins=disabled"

inherit meson pkgconfig

do_install:append() {
    CP_ARGS="-Prf --preserve=mode,timestamps --no-preserve=ownership"

    mkdir -p ${D}/opt/edgeai-gst-plugins
    cp ${CP_ARGS} ${S}/* ${D}/opt/edgeai-gst-plugins
}

INSANE_SKIP:${PN}-source += "dev-deps"
INSANE_SKIP:${PN} += "rpaths"

PR:append = "_edgeai_0"
