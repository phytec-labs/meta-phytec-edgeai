# Extracted verbatim from TI meta-edgeai at tag 11.01.05.03 (SDK 11.01),
# to match this BSP's PD25.1.0 kernel (6.12.35-11.01.05-phy6). Taking the
# recipes directly avoids pulling in meta-edgeai's four meta-arago layer deps.

SUMMARY = "OpenVX Middleware library"
DESCRIPTION = "Builds tivision_apps user space library"

PR:append = "_edgeai_13"

PV = "11.01.00"

LICENSE = "TI-TFL & BSD-2-Clause & BSD-3-Clause & BSD-4-Clause & MIT & Apache-2.0 & Apache-2.0-with-LLVM-exception & \
           Khronos & Hewlett-Packard & Patrick-Powell & FTL & Zlib & CC0-1.0 & OpenSSL"

LIC_FILES_CHKSUM = "file://${COREBASE}/../meta-ti/meta-ti-bsp/licenses/TI-TFL;md5=a1b59cb7ba626b9dbbcbf00f3fbc438a \
                    file://${COMMON_LICENSE_DIR}/BSD-2-Clause;md5=cb641bc04cda31daea161b1bc15da69f \
                    file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9 \
                    file://${COMMON_LICENSE_DIR}/BSD-4-Clause;md5=624d9e67e8ac41a78f6b6c2c55a83a2b \
                    file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302 \
                    file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10 \
                    file://${COMMON_LICENSE_DIR}/Apache-2.0-with-LLVM-exception;md5=0bcd48c3bdfef0c9d9fd17726e4b7dab \
                    file://${WORKDIR}/repo/tiovx/include/VX/vx.h;beginline=1;endline=15;md5=37315206223081f32a5b9aaaf912f637 \
                    file://${COREBASE}/../meta-ti/meta-ti-extras/licenses/Hewlett-Packard;md5=a07676ee09f5bfec457eb5ea75921d01 \
                    file://${COREBASE}/../meta-ti/meta-ti-extras/licenses/Patrick-Powell;md5=7e10716f13cff502f3cf6ebf8fe29c1e \
                    file://${COMMON_LICENSE_DIR}/FTL;md5=f0bf6b09ee8b02121ed10709d9e49d8b \
                    file://${COMMON_LICENSE_DIR}/Zlib;md5=87f239f408daca8a157858e192597633 \
                    file://${COMMON_LICENSE_DIR}/CC0-1.0;md5=0ceb3372c9595f0a8067e55da801e4a1 \
                    file://${COREBASE}/meta/files/common-licenses/OpenSSL;md5=4eb1764f3e65fafa1a25057f9082f2ae \
                    "

SRC_URI = "repo://git.ti.com/git/processor-sdk/psdk_repo_manifests.git;protocol=https;branch=refs/tags/REL.PSDK.ANALYTICS.11.00.00.06;manifest=vision_apps_yocto.xml"
SRC_URI:am62axx = "repo://git.ti.com/git/processor-sdk/psdk_repo_manifests.git;protocol=https;branch=refs/tags/REL.PSDK.ANALYTICS.AM62A.11.01.00.03;manifest=vision_apps_yocto_am62a.xml"

FILES:${PN} += "/opt/*"

#PTK needs:
# EGL/egl.h
# glm/glm.hpp
# IL/il.h
# /usr/include/freetype2/ft2build.h
# ti_rpmsg_char.h
# dlr.h

DEPENDS = "glm freetype ti-rpmsg-char repo-native virtual/egl libpam"

COMPATIBLE_MACHINE = "j721e|j721s2|j784s4|j722s|j742s2|am62axx"

PLAT_SOC = ""
PLAT_SOC:j721e = "j721e"
PLAT_SOC:j721s2 = "j721s2"
PLAT_SOC:j784s4 = "j784s4"
PLAT_SOC:j722s = "j722s"
PLAT_SOC:j742s2 = "j742s2"
PLAT_SOC:am62axx = "am62a"

S = "${WORKDIR}/repo/sdk_builder"

do_fetch[depends] += "repo-native:do_populate_sysroot"

CLEANBROKEN = "1"

do_compile() {
    # TI hardcodes Arago's target triple here. PHYTEC's ampliphy distro
    # builds aarch64-phytec-linux-, so use bitbake's own TARGET_PREFIX
    # rather than either vendor's literal.
    CROSS_COMPILE_LINARO=${TARGET_PREFIX} \
    LINUX_SYSROOT_ARM=${STAGING_DIR_TARGET} \
    TREAT_WARNINGS_AS_ERROR=0 \
    GCC_LINUX_ARM_ROOT= \
    GCC_LINUX_ARM_ROOT_A72= \
    LINUX_FS_PATH=${STAGING_DIR_TARGET} \
    SOC=${PLAT_SOC} \
    oe_runmake yocto_build
}

do_install() {
    SOC=${PLAT_SOC} LINUX_FS_STAGE_PATH=${D} oe_runmake yocto_install
}

INSANE_SKIP:${PN} += "ldflags buildpaths"
