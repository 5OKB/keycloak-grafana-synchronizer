package com.grid.sync;

import com.grid.grafana.Client;
import com.grid.grafana.entity.Organization;
import com.grid.grafana.entity.User;
import com.grid.grafana.entity.UserOrganization;
import com.grid.grafana.exception.ClientException;
import com.grid.sync.exception.SyncException;
import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class UserGrafanaSynchronizer {
    private final Client grafanaClient;
    private final RolesMapper rolesMapper;
    private final Logger logger;

    public UserGrafanaSynchronizer(Client grafanaClient, RolesMapper rolesMapper, Logger logger) {
        this.grafanaClient = grafanaClient;
        this.rolesMapper = rolesMapper;
        this.logger = logger;
    }

    private static String[] buildGroupsNames(Stream<GroupModel> groups) {
        return groups.map(GroupModel::getName).toArray(String[]::new);
    }

    private static Map<Integer, UserOrganization> buildUserOrgsMap(List<UserOrganization> list) {
        Map<Integer, UserOrganization> map = new HashMap<>();
        for (UserOrganization userOrg : list) {
            map.put(userOrg.orgId, userOrg);
        }
        return map;
    }

    public void sync(UserModel user) throws SyncException {
        this.sync(user.getId(), user.getEmail(), this.rolesMapper.getGrafanaRole(user), buildGroupsNames(user.getGroupsStream()));
    }

    private void sync(String keycloakUserId, String email, String targetGrafanaRole, String[] groupNames) throws SyncException {
        try {
            this.logger.infof("Starting Grafana synchronization for Keycloak user %s (%s)", keycloakUserId, email);
            User grafanaUser = this.findOrCreateGrafanaUser(email);

            this.logger.infof("Resolved Grafana role %s for user %s", targetGrafanaRole, email);

            this.syncGrafanaUserPermissions(grafanaUser, targetGrafanaRole);

            String targetGrafanaOrgRole = targetGrafanaRole.equals(User.ROLE_GRAFANA_ADMIN) ? User.ROLE_ORG_ADMIN : targetGrafanaRole;
            if (grafanaUser.isGrafanaAdmin) {
                this.logger.infof("Synchronizing organizations for Grafana administrator %s with org role %s", email, targetGrafanaOrgRole);
            }

            this.logger.infof("Resolved Keycloak groups %s for user %s", Arrays.toString(groupNames), email);

            List<Organization> targetGrafanaOrgs = this.grafanaClient.findOrganizationsByNames(groupNames);
            this.logger.infof("Matched Grafana organizations %s for user %s", Arrays.toString(targetGrafanaOrgs.stream().map(org -> org.id + " (" + org.name + ")").toArray(String[]::new)), email);

            Map<Integer, UserOrganization> currentGrafanaOrgs = buildUserOrgsMap(this.grafanaClient.findUserOrganizations(grafanaUser.id));
            this.logger.infof("Current Grafana organizations %s for user %s", Arrays.toString(currentGrafanaOrgs.values().stream().map(org -> org.orgId + " (" + org.name + ") as " + org.role).toArray(String[]::new)), email);

            for (Organization targetOrg : targetGrafanaOrgs) {
                boolean isUserInOrg = currentGrafanaOrgs.containsKey(targetOrg.id);
                if (isUserInOrg) {
                    UserOrganization currentOrg = currentGrafanaOrgs.get(targetOrg.id);
                    currentGrafanaOrgs.remove(targetOrg.id);

                    boolean isRoleTheSame = currentOrg.role.equals(targetGrafanaOrgRole);
                    if (isRoleTheSame) {
                        continue;
                    }
                    this.grafanaClient.removeUserFromOrganisation(grafanaUser, targetOrg.id);
                }
                this.grafanaClient.addUserToOrganisation(grafanaUser, targetOrg, targetGrafanaOrgRole);
            }

            if (currentGrafanaOrgs.remove(1) != null) {
                this.logger.infof("Preserving Main Org membership for user %s", email);
            }

            // removing rest org in the map
            currentGrafanaOrgs.forEach((orgId, v) -> this.grafanaClient.removeUserFromOrganisation(grafanaUser, orgId));

            List<UserOrganization> finalGrafanaOrgs = this.grafanaClient.findUserOrganizations(grafanaUser.id);
            this.logger.infof("Grafana organizations after synchronization %s for user %s", Arrays.toString(finalGrafanaOrgs.stream().map(org -> org.orgId + " (" + org.name + ") as " + org.role).toArray(String[]::new)), email);
            this.logger.infof("Completed Grafana synchronization for user %s", email);
        } catch (ClientException e) {
            this.logger.errorf(e, "Failed to synchronize Grafana user %s (%s)", keycloakUserId, email);
            throw new SyncException(e.getMessage(), e);
        }
    }

    private User findOrCreateGrafanaUser(String email) throws ClientException, SyncException {
        User grafanaUser = this.grafanaClient.findUserByEmail(email);
        if (grafanaUser != null) {
            this.logger.infof("Found Grafana user %s with id %d", email, grafanaUser.id);
            return grafanaUser;
        }

        this.logger.infof("Creating Grafana user %s", email);
        grafanaUser = new User();
        grafanaUser.email = email;
        grafanaUser.password = UUID.randomUUID().toString(); // just random password which will be overwritten by OAUTH
        this.grafanaClient.saveUser(grafanaUser);
        if (grafanaUser.id > 0) {
            this.logger.infof("Created Grafana user %s with id %d", email, grafanaUser.id);
            return grafanaUser;
        }

        throw new SyncException("Fail to sync user " + email, null);
    }

    private void syncGrafanaUserPermissions(User user, String role) throws ClientException {
        if (role.equals(User.ROLE_GRAFANA_ADMIN) != user.isGrafanaAdmin) {
            user.isGrafanaAdmin = role.equals(User.ROLE_GRAFANA_ADMIN);
            this.grafanaClient.saveUserPermissions(user);
        }
    }
}