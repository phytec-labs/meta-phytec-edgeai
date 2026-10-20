# Extracted verbatim from TI meta-edgeai at tag REL.PSDK.ANALYTICS.11.02.00.03.
# Its arm-tidl (67eba49d) expects C7x firmware 11_02_02_00, TIDL network
# version 0x20251208: the generation PD25.1.1 (ti-linux-firmware 8ec0c42b) and
# PD26.1.0 (9dffeff8) ship. Newer meta-edgeai tags expect 11_02_20_00
# (0x20260819) and that C7x would reject their networks. Taking the recipes
# directly avoids meta-edgeai's four meta-arago layer deps.

SUMMARY = "Open Source DL/ML runtime Modules"
DESCRIPTION = "Open Source DL/ML runtime Modules like TF-LITE, ONNX and TVM Runtime. Supports both Python and CPP APIs"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# The SRC_URI entries below unpack with subdir=${S}/..., and bitbake requires
# that to sit inside the unpack root. On wrynose that root is ${WORKDIR}/sources,
# so ${WORKDIR}/src is rejected. UNPACKDIR is ${WORKDIR} on scarthgap, which
# keeps TI's original layout there.
S = "${UNPACKDIR}/src"
PR:append = "_edgeai_5"

SRC_URI = "https://software-dl.ti.com/jacinto7/esd/tidl-tools/11_02_02_00/OSRT_TOOLS/ARM_LINUX/ARAGO/tflite_runtime-2.12.0-cp312-cp312-linux_aarch64.whl;name=tflite;subdir=${S}/tflite\
           https://software-dl.ti.com/jacinto7/esd/tidl-tools/11_02_02_00/OSRT_TOOLS/ARM_LINUX/ARAGO/onnxruntime_tidl-1.15.0-cp312-cp312-linux_aarch64.whl;name=ort;subdir=${S}/ort\
           https://software-dl.ti.com/jacinto7/esd/tidl-tools/11_02_02_00/OSRT_TOOLS/ARM_LINUX/ARAGO/tflite_2.12_aragoj7.tar.gz;name=tfl_lib;subdir=${S}/tfl_lib\
           https://software-dl.ti.com/jacinto7/esd/tidl-tools/11_02_02_00/OSRT_TOOLS/ARM_LINUX/ARAGO/onnx_1.15.0_aragoj7.tar.gz;name=ort_lib;subdir=${S}/ort_lib\
           https://software-dl.ti.com/jacinto7/esd/tidl-tools/11_02_02_00/OSRT_TOOLS/ARM_LINUX/ARAGO/tvm-0.18.0-cp312-cp312-linux_aarch64.whl;name=tvm;subdir=${S}/tvm\
           https://software-dl.ti.com/jacinto7/esd/tidl-tools/11_02_02_00/OSRT_TOOLS/ARM_LINUX/ARAGO/tidlruntime-0.1.0-cp312-cp312-linux_aarch64.whl;name=tidlrt;subdir=${S}/tidlrt\
           "

SRC_URI[tflite.sha256sum] = "94c5f0ccbd5458cfa1327b378c7d479dc7d23979df8f26f091720f850dc02364"
SRC_URI[ort.sha256sum] = "38c9953b6bef83f6e92012412fe0818dea5741caa790d70c19328bd88fca3056"
SRC_URI[tfl_lib.sha256sum] = "2ff6878f51595395d84830747da6a8ddbb168eab93e84edd9e5f75cfb33b6b55"
SRC_URI[ort_lib.sha256sum] = "f47dd643168eb330e6849fa60dffc48c6f43cb3f63cfd9079921684795817e3f"
SRC_URI[tidlrt.sha256sum] = "384d825a362db72411c10eccacfbbef5d4b487c159982a17e2788bb724c5a51e"
SRC_URI[tvm.sha256sum] = "bd687b3b993a796ed1cc5fafea6148556e1d5b6bd7f0fced071604fb712413f3"

do_cp_downloaded_build_deps() {
    mv ${S}/tfl_lib/*/* ${S}/tfl_lib
    mv ${S}/ort_lib/*/* ${S}/ort_lib
}
addtask cp_downloaded_build_deps after do_unpack before do_patch

DEPENDS += "unzip-native cnpy"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j722s|j742s2|am62axx"

inherit python3-dir

FILES:${PN}-staticdev += "${libdir}/tflite_2.12/"
FILES:${PN}-staticdev += "${libdir}/*.a"
FILES:${PN}-staticdev += "${PYTHON_SITEPACKAGES_DIR}/tidlruntime/lib/*.a"
FILES:${PN} += "${libdir}/*.so*"
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"
FILES:${PN} += "${includedir}"

INSANE_SKIP:${PN} += "already-stripped"

do_install() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    unzip -d ${D}${PYTHON_SITEPACKAGES_DIR} ${S}/tflite/tflite_runtime-2.12.0-cp312-cp312-linux_aarch64.whl
    unzip -d ${D}${PYTHON_SITEPACKAGES_DIR} ${S}/ort/onnxruntime_tidl-1.15.0-cp312-cp312-linux_aarch64.whl
    unzip -d ${D}${PYTHON_SITEPACKAGES_DIR} ${S}/tidlrt/tidlruntime-0.1.0-cp312-cp312-linux_aarch64.whl
    unzip -d ${D}${PYTHON_SITEPACKAGES_DIR} ${S}/tvm/tvm-0.18.0-cp312-cp312-linux_aarch64.whl

    install -d ${D}${includedir}
    install -d ${D}${libdir}

    cp -r ${S}/tfl_lib/tensorflow  ${D}${includedir}/
    cp -r ${S}/tfl_lib/tflite_2.12  ${D}${libdir}/
    cp ${S}/tfl_lib/libtensorflow-lite.a ${D}${libdir}/

    cp   ${S}/ort_lib/libonnxruntime.so.1.15.0  ${D}${libdir}/
    ln -s -r ${D}${libdir}/libonnxruntime.so.1.15.0 ${D}${libdir}/libonnxruntime.so
    rm -rf  ${S}/ort_lib/onnxruntime/csharp
    cp -r  ${S}/ort_lib/onnxruntime ${D}${includedir}/

    ln -s -r ${D}${PYTHON_SITEPACKAGES_DIR}/tidlruntime/include ${D}${includedir}/tidlruntime
    ln -s -r ${D}${PYTHON_SITEPACKAGES_DIR}/tidlruntime/lib/libtidlruntime.a ${D}${libdir}/libtidlruntime.a

    install -d ${D}${includedir}/tvm/tvm
    ln -s -r ${D}${PYTHON_SITEPACKAGES_DIR}/tvm/libtvm.so ${D}${libdir}/libtvm.so
    ln -s -r ${D}${PYTHON_SITEPACKAGES_DIR}/tvm/libtvm_runtime.so ${D}${libdir}/libtvm_runtime.so
    cd ${D}${PYTHON_SITEPACKAGES_DIR}/tvm/
    cp --parents $(find . -name "*.h*") ${D}${includedir}/tvm/tvm
    cd -
}

