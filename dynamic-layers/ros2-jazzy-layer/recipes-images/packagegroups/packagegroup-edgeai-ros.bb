DESCRIPTION = "ROS 2 Jazzy runtime and camera bridge for the AM62Ax Edge AI demo"
LICENSE = "MIT"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

# Requires meta-ros (meta-ros-common, meta-ros2, meta-ros2-jazzy) on the
# scarthgap branch. Parsed only when that layer is present.
#

RDEPENDS:${PN} = " \
    ros-base \
    gscam \
    demo-nodes-cpp \
"

COMPATIBLE_MACHINE = "am62axx"
