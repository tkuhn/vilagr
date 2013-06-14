lagravis
========

Large graph visualisation based on Gephi.

**Under construction...**


Install OpenOrd
---------------

OpenOrd plugin has to be installed manually (build jar from sources first):

    mvn install:install-file \
      -DgroupId=org.gephi \
      -DartifactId=layout-plugin-openord \
      -Dversion=0.8.2 \
      -Dfile=~/Packages/gephi-plugins-src/build/cluster/modules/org-gephi-layout-plugin-openord.jar \
      -Dpackaging=jar \
      -DgeneratePom=true

(for some reason, this did not work, I had to copy the jar manually to the
local Maven repository...)
