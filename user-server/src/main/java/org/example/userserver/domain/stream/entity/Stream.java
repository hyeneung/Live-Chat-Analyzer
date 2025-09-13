package org.example.userserver.domain.stream.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.userserver.domain.user.entity.User;
import org.example.userserver.global.entity.BaseEntity;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "stream")
public class Stream extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    @Column
    private String thumbnailUrl;

    @Builder
    public Stream(User host, String title, String thumbnailUrl) {
        this.host = host;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
    }
}