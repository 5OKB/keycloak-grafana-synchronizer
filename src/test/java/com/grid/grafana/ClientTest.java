package com.grid.grafana;

import com.grid.grafana.entity.Organization;
import com.grid.grafana.entity.User;
import com.grid.grafana.exception.ClientException;
import org.jboss.logging.Logger;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ClientTest {
    @Test
    public void findUserByEmailSuccess() {
        String expectedEmail = "owner@gridgs.local";

        HttpClient httpClient = this.buildHttpClient();
        Client client = new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));

        User user = client.findUserByEmail(expectedEmail);

        assertNotNull(user);
        assertEquals(expectedEmail, user.email);
        assertTrue(user.id > 0);
    }

    @Test
    public void findUserByEmailReturnEmptySuccess() {
        String expectedEmail = "notexistinguser@example.com";

        HttpClient httpClient = this.buildHttpClient();
        Client client = new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));

        User user = client.findUserByEmail(expectedEmail);

        assertNull(user);
    }

    @Test
    public void findUserByEmailReturnException() {
        String expectedEmail = "owner@gridgs.local";

        HttpClient httpClient = this.buildHttpClient();
        Client client = new Client(httpClient, "https://wrong_address", Logger.getLogger(Client.class));

        assertThrows(ClientException.class, () -> {
            client.findUserByEmail(expectedEmail);
        });
    }

    @Test
    public void findOrganisationByNamesSuccess() {
        String expectedOrganisation = "5OKB";

        String[] orgNames = {"tralala", expectedOrganisation, "pumpurum"};

        HttpClient httpClient = this.buildHttpClient();
        Client client = new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));

        List<Organization> organizations = client.findOrganizationsByNames(orgNames);

        assertEquals(1, organizations.size());
        assertEquals(expectedOrganisation, organizations.get(0).name);
    }

    @Test
    public void createAndDeleteUserSuccess() {
        String expectedEmail = "newuser@gridgs.local";

        User user = new User();
        user.email = expectedEmail;
        user.password = "tralala";

        HttpClient httpClient = this.buildHttpClient();
        Client client = new Client(httpClient, System.getenv("GF_ADMIN_API_URL"), Logger.getLogger(Client.class));
        client.saveUser(user);

        assertEquals(expectedEmail, user.email);
        assertTrue(user.id > 0);

        client.removeUser(user);

        user = client.findUserByEmail(expectedEmail);
        assertNull(user);
    }

    private HttpClient buildHttpClient() {
        try {
            return new HttpClient(System.getenv("GF_ADMIN_API_URL"), System.getenv("GF_ADMIN_API_USER"), System.getenv("GF_ADMIN_API_PASSWORD"), Logger.getLogger(HttpClient.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
