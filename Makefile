all::

PREFIX=/usr/local
FIND=find
SED=sed
XARGS=xargs
INSTALL=install
MKDIR=mkdir -p
CMP=cmp -s
CP=cp

VWORDS:=$(shell src/getversion.sh --prefix=v MAJOR MINOR PATCH)
VERSION:=$(word 1,$(VWORDS))
BUILD:=$(word 2,$(VWORDS))

myblank:=
myspace:=$(myblank) $(myblank)
MYCURDIR:=$(subst $(myspace),\$(myspace),$(CURDIR)/)
MYABSPATH=$(foreach f,$1,$(if $(patsubst /%,,$f),$(MYCURDIR)$f,$f))

-include $(call MYABSPATH,config.mk)
-include jedges-env.mk

JARDEPS_SRCDIR=src/java/tree
JARDEPS_DEPDIR=src/java
JARDEPS_MERGEDIR=src/java/merge

SELECTED_JARS += edges
SELECTED_JARS += voronoi

roots_voronoi=$(found_voronoi)

jars += $(SELECTED_JARS)
trees_edges += lib
roots_lib=$(found_lib)

SELECTED_JARS += edges_apps
trees_edges_apps += apps
roots_apps=$(found_apps)
deps_apps += lib

version_edges=$(file <VERSION)
version_edges_apps=$(file <VERSION)
version_voronoi=$(file <VERSION)

-include jardeps.mk
-include jardeps-install.mk

DOC_PKGS += uk.ac.lancs.voronoi
DOC_PKGS += uk.ac.lancs.edges
DOC_PKGS += uk.ac.lancs.edges.rect
DOC_OVERVIEW=src/java/overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(jardeps_srcdirs)
DOC_CORE=jedges$(DOC_CORE_SFX)

tidy::
	@$(PRINTF) "Removing back-up files...\n"
	@$(FIND) . -name "*~" -delete

clean:: tidy

all:: VERSION BUILD installed-jars
installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%.jar)
installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%-src.zip)

MYCMPCP=$(CMP) -s '$1' '$2' || $(CP) '$1' '$2'
.PHONY: prepare-version
mktmp:
	@$(MKDIR) tmp/
prepare-version: mktmp
	$(file >tmp/BUILD,$(BUILD))
	$(file >tmp/VERSION,$(VERSION))
BUILD: prepare-version
	@$(call MYCMPCP,tmp/BUILD,$@)
VERSION: prepare-version
	@$(call MYCMPCP,tmp/VERSION,$@)

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))

install-jars:: $(SELECTED_JARS:%=install-jar-%)

install:: install-jars

YEARS=2016

update-licence:
	$(FIND) . -name ".git" -prune -or -type f -print0 | $(XARGS) -0 \
	$(SED) -i 's/Copyright (c)\s[-0-9,]\+\sLancaster University/Copyright (c) $(YEARS), Lancaster University/g'

distclean: blank
	$(RM) VERSION BUILD
