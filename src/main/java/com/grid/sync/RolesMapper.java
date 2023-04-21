package com.grid.sync;

import com.grid.grafana.entity.User;
import org.jboss.logging.Logger;
import org.keycloak.models.RoleModel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RolesMapper {
    private final Map<String, String> rolesMap = new HashMap<>();

    public RolesMapper(String roleAdminValues, String roleEditorValues, Logger logger) {
        this.setMapping(roleEditorValues, User.ROLE_EDITOR);
        this.setMapping(roleAdminValues, User.ROLE_ADMIN);

        logger.infof("Roles mapping: %s \n", this.rolesMap.toString());
    }

    private void setMapping(String keycloakRolesList, String grafanaRole) {
        if (keycloakRolesList == null) {
            return;
        }
        for (String keycloakRole : keycloakRolesList.split(",")) {
            keycloakRole = keycloakRole.trim();
            if (!keycloakRole.isEmpty()) {
                this.rolesMap.put(keycloakRole, grafanaRole);
            }
        }
    }

    public String getGrafanaRole(Stream<RoleModel> roles) {
        // it returns the first matched role only
        for (String value : roles.map(RoleModel::getName).toArray(String[]::new)) {
            if (this.rolesMap.containsKey(value)) {
                return this.rolesMap.get(value);
            }
        }
        return User.ROLE_VIEWER;
    }
}

