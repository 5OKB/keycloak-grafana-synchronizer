# Keycloak Extension - Grafana Synchronizer

Sync users from keycloak to grafana (groups -> organisations).

EventListenerProvider which listens `LOGIN` event of specified `ClientId` and sync the user to grafana where user groups -> grafana organisations and roles to grafana role

## Env variables for configuration

Specify your Grafana api endpoint and Admin user credentials:
```
GF_ADMIN_API_URL=http://[GrafanaUrl]/api/
GF_ADMIN_API_USER=[User]
GF_ADMIN_API_PASSWORD=[Password]
```

Your grafana OAUTH ClientId and roles mapping:
```
GF_OAUTH_CLIENT_ID=[ClientId]
GF_ROLE_GRAFANA_ADMIN_MAPPING=[Your coma separated keycloak roles]
GF_ROLE_ORG_ADMIN_MAPPING=[Your coma separated keycloak roles]
GF_ROLE_ORG_EDITOR_MAPPING=[Your coma separated keycloak roles]
```

## Requirements

* Docker and docker-composer

## Run test

```
$ make test
```

## Run build

```
$ make build
```
the result of the build you can find in the `target` folder


