package com.grid.grafana;

import com.grid.grafana.entity.Organization;
import com.grid.grafana.entity.User;
import com.grid.grafana.exception.ClientException;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ClientTest {
    @Test
    public void createAndDeleteUserSuccess() {
        String expectedEmail = "org_user_" + RandomStringUtils.randomAlphabetic(10) + "@example.com";

        User user = new User();
        user.email = expectedEmail;
        user.password = "tralala";

        Client client = this.buildClient();

        // test create
        client.saveUser(user);

        assertEquals(expectedEmail, user.email);
        assertTrue(user.id > 0);
        assertFalse(user.isGrafanaAdmin);

        // test find
        user = client.findUserByEmail(expectedEmail);
        assertEquals(expectedEmail, user.email);
        assertTrue(user.id > 0);

        // test remove
        client.removeUser(user);

        user = client.findUserByEmail(expectedEmail);
        assertNull(user);
    }

    @Test
    public void createGrafanaAdminUser() {
        String expectedEmail = "grafana_admin_" + RandomStringUtils.randomAlphabetic(10) + "@example.com";

        User user = new User();
        user.email = expectedEmail;
        user.password = "tralala";

        Client client = this.buildClient();

        client.saveUser(user);
        assertEquals(expectedEmail, user.email);
        assertTrue(user.id > 0);
        assertFalse(user.isGrafanaAdmin);

        user.isGrafanaAdmin = true;
        client.saveUserPermissions(user);
        assertEquals(expectedEmail, user.email);
        assertTrue(user.isGrafanaAdmin);

        user = client.findUserByEmail(expectedEmail);
        assertEquals(expectedEmail, user.email);
        assertTrue(user.isGrafanaAdmin);
    }

    @Test
    public void findUserByEmailReturnException() {
        String expectedEmail = "someuser@example.com";

        HttpClient httpClient = this.buildHttpClient();
        Client client = new Client(httpClient, "https://wrong_address", Logger.getLogger(Client.class));

        assertThrows(ClientException.class, () -> {
            client.findUserByEmail(expectedEmail);
        });
    }

    @Test
    public void createAndFindOrganisationByNamesSuccess() {
        String expectedOrganisationName = "Org" + RandomStringUtils.randomAlphabetic(10);

        // create organization
        Organization organization = new Organization();
        organization.name = expectedOrganisationName;
        Client client = this.buildClient();
        client.saveOrganization(organization);
        assertEquals(expectedOrganisationName, organization.name);
        assertTrue("Organisation id is missing", organization.id > 0);

        String[] orgNames = {"tralala", expectedOrganisationName, "pumpurum"};

        List<Organization> organizations = client.findOrganizationsByNames(orgNames);

        assertEquals(1, organizations.size());
        assertEquals(expectedOrganisationName, organizations.get(0).name);
    }

    private HttpClient buildHttpClient() {
        try {
            return new HttpClient(System.getenv("GF_ADMIN_API_URL"), System.getenv("GF_ADMIN_API_USER"), System.getenv("GF_ADMIN_API_PASSWORD"), Logger.getLogger(HttpClient.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Client buildClient() {
        HttpClient httpClient = this.buildHttpClient();
        return new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));
    }
}
