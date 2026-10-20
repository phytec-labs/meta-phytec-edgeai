# TIDL model artifacts for the AM62Ax Edge AI demo, pinned to the model-zoo
# folder that matches this BSP's C7x firmware.
#
# This is deliberately NOT TI's meta-edgeai edgeai-tidl-models recipe. That one
# wgets download_models.sh inside do_fetch and pulls whichever folder its
# EDGEAI_SDK_VERSION names, regardless of the firmware on the board. A model
# carries the TIDL network version it was compiled for in the first word of
# its net.bin, and the C7x refuses anything else ("Network version - 0x...,
# Expected version - 0x..." in the C7x log). These two are fetched with pinned
# URLs and checksums instead. See docs/tidl-test.
#
# 11_02_02_00 artifacts are network version 0x20251208, matching the firmware
# PD25.1.1 ships and the arm-tidl the TI recipes pin.

SUMMARY = "TIDL model artifacts for the AM62Ax Edge AI demo"
DESCRIPTION = "Two TI Edge AI Model Zoo classification models compiled for the \
AM62A C7x: MobileNetV1 (TFLite) and RegNetX-200MF (ONNX Runtime), 8-bit."
LICENSE = "TI-TFL"
LIC_FILES_CHKSUM = "file://${COREBASE}/../meta-ti/meta-ti-bsp/licenses/TI-TFL;md5=a1b59cb7ba626b9dbbcbf00f3fbc438a"

PV = "11.02.02.00"

MODELZOO_URL = "https://software-dl.ti.com/jacinto7/esd/modelzoo/11_02_02_00/modelartifacts/AM62A/8bits"
TFL_MODEL = "cl-0000_tflitert_imagenet1k_mlperf_mobilenet_v1_1.0_224_tflite"
ONNX_MODEL = "cl-6360_onnxrt_imagenet1k_fbr-pycls_regnetx-200mf_onnx"

SRC_URI = " \
    ${MODELZOO_URL}/${TFL_MODEL}.tar.gz;name=tfl;subdir=model_zoo/${TFL_MODEL} \
    ${MODELZOO_URL}/${ONNX_MODEL}.tar.gz;name=onnx;subdir=model_zoo/${ONNX_MODEL} \
"
SRC_URI[tfl.sha256sum] = "e7caedce0200cba71c861d7cca6c431387f02230c379ce17c98fc622c09d41f8"
SRC_URI[onnx.sha256sum] = "5c175141dd52cc6150e71c195386c93d4d8538bed289c6408e053d2f3a19656c"

S = "${WORKDIR}/model_zoo"

do_install() {
    install -d ${D}/opt/model_zoo
    cp -R --no-preserve=ownership ${S}/. ${D}/opt/model_zoo/
}

FILES:${PN} += "/opt/model_zoo"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "am62axx"
