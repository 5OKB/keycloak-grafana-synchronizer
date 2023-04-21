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
        try {
            User grafanaUser = this.findOrCreateGrafanaUser(user);
            String targetGrafanaRole = this.rolesMapper.getGrafanaRole(user.getRoleMappingsStream());

            List<Organization> targetGrafanaOrgs = this.grafanaClient.findOrganizationsByNames(buildGroupsNames(user.getGroupsStream()));

            Map<Integer, UserOrganization> currentGrafanaOrgs = buildUserOrgsMap(this.grafanaClient.findUserOrganizations(grafanaUser.id));

            for (Organization targetOrg : targetGrafanaOrgs) {
                boolean isUserInOrg = currentGrafanaOrgs.containsKey(targetOrg.id);
                if (isUserInOrg) {
                    UserOrganization currentOrg = currentGrafanaOrgs.get(targetOrg.id);
                    currentGrafanaOrgs.remove(targetOrg.id);

                    boolean isRoleTheSame = currentOrg.role.equals(targetGrafanaRole);
                    if (isRoleTheSame) {
                        continue;
                    }
                    this.grafanaClient.removeUserFromOrganisation(grafanaUser, targetOrg.id);
                }
                this.grafanaClient.addUserToOrganisation(grafanaUser, targetOrg, targetGrafanaRole);
            }

            // removing rest org in the map
            currentGrafanaOrgs.forEach((orgId, v) -> this.grafanaClient.removeUserFromOrganisation(grafanaUser, orgId));
        } catch (ClientException e) {
            throw new SyncException(e.getMessage(), e);
        }
    }

    private User findOrCreateGrafanaUser(UserModel user) throws ClientException, SyncException {
        User grafanaUser = this.grafanaClient.findUserByEmail(user.getEmail());
        if (grafanaUser != null) {
            return grafanaUser;
        }

        grafanaUser = new User();
        grafanaUser.email = user.getEmail();
        grafanaUser.password = UUID.randomUUID().toString(); // just random password which will be overwritten by OAUTH
        this.grafanaClient.saveUser(grafanaUser);
        if (grafanaUser.id > 0) {
            return grafanaUser;
        }

        throw new SyncException("Fail to sync user " + user.getEmail(), null);
    }
}