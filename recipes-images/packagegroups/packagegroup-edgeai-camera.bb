DESCRIPTION = "GStreamer camera / encode / stream stack for the AM62Ax Edge AI demo"
LICENSE = "MIT"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

# The GStreamer elements the demo pipelines need, split into two groups. Six are stock
# OE-core and are covered below. The remaining two (tiovxldc,
# tiovxmultiscaler) come from TI's meta-edgeai and are NOT installed here --
# see README.md for why that is a separate piece of work.
#
# Package names come from gstreamer1.0-plugins-packaging.inc, which emits one
# package per plugin .so: libgst<name>.so -> <recipe>-<name>.

REQUESTED_ELEMENTS = " \
    gstreamer1.0-plugins-base-tcp \
    gstreamer1.0-plugins-good-udp \
    gstreamer1.0-plugins-good-rtp \
    gstreamer1.0-plugins-good-video4linux2 \
"

# Core runtime. The gstreamer1.0 package carries gst-inspect-1.0 and
# gst-launch-1.0 -- there is no gstreamer1.0-tools package in OE-core.
GST_CORE = " \
    gstreamer1.0 \
    gstreamer1.0-plugins-base-app \
    gstreamer1.0-plugins-base-typefindfunctions \
    gstreamer1.0-plugins-base-videoconvertscale \
    gstreamer1.0-plugins-base-videorate \
    gstreamer1.0-plugins-base-videotestsrc \
"

# Needed to actually build and verify the pipelines: h264parse for the RTP
# payloader, multifilesink for the encoder soak test, jpeg for decoding what
# the hardware encoder produced, isomp4 for recording to a file.
GST_PIPELINE_SUPPORT = " \
    gstreamer1.0-plugins-bad-videoparsersbad \
    gstreamer1.0-plugins-good-multifile \
    gstreamer1.0-plugins-good-jpeg \
    gstreamer1.0-plugins-good-isomp4 \
    gstreamer1.0-plugins-good-matroska \
"

RDEPENDS:${PN} = " \
    ${REQUESTED_ELEMENTS} \
    ${GST_CORE} \
    ${GST_PIPELINE_SUPPORT} \
    v4l-utils \
    edgeai-camera-tools \
"
