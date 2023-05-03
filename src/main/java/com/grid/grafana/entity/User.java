package com.grid.grafana.entity;

public class User {
    public static final String ROLE_GRAFANA_ADMIN = "GrafanaAdmin";
    public static final String ROLE_ORG_ADMIN = "Admin";
    public static final String ROLE_ORG_EDITOR = "Editor";
    public static final String ROLE_ORG_VIEWER = "Viewer";
    public int id;
    public String email;
    public String password;
    public boolean isGrafanaAdmin = false;
}