# Materialize driver for Metabase

[![Slack Badge](https://img.shields.io/badge/Join%20us%20on%20Slack!-blueviolet?style=flat&logo=slack&link=https://materialize.com/s/chat)](https://materialize.com/s/chat)

The `metabase-materialize-driver` lets
[Metabase](https://github.com/metabase/metabase) connect to an instance of
[Materialize](https://github.com/MaterializeInc/materialize).

![Choose Materialize from database dropdown](https://github-production-user-asset-6210df.s3.amazonaws.com/21223421/268976090-6ed5b4b0-abb0-48dc-862f-b2284cb878d7.png)

## To Use the Driver

### With Metabase Cloud

As a Metabase partner driver, the Materialize driver is
included in [Metabase Cloud](https://www.metabase.com/docs/latest/cloud/start) offering.
To use it, simply select Materialize from the database dropdown when adding a new database.

### With Docker

We provide a pre-built Docker image of Metabase including this driver as
[materialize/metabase][] on Docker Hub. To use it, run:

```bash
docker run -p 3000:3000 materialize/metabase
```

### With self-hosted Metabase

To use the `metabase-materialize-driver` with an existing Metabase
installation, copy a `.jar` file from one of our [releases][] into the
`/plugins` directory of your Metabase instance. Metabase will register the
driver automatically! (For deployment-specific details, please consult the
following sections.)

### Connecting to Materialize

Select Materialize from the database dropdown when adding a new database.
Next, use the following information to connect:

| Field             | Value                  |
| ----------------- | ---------------------- |
| Database type     | **Materialize**        |
| Host              | Materialize host name. |
| Port              | **6875**               |
| Database name     | **materialize**        |
| Cluster name      | **quickstart**         |
| Database username | Materialize user.      |
| Database password | App-specific password. |
| SSL               | **Enabled**            |

[releases]: https://github.com/MaterializeInc/metabase-materialize-driver/releases
[materialize/metabase]: https://hub.docker.com/repository/docker/materialize/metabase

## Choosing the Right Version

Metabase Release | Driver Version
---------------- | --------------
v0.46.7          | v0.1.0
v0.47.0          | v1.0.0
v0.47.1          | v1.0.1 <br> v1.0.2 <br> v1.0.3
v0.49.12         | v1.1.0
v0.50.10         | v1.2.0 <br> v1.2.1
v0.51.11         | v1.3.0
v0.52.6          | v1.4.0

## Contributing

Check out our [contributing guide](CONTRIBUTING.md) for more information on how
to contribute to this project.
