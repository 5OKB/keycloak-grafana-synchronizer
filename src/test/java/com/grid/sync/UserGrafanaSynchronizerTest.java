package com.grid.sync;

import com.grid.grafana.Client;
import com.grid.grafana.HttpClient;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.stream.Stream;

/**
 * Unit test for simple App.
 */
public class UserGrafanaSynchronizerTest {
    @Test
    public void syncUserSuccess() {
        String expectedEmail = "operator@gridgs.local";
        String expectedGroupName = "5OKB";
        String expectedRoleName = "ROLE_OPERATOR";

        GroupModel group1 = Mockito.mock(GroupModel.class);
        Mockito.when(group1.getName()).thenReturn(expectedGroupName);

        GroupModel group2 = Mockito.mock(GroupModel.class);
        Mockito.when(group2.getName()).thenReturn("some value");

        Stream.Builder<GroupModel> groups = Stream.builder();
        groups.add(group1).add(group2);

        RoleModel role1 = Mockito.mock(RoleModel.class);
        Mockito.when(role1.getName()).thenReturn(expectedRoleName);

        Stream.Builder<RoleModel> roles = Stream.builder();
        roles.add(role1);

        UserModel mockedUser = Mockito.mock(UserModel.class);
        Mockito.when(mockedUser.getEmail()).thenReturn(expectedEmail);
        Mockito.when(mockedUser.getGroupsStream()).thenReturn(groups.build());
        Mockito.when(mockedUser.getRoleMappingsStream()).thenReturn(roles.build());

        UserGrafanaSynchronizer userGrafanaSynchronizer = new UserGrafanaSynchronizer(this.buildClient(), this.buildRolesMapper(), Logger.getLogger(UserGrafanaSynchronizer.class));
        userGrafanaSynchronizer.sync(mockedUser);
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
        return new RolesMapper(System.getenv("GF_ROLE_ADMIN_MAPPING"), System.getenv("GF_ROLE_EDITOR_MAPPING"), Logger.getLogger(RolesMapper.class));
    }
}
