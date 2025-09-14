package org.example.userserver.domain.stream.service;

import org.example.userserver.domain.stream.dto.response.ReadStreamListResponseDto;
import org.example.userserver.domain.stream.dto.response.StreamUserCountUpdateDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface StreamService {
    ReadStreamListResponseDto getAllStreams(Pageable pageable);

    void enterStream(Long userId, String streamId);

    SseEmitter subscribe();

    void notifyUserCountUpdate(StreamUserCountUpdateDto dto);

    long getStreamViewerCount(String streamId);
}