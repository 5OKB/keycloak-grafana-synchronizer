package com.grid.sync;

import com.grid.grafana.Client;
import com.grid.grafana.HttpClient;
import com.grid.grafana.entity.Organization;
import com.grid.grafana.entity.User;
import com.grid.grafana.entity.UserOrganization;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserGrafanaSynchronizerTest {
    private final Client client;

    public UserGrafanaSynchronizerTest() {
        this.client = buildClient();
    }

    @Test
    public void syncUserSuccess() {
        String expectedEmail = "org_admin@example.com";
        String expectedOrgName = "org_admin_org_" + RandomStringUtils.randomAlphabetic(10);
        String roleKeycloakOrgAdmin = "ROLE_KEYCLOAK_ORG_ADMIN";
        String expectedOrgRole = User.ROLE_ORG_ADMIN;

        Organization organization = this.createOrg(expectedOrgName);

        GroupModel group1 = buildGroup(expectedOrgName);
        GroupModel group2 = buildGroup("anotherGroup");

        RoleModel role1 = buildRole(roleKeycloakOrgAdmin);
        RoleModel role2 = buildRole("otherRole");

        UserModel mockedUser = Mockito.mock(UserModel.class);
        Mockito.when(mockedUser.getEmail()).thenAnswer(i -> expectedEmail);
        Mockito.when(mockedUser.getGroupsStream()).thenAnswer(i -> Stream.of(group1, group2));
        Mockito.when(mockedUser.getRoleMappingsStream()).thenAnswer(i -> Stream.of(role1, role2));

        UserGrafanaSynchronizer userGrafanaSynchronizer = new UserGrafanaSynchronizer(this.client, this.buildRolesMapper(), Logger.getLogger(UserGrafanaSynchronizer.class));
        userGrafanaSynchronizer.sync(mockedUser);

        User grafanaUser = this.client.findUserByEmail(expectedEmail);
        assertEquals(expectedEmail, grafanaUser.email);

        List<UserOrganization> orgs = this.client.findUserOrganizations(grafanaUser.id);
        assertEquals(1, orgs.size());

        for (UserOrganization userOrg : orgs) {
            assertEquals(organization.id, userOrg.orgId);
            assertEquals(expectedOrgName, userOrg.name);
            assertEquals(expectedOrgRole, userOrg.role);
        }
    }

    @Test
    public void syncGrafanaAdminUserSuccess() {
        String expectedEmail = "grafana_admin@example.com";
        String roleKeycloakOrgAdmin = "ROLE_KEYCLOAK_SUPER_ADMIN";
        String expectedOrgRole = User.ROLE_GRAFANA_ADMIN;

        RoleModel role = buildRole(roleKeycloakOrgAdmin);

        UserModel mockedUser = Mockito.mock(UserModel.class);
        Mockito.when(mockedUser.getEmail()).thenAnswer(i -> expectedEmail);
        Mockito.when(mockedUser.getRoleMappingsStream()).thenAnswer(i -> Stream.of(role));

        UserGrafanaSynchronizer userGrafanaSynchronizer = new UserGrafanaSynchronizer(this.client, this.buildRolesMapper(), Logger.getLogger(UserGrafanaSynchronizer.class));
        userGrafanaSynchronizer.sync(mockedUser);

        User grafanaAdminUser = this.client.findUserByEmail(expectedEmail);
        assertEquals(expectedEmail, grafanaAdminUser.email);
        assertTrue(grafanaAdminUser.isGrafanaAdmin);
    }


    private Client buildClient() {
        try {
            HttpClient httpClient = new HttpClient(System.getenv("GF_ADMIN_API_URL"), System.getenv("GF_ADMIN_API_USER"), System.getenv("GF_ADMIN_API_PASSWORD"), Logger.getLogger(HttpClient.class));
            return new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RolesMapper buildRolesMapper() {
        return new RolesMapper(System.getenv("GF_ROLE_GRAFANA_ADMIN_MAPPING"), System.getenv("GF_ROLE_ORG_ADMIN_MAPPING"), System.getenv("GF_ROLE_ORG_EDITOR_MAPPING"), Logger.getLogger(RolesMapper.class));
    }

    private GroupModel buildGroup(String name) {
        GroupModel group = Mockito.mock(GroupModel.class);
        Mockito.when(group.getName()).thenReturn(name);
        Mockito.when(group.getRoleMappingsStream()).thenAnswer(i -> Stream.empty());
        return group;
    }

    private RoleModel buildRole(String name) {
        RoleModel role = Mockito.mock(RoleModel.class);
        Mockito.when(role.getName()).thenAnswer(i -> name);
        return role;
    }

    private Organization createOrg(String name) {
        Organization organization = new Organization();
        organization.name = name;
        this.client.saveOrganization(organization);
        assertTrue("Organisation id is missing", organization.id > 0);
        return organization;
    }
}
