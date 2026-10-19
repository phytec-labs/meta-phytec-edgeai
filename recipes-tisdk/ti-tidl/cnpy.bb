# Extracted from TI meta-edgeai at tag REL.PSDK.ANALYTICS.11.02.00.03.
# ti-tidl-osrt 11.02 DEPENDS on cnpy. Two changes from TI's copy, both for
# wrynose: the LICENSE path is relative to S, and S itself is dropped because
# oe-core now sets it for git recipes and errors when a recipe sets it. The
# scarthgap branch of this layer keeps S = "${WORKDIR}/git".

SUMMARY = "NPZ/NPY file reader"
DESCRIPTION = "CNPY is opensource library for reading .npz and .npy files in C++"
HOMEPAGE = "https://github.com/rogersce/cnpy/"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=689f10b06d1ca2d4b1057e67b16cd580"

PV = "1.0.0"
BRANCH = "master"
SRC_URI = "git://github.com/rogersce/cnpy.git;branch=${BRANCH};protocol=https"
SRCREV = "4e8810b1a8637695171ed346ce68f6984e585ef4"

DEPENDS += "zlib"

# cnpy's CMakeLists asks for a CMake older than 3.5, and CMake 4 (wrynose
# ships 4.3.1) removed that compatibility. The policy flag is CMake's own
# suggested escape; CMake 3.x on scarthgap ignores it.
EXTRA_OECMAKE = "-DCMAKE_SKIP_RPATH=TRUE -DCMAKE_POLICY_VERSION_MINIMUM=3.5"

FILES:${PN} += "${libdir}/*"
FILES:${PN}-dev += "${includedir}/* ${libdir}/*"

do_install:append() {
   mv ${D}${libdir}/libcnpy.so ${D}${libdir}/libcnpy.so.1.0
   ln -s -r ${D}${libdir}/libcnpy.so.1.0 ${D}${libdir}/libcnpy.so
}

inherit cmake