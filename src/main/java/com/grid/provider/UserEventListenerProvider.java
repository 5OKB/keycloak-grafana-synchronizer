package com.grid.provider;

import com.grid.sync.UserGrafanaSynchronizer;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

public class UserEventListenerProvider implements EventListenerProvider {
    private final UserGrafanaSynchronizer userGrafanaSynchronizer;
    private final KeycloakSession session;
    private final RealmProvider model;
    private final String oauthClientId;
    private final Logger logger;

    public UserEventListenerProvider(KeycloakSession session, UserGrafanaSynchronizer userGrafanaSynchronizer, String oauthClientId, Logger logger) {
        this.userGrafanaSynchronizer = userGrafanaSynchronizer;
        this.session = session;
        this.model = session.realms();
        this.oauthClientId = oauthClientId;
        this.logger = logger;
    }

    @Override
    public void onEvent(Event event) {
        if (EventType.LOGIN.equals(event.getType()) && event.getClientId().equals(this.oauthClientId)) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel user = this.session.users().getUserById(realm, event.getUserId());
            this.userGrafanaSynchronizer.sync(user);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
    }

    @Override
    public void close() {
    }
}
