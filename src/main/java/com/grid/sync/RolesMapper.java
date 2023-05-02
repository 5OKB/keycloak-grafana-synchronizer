package com.grid.sync;

import com.grid.grafana.entity.User;
import org.jboss.logging.Logger;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;

import java.util.HashMap;
import java.util.Map;

public class RolesMapper {
    private final Map<String, String> rolesMap = new HashMap<>();

    public RolesMapper(String roleGrafanaAdminValues, String roleOrgAdminValues, String roleOrgEditorValues, Logger logger) {
        this.setMapping(roleOrgEditorValues, User.ROLE_ORG_EDITOR);
        this.setMapping(roleOrgAdminValues, User.ROLE_ORG_ADMIN);
        this.setMapping(roleGrafanaAdminValues, User.ROLE_GRAFANA_ADMIN);

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

    public String getGrafanaRole(UserModel user) {
        // it returns the first matched role only
        for (RoleModel role : RoleUtils.getDeepUserRoleMappings(user)) {
            if (this.rolesMap.containsKey(role.getName())) {
                return this.rolesMap.get(role.getName());
            }
        }
        return User.ROLE_ORG_VIEWER;
    }
}

