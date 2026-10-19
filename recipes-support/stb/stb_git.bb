# Lifted from TI meta-arago (meta-arago-extras/recipes-support/stb/stb_git.bb).
# ti-vision-apps 11.02 DEPENDS on stb, and no layer in a PHYTEC BSP provides
# it, so carry it here rather than pull in the meta-arago layers.

SUMMARY = "single-file public domain (or MIT licensed) libraries for C/C++"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://README.md;md5=b499fad62f2f8e336bbea84cc94d1e60"

PV = "2.30"
PE = "1"

BRANCH = "master"
SRCREV = "f1c79c02822848a9bed4315b12c8c8f3761e1296"

SRC_URI = " \
    git://github.com/nothings/stb.git;protocol=https;branch=${BRANCH} \
"

# wrynose sets S for git recipes in bitbake.conf and errors if a recipe sets
# it; the scarthgap branch of this layer adds S = "${WORKDIR}/git" instead.

do_install() {
    install -d ${D}${includedir}
    for hdr in ${S}/*.h
    do
        install -m 0644 $hdr ${D}${includedir}
    done
}
