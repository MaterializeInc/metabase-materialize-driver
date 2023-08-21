# Materialize driver for Metabase

The `metabase-materialize-driver` lets
[Metabase](https://github.com/metabase/metabase) connect to an instance of
[Materialize](https://github.com/MaterializeInc/materialize).

![Choose Materialize from database dropdown](images/materialize-metabase.png)

## To Use the Driver

We provide a pre-built docker image of metabase including this driver as
[materialize/metabase][]

To use the `metabase-materialize-driver` with an existing Metabase
installation, copy a `.jar` file from one of our [releases][] into the
`/plugins` directory of your Metabase instance. Metabase will register the
driver automatically! (For deployment-specific details, please consult the
following sections.)

Once the Materialize driver is registered, use the following information to
connect:

| Field             | Value     |
| ----------------- |:---------:|
| Host              | localhost |
| Port              | 6875      |
| Database username | default   |
| Database password | default   |
| Cluster name      | default   |

[releases]: https://github.com/MaterializeInc/metabase-materialize-driver/releases
[materialize/metabase]: https://hub.docker.com/repository/docker/materialize/metabase

## Choosing the Right Version

Metabase Release | Driver Version
---------------- | --------------
v0.46.7          | v0.1.0

## Contributing

Check out our [contributing guide](CONTRIBUTING.md) for more information on how
to contribute to this project.
