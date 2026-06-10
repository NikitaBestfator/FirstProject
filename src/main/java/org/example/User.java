package org.example;

public class User {
    private int id;
    private String login;
    private String password;
    private String name;
    private String role;

    public User(int id, String login, String password, String name, String role) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public int getId() { return id; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getRole() { return role; }
}
