lagravis
========

Large graph visualization based on Gephi.

**Under construction...**


Install Dependencies
--------------------

Build Gephi toolkit 0.8.2 from sources and install with Maven:

    mvn install:install-file \
      -DgroupId=org.gephi \
      -DartifactId=gephi-toolkit \
      -Dversion=0.8.2-all \
      -Dfile=~/Packages/gephi-toolkit-src/target/gephi-toolkit-0.8.2-all.jar \
      -Dpackaging=jar \
      -DgeneratePom=true

Build OpenOrd plugin from sources and install with Maven:

    mvn install:install-file \
      -DgroupId=org.gephi \
      -DartifactId=layout-plugin-openord \
      -Dversion=0.8.2 \
      -Dfile=~/Packages/gephi-plugins-src/build/cluster/modules/org-gephi-layout-plugin-openord.jar \
      -Dpackaging=jar \
      -DgeneratePom=true


Build
-----

Run from sources:

    scripts/Lagravis.sh inputfile.csv

Create single jar file that includes all dependencies:

    mvn clean compile assembly:single
