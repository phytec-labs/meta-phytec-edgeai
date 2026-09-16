require recipes-images/images/phytec-headless-image.bb

SUMMARY = "PHYTEC Edge AI demo image"
DESCRIPTION = "phytec-headless-image plus the camera, TIOVX and TIDL stacks used \
by the AM62Ax Edge AI demo. ROS 2 is added when meta-ros is present."

# GStreamer capture, hardware JPEG / H.264 encode and streaming, plus the
# edgeai-camera-probe and edgeai-camera-test scripts.
IMAGE_INSTALL:append:am62axx = " packagegroup-edgeai-camera"

# TI TIOVX and TIDL userspace: tiovxldc, tiovxmultiscaler, and the deep learning
# runtime that offloads models to the C7x. TI-TFL licensed; drop this line for a
# camera-only image.
IMAGE_INSTALL:append:am62axx = " packagegroup-edgeai-tiovx"
