# Extracted verbatim from TI meta-edgeai at tag REL.PSDK.ANALYTICS.11.02.00.03.
# Its arm-tidl (67eba49d) expects C7x firmware 11_02_02_00, TIDL network
# version 0x20251208: the generation PD25.1.1 (ti-linux-firmware 8ec0c42b) and
# PD26.1.0 (9dffeff8) ship. Newer meta-edgeai tags expect 11_02_20_00
# (0x20260819) and that C7x would reject their networks. Taking the recipes
# directly avoids meta-edgeai's four meta-arago layer deps.

SUMMARY = "TIDL Runtime Modules"
DESCRIPTION = "TIDL Runtime Modules like TIDL-RT, TF-LITE Delegate Library and ONNXRT Execution provider Library"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

S = "${WORKDIR}/git"
PR:append = "_edgeai_2"
PV="1.0.0"

SRCREV_FORMAT="default"
SRCREV_arm-tidl="67eba49d18b60714a18981813da08e43d98ab741"
SRCREV_concerto="f5541b85b9973ca47680d1a6d970bf61a126daa8"
SRCREV_onnxruntime="5816ccce5bb4b9b8ce1869bdda397257a8d2028a"
SRCREV_tensorflow="422156a973b23bab6b86176a245a66193dccb995"
SRCREV_protobuf="f0dc78d7e6e331b8c6bb2d5283e06aa26883ca7c"

SRC_URI = " \
    git://git.ti.com/git/processor-sdk-vision/arm-tidl.git;branch=master;protocol=https;name=arm-tidl;destsuffix=git/arm-tidl \
    git://git.ti.com/git/processor-sdk/concerto.git;branch=main;protocol=https;name=concerto;destsuffix=git/concerto \
    git://github.com/TexasInstruments/onnxruntime;branch=tidl-1.15;protocol=https;name=onnxruntime;destsuffix=git/onnxruntime  \
    git://github.com/TexasInstruments/tensorflow;branch=tidl-j7-2.12;protocol=https;name=tensorflow;destsuffix=git/tensorflow  \
    git://github.com/protocolbuffers/protobuf;branch=main;protocol=https;name=protobuf;destsuffix=git/protobuf \
"

PLAT_SOC = ""
PLAT_SOC:j721e = "j721e"
PLAT_SOC:j721s2 = "j721s2"
PLAT_SOC:j784s4 = "j784s4"
PLAT_SOC:j722s = "j722s"
PLAT_SOC:j742s2 = "j742s2"
PLAT_SOC:am62axx = "am62a"

CPU = "A72"
CPU:am62axx = "A53"
CPU:j722s = "A53"

DEPENDS += "ti-vision-apps"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j722s|j742s2|am62axx"

export TARGET_FS = "${WORKDIR}/recipe-sysroot"

FILES:${PN} += "/opt/*"
FILES:${PN} += "${libdir}/*"
FILES:${PN} += "${includedir}/*"

EXTRA_OEMAKE += "-C ${S}/arm-tidl"

do_compile() {
    ln -snf ${TARGET_FS} ${WORKDIR}/targetfs

    PSDK_INSTALL_PATH=${WORKDIR} \
    IVISION_PATH=${TARGET_FS}${includedir}/processor_sdk/ivision \
    VISION_APPS_PATH=${TARGET_FS}${includedir}/processor_sdk/vision_apps \
    APP_UTILS_PATH=${TARGET_FS}${includedir}/processor_sdk/app_utils \
    TIOVX_PATH=${TARGET_FS}${includedir}/processor_sdk/tiovx \
    LINUX_FS_PATH=${TARGET_FS} \
    CONCERTO_ROOT=${S}/concerto \
    TF_REPO_PATH=${S}/tensorflow \
    ONNX_REPO_PATH=${S}/onnxruntime \
    TIDL_PROTOBUF_PATH=${S}/protobuf \
    GCC_LINUX_ARM_ROOT= \
    TARGET_SOC=${PLAT_SOC} \
    # TI hardcodes Arago's target triple here. PHYTEC's ampliphy distro
    # builds aarch64-phytec-linux-, so use bitbake's own TARGET_PREFIX
    # rather than either vendor's literal.
    CROSS_COMPILE_LINARO=${TARGET_PREFIX} \
    LINUX_SYSROOT_ARM=${STAGING_DIR_TARGET} \
    TREAT_WARNINGS_AS_ERROR=0 \
    oe_runmake
}

LIB_DST_DIR="${D}${libdir}"
INC_DST_DIR="${D}${includedir}"
OPT_DST_DIR="${D}/opt"

TIDL_SOC_NAME = ""
TIDL_SOC_NAME:j721e = "J721E"
TIDL_SOC_NAME:j721s2 = "J721S2"
TIDL_SOC_NAME:j784s4 = "J784S4"
TIDL_SOC_NAME:j722s = "J722S"
TIDL_SOC_NAME:j742s2 = "J742S2"
TIDL_SOC_NAME:am62axx = "AM62A"

do_install() {
    install -d ${LIB_DST_DIR}
    cp ${S}/arm-tidl/rt/out/${TIDL_SOC_NAME}/${CPU}/LINUX/release/libvx_tidl_rt.so.1.0 ${LIB_DST_DIR}/
    ln -s -r ${LIB_DST_DIR}/libvx_tidl_rt.so.1.0 ${LIB_DST_DIR}/libvx_tidl_rt.so
    cp ${S}/arm-tidl/tfl_delegate/out/${TIDL_SOC_NAME}/${CPU}/LINUX/release/libtidl_tfl_delegate.so.1.0 ${LIB_DST_DIR}/
    ln -s -r ${LIB_DST_DIR}/libtidl_tfl_delegate.so.1.0 ${LIB_DST_DIR}/libtidl_tfl_delegate.so
    cp ${S}/arm-tidl/onnxrt_ep/out/${TIDL_SOC_NAME}/${CPU}/LINUX/release/libtidl_onnxrt_EP.so.1.0 ${LIB_DST_DIR}/
    ln -s -r ${LIB_DST_DIR}/libtidl_onnxrt_EP.so.1.0 ${LIB_DST_DIR}/libtidl_onnxrt_EP.so
    cp ${S}/arm-tidl/tidlrt_ep/out/${TIDL_SOC_NAME}/${CPU}/LINUX/release/libtidlrt_EP.so.1.0 ${LIB_DST_DIR}/
    ln -s -r ${LIB_DST_DIR}/libtidlrt_EP.so.1.0 ${LIB_DST_DIR}/libtidlrt_EP.so

    install -d ${INC_DST_DIR}
    cp ${S}/arm-tidl/rt/inc/itidl_rt.h  ${INC_DST_DIR}/
    cp ${S}/arm-tidl/rt/inc/itidl_io.h  ${INC_DST_DIR}/
    cp ${S}/arm-tidl/rt/inc/itvm_rt.h ${INC_DST_DIR}/

    install -d ${OPT_DST_DIR}/tidl_test
    cp ${S}/arm-tidl/rt/out/${TIDL_SOC_NAME}/${CPU}/LINUX/release/TI_DEVICE_armv8_test_dl_algo_host_rt.out ${OPT_DST_DIR}/tidl_test/

}

INSANE_SKIP:${PN} += "ldflags buildpaths"

# ONNX Runtime, the TFLite delegate, DLR and libgstti.so (tidlinferer) dlopen
# the TIDL libraries by their unversioned names, which OE would otherwise move
# into ${PN}-dev. Keep the .so links in the runtime package.
FILES_SOLIBSDEV = ""
INSANE_SKIP:${PN} += "dev-so"
