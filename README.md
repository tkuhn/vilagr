Vilagr
======

Visualization of large graphs based on Gephi.

**Under construction...**


Install Dependencies
--------------------

(Update on 6 October 2015:
Check this reply, maybe the things below can now be done in an easier way:
https://github.com/gephi/gephi-toolkit/issues/2#issuecomment-145868185
)

Build Gephi toolkit 0.8.2 from sources and install with Maven:

    mvn install:install-file \
      -DgroupId=org.gephi \
      -DartifactId=gephi-toolkit \
      -Dversion=0.8.2-all \
      -Dfile=gephi-toolkit-src/target/gephi-toolkit-0.8.2-all.jar \
      -Dpackaging=jar \
      -DgeneratePom=true

Build OpenOrd plugin from sources and install with Maven:

    mvn install:install-file \
      -DgroupId=org.gephi \
      -DartifactId=layout-plugin-openord \
      -Dversion=0.8.2 \
      -Dfile=gephi-plugins-src/build/cluster/modules/org-gephi-layout-plugin-openord.jar \
      -Dpackaging=jar \
      -DgeneratePom=true


Build
-----

Run from sources:

    scripts/Vilagr.sh params.properties

Create single jar file that includes all dependencies:

    mvn clean compile assembly:single
