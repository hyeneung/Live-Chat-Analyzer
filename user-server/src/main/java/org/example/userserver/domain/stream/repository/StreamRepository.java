package org.example.userserver.domain.stream.repository;

import org.example.userserver.domain.stream.entity.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StreamRepository extends JpaRepository<Stream, Long> {
    @Query("SELECT s FROM Stream s LEFT JOIN FETCH s.host")
    Slice<Stream> findAllWithHost(Pageable pageable);
}
