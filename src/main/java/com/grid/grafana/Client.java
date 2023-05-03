package com.grid.grafana;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grid.grafana.entity.Organization;
import com.grid.grafana.entity.User;
import com.grid.grafana.entity.UserOrganization;
import com.grid.grafana.exception.ClientException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Client {
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Logger logger;

    public Client(HttpClient httpClient, String baseUrl, Logger logger) {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.logger = logger;
    }

    public User findUserByEmail(String email) throws ClientException {
        HttpGet request = new HttpGet(this.baseUrl + "users/lookup?loginOrEmail=" + email);

        try (CloseableHttpResponse response = this.execute(request)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return this.unserialize(response.getEntity().getContent(), User.class);
            }
            return null;
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public void saveUser(User user) throws ClientException {
        if (user.id > 0) {
            throw new ClientException("Updating of the user is not implemented yet");
        } else {
            HttpPost request = new HttpPost(this.baseUrl + "admin/users");
            try {
                request.setEntity(new StringEntity(this.serialize(user), ContentType.APPLICATION_JSON));
                try (CloseableHttpResponse response = this.execute(request)) {
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        User newUser = this.unserialize(response.getEntity().getContent(), User.class);
                        user.id = newUser.id;
                        return;
                    }
                    throw new ClientException("saveUser error: " + response.getStatusLine().getStatusCode(), null);
                }
            } catch (Exception e) {
                throw new ClientException(e.getMessage(), e);
            }
        }
    }

    public void saveUserPermissions(User user) throws ClientException {
        HttpPut request = new HttpPut(this.baseUrl + "admin/users/" + user.id + "/permissions");
        String requestJson = "{\"isGrafanaAdmin\":" + user.isGrafanaAdmin + "}";
        request.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = this.execute(request)) {
            this.logger.infof("Setting user %s permissions. isGrafanaAdmin:%s", user.email, user.isGrafanaAdmin);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return;
            }
            throw new ClientException(response.getStatusLine().getReasonPhrase());
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public void removeUser(User user) throws ClientException {
        HttpDelete request = new HttpDelete(this.baseUrl + "admin/users/" + user.id);

        try (CloseableHttpResponse response = this.execute(request)) {
            this.logger.infof("Removing user %s", user.email);
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public List<UserOrganization> findUserOrganizations(int userId) throws ClientException {
        HttpGet request = new HttpGet(this.baseUrl + "users/" + userId + "/orgs");

        try (CloseableHttpResponse response = this.execute(request)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                UserOrganization[] organizations = this.unserialize(response.getEntity().getContent(), UserOrganization[].class);
                return Arrays.asList(organizations);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public List<Organization> findOrganizationsByNames(String[] names) throws ClientException {
        HttpGet request = new HttpGet(this.baseUrl + "orgs");
        List<Organization> result = new ArrayList<>();

        try (CloseableHttpResponse response = this.execute(request)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Organization[] organizations = this.unserialize(response.getEntity().getContent(), Organization[].class);
                for (Organization organization : organizations) {
                    for (String name : names) {
                        if (organization.name.equals(name)) {
                            result.add(organization);
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public void addUserToOrganisation(User user, Organization org, String role) throws ClientException {
        HttpPost request = new HttpPost(this.baseUrl + "orgs/" + org.id + "/users/");
        String requestJson = "{\"loginOrEmail\":\"" + user.email + "\",\"role\":\"" + role + "\"}";
        request.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = this.execute(request)) {
            this.logger.infof("Adding user %s to %d (%s) as %s", user.email, org.id, org.name, role);
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public void removeUserFromOrganisation(User user, int orgId) throws ClientException {
        HttpDelete request = new HttpDelete(this.baseUrl + "orgs/" + orgId + "/users/" + user.id);

        try (CloseableHttpResponse response = this.execute(request)) {
            this.logger.infof("Removing user %s from %d", user.email, orgId);
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    private CloseableHttpResponse execute(HttpUriRequest request) throws ClientException, IOException {
        try (CloseableHttpResponse response = this.httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            // @TODO improve condition
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NOT_FOUND || status == HttpStatus.SC_CONFLICT) {
                return response;
            }
            throw new ClientException("Response: " + status + " " + EntityUtils.toString(response.getEntity()), null);
        }
    }

    private String serialize(Object obj) throws IOException {
        return this.mapper.writeValueAsString(obj);
    }

    private <T> T unserialize(InputStream bytes, Class<T> type) throws IOException {
        return this.mapper.readValue(bytes, type);
    }
}