package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String address; // Optional field

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // One company can have many users (admins, medics, patients)
    // 'mappedBy' indicates the field in the User entity that owns the relationship
    // CascadeType.ALL means operations (persist, remove, etc.) on Company cascade to its Users - Be careful with REMOVE!
    // Consider using CascadeType.PERSIST, CascadeType.MERGE instead if needed.
    // FetchType.LAZY is generally preferred for collections to avoid loading all users unnecessarily.
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    // Helper method to add a user to the company
    public void addUser(User user) {
        users.add(user);
        user.setCompany(this);
    }

    // Helper method to remove a user from the company
    public void removeUser(User user) {
        users.remove(user);
        user.setCompany(null);
    }
}