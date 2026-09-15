DESCRIPTION = "ROS 2 Jazzy runtime and camera bridge for the Evolv pipeline"
LICENSE = "MIT"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

# Requires meta-ros (meta-ros-common, meta-ros2, meta-ros2-jazzy) on the
# scarthgap branch. ros-base matches the ros-jazzy-ros-base 0.11.0 set Evolv
# runs on Ubuntu 24.04.

RDEPENDS:${PN} = " \
    ros-base \
    gscam \
    demo-nodes-cpp \
"

COMPATIBLE_MACHINE = "am62axx"
