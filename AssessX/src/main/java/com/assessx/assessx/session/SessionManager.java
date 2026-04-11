package com.assessx.assessx.session;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private String token;
    private Long   userId;
    private String githubLogin;
    private String name;
    private String role;       // "STUDENT" | "TEACHER"

    private SessionManager() {}

    public static SessionManager get() {
        return INSTANCE;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public void setUserId(Long userId)         { this.userId = userId; }
    public void setGithubLogin(String login)   { this.githubLogin = login; }
    public void setName(String name)           { this.name = name; }
    public void setRole(String role)           { this.role = role; }

    public Long   getUserId()     { return userId; }
    public String getGithubLogin(){ return githubLogin; }
    public String getName()       { return name; }
    public String getRole()       { return role; }

    public boolean isTeacher() {
        return "TEACHER".equals(role);
    }

    public void clear() {
        token       = null;
        userId      = null;
        githubLogin = null;
        name        = null;
        role        = null;
    }
}
