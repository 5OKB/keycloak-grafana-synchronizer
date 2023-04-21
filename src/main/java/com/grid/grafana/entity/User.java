package com.grid.grafana.entity;

public class User {
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_EDITOR = "Editor";
    public static final String ROLE_VIEWER = "Viewer";
    public static final String[] ROLES = {User.ROLE_ADMIN, User.ROLE_EDITOR, User.ROLE_VIEWER};
    public int id;
    public String email;
    public String password;
}