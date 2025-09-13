package org.example.userserver.domain.stream.service;

import org.example.userserver.domain.stream.dto.ReadStreamListResponseDto;
import org.springframework.data.domain.Pageable;

public interface StreamService {
    ReadStreamListResponseDto getAllStreams(Pageable pageable);
}
