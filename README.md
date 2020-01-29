# metabase-materialize-driver

The `metabase-materialize-driver` allows [Metabase](https://github.com/metabase/metabase) 
to connect to an instance of Materialize.

![Choose Materialize from database dropdown](images/materialize-metabase.png)

## To Use the Driver

To use the `metabase-materialize-driver`, put a copy of the driver's
.jar file into the `/plugins` directory of your Metabase instance. Metabase
will register the driver automatically! (For deployment-specific details, 
please consult the following sections.)

Once the Materialize driver is registered, use the following information
to connect: 

| Field             | Value     |
| ----------------- |:---------:|
| Host              | localhost |
| Port              | 6875      |
| Database username | default   |
| Database password | default   |


### With Docker

To generate a Metabase Docker image containing the Materialize driver,
use [our Dockerfile](Dockerfile):
```shell script
FROM metabase/metabase:latest
ADD https://github.com/MaterializeInc/metabase-materialize-driver/releases/download/0.0.1/materialize-driver-0.0.1-SNAPSHOT-standalone.jar /plugins/
RUN chmod 744 /plugins/materialize-driver-0.0.1-SNAPSHOT-standalone.jar
```

And run the following from your terminal to generate an image with
the tag `metabase-with-materialize:latest`:
```shell script
cd /path/to/metabase-materialize-driver
docker build -f Dockerfile -t metabase-with-materialize .
```

Then, to start the container, run:
```shell script
docker run --rm -p 3000:3000 --name metabase metabase-with-materialize:latest
```

Once it's finished loading, you should be able to access Metabase at 
`localhost:3000` in your browser.

**NB: If you are running this on a Mac and running Materialize locally, 
you can connect by substituting the following connection information:**

| Field             | Value                |
| ----------------- |:--------------------:|
| Host              | [host.docker.internal](https://stackoverflow.com/questions/24319662/from-inside-of-a-docker-container-how-do-i-connect-to-the-localhost-of-the-mach/24326540#24326540) |


### Without Docker

Use this driver in two steps:
1. Download a copy of the driver by running:
   ```shell script
   curl -L "https://github.com/MaterializeInc/metabase-materialize-driver/releases/download/0.0.1/materialize-driver-0.0.1-SNAPSHOT-standalone.jar" -o materialize-driver-0.0.1-SNAPSHOT-standalone.jar
   ```
2. Move the downloaded copy of the driver (from the previous step)
   into the `/plugins` directory of your Metabase instance. 

For more info, check out these resources:
* [Managing databases in Metabase](https://www.metabase.com/docs/latest/administration-guide/01-managing-databases.html)
* [Driver plugins in Metabase](https://github.com/metabase/metabase/wiki/Writing-a-Driver:-Packaging-a-Driver-&-Metabase-Plugin-Basics)  
   
## To Build the Driver

**NB: These steps are not necessary to use the `metabase-materialize-driver`!
They are only for the curious or those attempting to make updates. To just use
the driver, stick to `How to use the driver` above.**

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
