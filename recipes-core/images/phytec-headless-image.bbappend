IMAGE_INSTALL:append:am62axx = " packagegroup-evolv-camera"

# TIOVX userspace (tiovxldc / tiovxmultiscaler). Separate packagegroup so it can
# be dropped without affecting the standard GStreamer stack -- it carries TI-TFL
# licensed components. Comment this line out to build Bucket 1 only.
IMAGE_INSTALL:append:am62axx = " packagegroup-evolv-tiovx"
