package com.grid.provider;

import com.grid.grafana.Client;
import com.grid.grafana.HttpClient;
import com.grid.sync.RolesMapper;
import com.grid.sync.UserGrafanaSynchronizer;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserEventListenerProviderFactory implements EventListenerProviderFactory {
    UserGrafanaSynchronizer userGrafanaSynchronizer;

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UserEventListenerProvider(keycloakSession, this.userGrafanaSynchronizer, System.getenv("GF_OAUTH_CLIENT_ID"), Logger.getLogger(UserEventListenerProvider.class));
    }

    @Override
    public void init(Config.Scope scope) {
        Logger logger = Logger.getLogger(UserEventListenerProviderFactory.class);
        logger.info("Init " + this.getId());

        try {
            HttpClient httpClient = new HttpClient(System.getenv("GF_ADMIN_API_URL"), System.getenv("GF_ADMIN_API_USER"), System.getenv("GF_ADMIN_API_PASSWORD"), Logger.getLogger(HttpClient.class));
            Client grafanaClient = new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));
            RolesMapper rolesMapper = new RolesMapper(System.getenv("GF_ROLE_ADMIN_MAPPING"), System.getenv("GF_ROLE_EDITOR_MAPPING"), Logger.getLogger(RolesMapper.class));
            this.userGrafanaSynchronizer = new UserGrafanaSynchronizer(grafanaClient, rolesMapper, Logger.getLogger(UserGrafanaSynchronizer.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "grafana-synchronizer";
    }
}
