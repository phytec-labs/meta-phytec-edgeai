# ROS 2 Jazzy runtime and gscam. Parsed only when meta-ros is in bblayers.conf,
# via BBFILES_DYNAMIC in conf/layer.conf.
IMAGE_INSTALL:append:am62axx = " packagegroup-edgeai-ros"
