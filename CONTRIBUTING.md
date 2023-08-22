## Getting started

* Please report any issues you encounter during operations.
* Feel free to create a pull request, preferably with a test or five.

## Setting up a development environment

### Requirements

* Clojure 1.11+
* OpenJDK 17
* Node.js 18.x
* Yarn

For testing: Docker Compose

Please refer to the extensive documentation available on the Metabase website: [Guide to writing a Metabase driver](https://www.metabase.com/docs/latest/developers-guide/drivers/start.html)

Materialize driver's code should be inside the main Metabase repository checkout in `modules/drivers/materialize` directory.

The easiest way to set up a development environment is as follows (mostly the same as in the [CI](https://github.com/MaterializeInc/metabase-materialize-driver/blob/master/.github/workflows/tests.yml)):

* Clone Metabase and Materialize driver repositories
```bash
git clone https://github.com/metabase/metabase.git
cd metabase
checkout v0.47.0
git clone https://github.com/MaterializeInc/metabase-materialize-driver.git modules/drivers/materialize
```

* Create custom Clojure profiles, you can get it using the following command:

```bash
cat modules/drivers/materialize/.github/deps.edn | sed -e "s|PWD|$PWD|g" | tr -d '\n'
```

Modifying `~/.clojure/deps.edn` will create two useful profiles: `user/materialize` that adds driver's sources to the classpath, and `user/test` that includes all the Metabase tests that are guaranteed to work with the driver.

* Install the Metabase dependencies:

```bash
clojure -X:deps:drivers prep
```

* Build the frontend:

```bash
yarn && yarn build-static-viz
```

* Add `/etc/hosts` entry

Required for TLS tests.

```bash
sudo -- sh -c "echo 127.0.0.1 materialize >> /etc/hosts"
```

* Start Materialize as a Docker container

```bash
docker compose -f modules/drivers/materialize/docker-compose.yml up -d materialize init
```

Now, you should be able to run the tests:

```bash
mz_deps=$(cat modules/drivers/materialize/.github/deps.edn | sed -e "s|PWD|$PWD|g" | tr -d '\n')
DRIVERS=materialize clojure -Sdeps ${mz_deps} -X:dev:drivers:drivers-dev:test:user/materialize
```

you can see that we have our profiles `:user/materialize:user/test` added to the command above, and with `DRIVERS=materialize` we instruct Metabase to run the tests only for Materialize.

> **Note** Omitting `DRIVERS` will run the tests for all the built-in database drivers.

If you want to run tests for only a specific test:

```bash
mz_deps=$(cat modules/drivers/materialize/.github/deps.edn | sed -e "s|PWD|$PWD|g" | tr -d '\n')
DRIVERS=materialize clojure -Sdeps ${mz_deps} -X:dev:drivers:drivers-dev:test:user/materialize :only metabase.query-processor.middleware.parameters.mbql-test
```

## Building a jar

You need to add an entry for Materialize in `modules/drivers/deps.edn`

```clj
{:deps
 {...
  metabase/materialize {:local/root "materialize"}
  ...}}
```

or just run this from the root Metabase directory, overwriting the entire file:

```bash
echo "{:deps {metabase/materialize {:local/root \"materialize\" }}}" > modules/drivers/deps.edn
```

Now, you should be able to build the final jar:

```bash
bin/build-driver.sh materialize
```

As the result, `resources/modules/materialize.metabase-driver.jar` should be created.

For smoke testing, there is a Metabase with the link to the driver available as a Docker container:

```bash
docker compose -f modules/drivers/materialize/docker-compose.yml up -d metabase
```

It should pick up the driver jar as a volume.
