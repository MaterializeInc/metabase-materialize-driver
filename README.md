# metabase-materialize-driver

This repo contains the code necessary to connect
Metabase to Materialize. 

## How to use the driver



## How the driver is built

### Step 1: Use the forked pgjdbc Driver to connect to Materialize

Connecting to Materialize is nearly identical* to connecting
to PostgreSQL. For that reason, we use a slightly modified 
[pgjdbc fork](https://github.com/MaterializeInc/pgjdbc) to provide 
the underlying SQL Driver.

To use the forked Driver, we go through the following steps:
* Check out the forked pgjdbc repo locally
* Build a shaded jar with:
  ```shell script
  mvn clean && mvn package -DskipTests -Dmaven.javadoc.skip=true -P release-artifacts
  ``` 
* Check out this repo locally and move the resulting jar into
 the `/src` folder.
 ```shell script
 mv /path/to/postgresql-1-MZ-SNAPSHOT.jar /path/to/metabase-materialize-driver/src/
 ``` 
* Extract the files from the jarfile **into** the `/src` directory
 ```shell script
 tar -xvf postgresql-1-MZ-SNAPSHOT.jar
 ```
* Remove the resulting `META-INF/` directory. 
 ```shell script
 rm -rf /path/to/metabase-materialize-driver/src/META-INF/
 ```

### Step 2: Build and move the metabase-materialize-driver 

Once we've successfully completed copying over the code from the
forked Driver, we're ready to build the `metabase-materialize-driver`.
This step is simple! We just run:
```shell script
cd /path/to/metabase-materialize-driver
lein clean && lein uberjar
```

When the `uberjar` command is finished executing, it will output
the path to the newly created uberjar like: 
```shell script
Jessicas-MacBook-Pro:metabase-materialize-driver jessicalaughlin$ lein clean && lein uberjar
Compiling metabase.driver.materialize
Created /path/to/metabase-materialize-driver/target/materialize-driver-0.0.1-SNAPSHOT.jar
Created /path/to/metabase-materialize-driver/target/materialize-driver-0.0.1-SNAPSHOT-standalone.jar
```

We then can move the `standalone.jar` (the uberjar) over to 
the `/plugin` directory of our local copy of Metabase like:
```shell script
cp /path/to/metabase-materialize-driver/target/materialize-driver-0.0.1-SNAPSHOT-standalone.jar /path/to/metabase/plugins/
```

Once the `metabase-materialize-driver` jar is in the `/plugins` directory,
Metabase will register the plugin on startup! 
