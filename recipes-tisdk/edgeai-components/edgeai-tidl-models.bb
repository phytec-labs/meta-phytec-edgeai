# TIDL model artifacts for the AM62Ax Edge AI demo, pinned to the model-zoo
# folder that matches this BSP's C7x firmware.
#
# This is deliberately NOT TI's meta-edgeai edgeai-tidl-models recipe. That one
# wgets download_models.sh inside do_fetch and pulls EDGEAI_SDK_VERSION
# 11_01_00, whose artifacts are built for TIDL network version 0x20250821 and
# are rejected by the 11_01_00_00 firmware PD25.1.0 ships ("Network version -
# 0x20250821, Expected version - 0x20250630" in the C7x log). These two models
# are fetched with pinned URLs and checksums instead. See docs/tidl-test.

SUMMARY = "TIDL model artifacts for the AM62Ax Edge AI demo"
DESCRIPTION = "Two TI Edge AI Model Zoo classification models compiled for the \
AM62A C7x: MobileNetV1 (TFLite) and RegNetX-200MF (ONNX Runtime), 8-bit."
LICENSE = "TI-TFL"
LIC_FILES_CHKSUM = "file://${COREBASE}/../meta-ti/meta-ti-bsp/licenses/TI-TFL;md5=a1b59cb7ba626b9dbbcbf00f3fbc438a"

PV = "11.01.00.00"

MODELZOO_URL = "https://software-dl.ti.com/jacinto7/esd/modelzoo/11_01_00_00/modelartifacts/AM62A/8bits"
TFL_MODEL = "cl-0000_tflitert_imagenet1k_mlperf_mobilenet_v1_1.0_224_tflite"
ONNX_MODEL = "cl-6360_onnxrt_imagenet1k_fbr-pycls_regnetx-200mf_onnx"

SRC_URI = " \
    ${MODELZOO_URL}/${TFL_MODEL}.tar.gz;name=tfl;subdir=model_zoo/${TFL_MODEL} \
    ${MODELZOO_URL}/${ONNX_MODEL}.tar.gz;name=onnx;subdir=model_zoo/${ONNX_MODEL} \
"
SRC_URI[tfl.sha256sum] = "4d173ba1c2b8231a6b8921bd7259f96b2ed4381bec65ee41d7988bec3f245b5f"
SRC_URI[onnx.sha256sum] = "69d4c75cce446ae1211a38cfbf87be395ff05d0f5fe2af8f03e0cc82f6bd5e32"

S = "${WORKDIR}/model_zoo"

do_install() {
    install -d ${D}/opt/model_zoo
    cp -R --no-preserve=ownership ${S}/. ${D}/opt/model_zoo/
}

FILES:${PN} += "/opt/model_zoo"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "am62axx"
