package com.parking.manager.model;

/**
 * اطلاعات کاربران سیستم (اپراتور، ادمین، مالک)
 */
public class User {
    private String username;
    private String passwordHash;   // رمز عبور هش شده با SHA-256
    private Role role;
    private String fullName;
    private boolean archived;      // بایگانی شده یا فعال

    public User(String username, String passwordHash, Role role, String fullName, boolean archived) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.archived = archived;
    }

    // Getter و Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}