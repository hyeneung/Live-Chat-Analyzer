package org.example.userserver.domain.stream.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.userserver.domain.stream.dto.ReadStreamListResponseDto;
import org.example.userserver.domain.stream.entity.Stream;
import org.example.userserver.domain.stream.repository.StreamRepository;
import org.example.userserver.domain.stream.service.StreamService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamServiceImpl implements StreamService {

    private final StreamRepository streamRepository;

    @Override
    public ReadStreamListResponseDto getAllStreams(Pageable pageable) {
        Slice<Stream> streams = streamRepository.findAllWithHost(pageable);
        return ReadStreamListResponseDto.from(streams);
    }
}
