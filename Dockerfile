FROM metabase/metabase:latest
ADD https://github.com/MaterializeInc/metabase-materialize-driver/releases/download/0.0.1/materialize-driver-0.0.3-SNAPSHOT-standalone.jar /plugins/
RUN chmod 744 /plugins/materialize-driver-0.0.3-SNAPSHOT-standalone.jar
