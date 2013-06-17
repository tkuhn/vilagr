lagravis
========

Large graph visualisation based on Gephi.

**Under construction...**


Install Dependencies
--------------------

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

Version 0.8.2 of gephi-toolkit is required:

    mvn install:install-file \
      -DgroupId=org.gephi \
      -DartifactId=gephi-toolkit \
      -Dversion=0.8.2-all \
      -Dfile=~/Packages/gephi-toolkit-src/target/gephi-toolkit-0.8.2-all.jar \
      -Dpackaging=jar \
      -DgeneratePom=true
