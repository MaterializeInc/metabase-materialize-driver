FROM metabase/metabase:latest

ADD target/dist/materialize-driver.jar /plugins/
RUN chmod 744 /plugins/materialize-driver.jar
