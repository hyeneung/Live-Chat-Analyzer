package org.example.userserver.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.userserver.global.entity.BaseEntity;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    public User(String name, String email, Role role, String profileImage) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.profileImage = profileImage;
    }

    public User updateNameTo(String name) {
        this.name = name;
        return this;
    }

    public User updateProfileImageTo(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }
}