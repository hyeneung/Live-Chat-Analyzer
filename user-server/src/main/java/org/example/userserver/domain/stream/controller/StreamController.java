package org.example.userserver.domain.stream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.userserver.domain.stream.dto.response.ReadStreamListResponseDto;
import org.example.userserver.domain.stream.dto.response.ReadStreamResponseDto;
import org.example.userserver.domain.stream.dto.request.StreamEnterRequestDto;
import org.example.userserver.domain.stream.dto.request.StreamLeaveRequestDto;
import org.example.userserver.domain.stream.service.StreamService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/streams")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @Operation(
            summary = "Get all streams",
            description = "Retrieves a paginated list of all available streams."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved stream list"),
    })
    @GetMapping
    public ResponseEntity<ReadStreamListResponseDto> getAllStreams(
            @PageableDefault(size = 12) Pageable pageable
    ) {
        ReadStreamListResponseDto streams = streamService.getAllStreams(pageable);
        return ResponseEntity.ok(streams);
    }

    @Operation(
            summary = "Get stream information by ID",
            description = "Retrieves detailed information for a specific stream, including viewer count."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved stream information"),
            @ApiResponse(responseCode = "404", description = "Stream not found"),
    })
    @GetMapping("/{streamId}")
    public ResponseEntity<ReadStreamResponseDto> getStreamInfo(@PathVariable Long streamId) {
        ReadStreamResponseDto streamInfo = streamService.readStreamInfo(streamId);
        return ResponseEntity.ok(streamInfo);
    }

    @Operation(
            summary = "Enter a stream room",
            description = "Adds the user to the set of participants for a given stream."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully entered stream"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PostMapping("/enter")
    public ResponseEntity<Void> enterStream(
            @RequestBody StreamEnterRequestDto requestDto,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        streamService.enterStream(userId, requestDto.streamId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Leave a stream room",
            description = "Removes the user from the set of participants for a given stream."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully left stream"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveStream(
            @RequestBody StreamLeaveRequestDto requestDto,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        streamService.leaveStream(userId, requestDto.streamId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Subscribe to all stream updates",
            description = "Subscribes to real-time updates for all streams using Server-Sent Events."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully subscribed"),
    })
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        return streamService.subscribe();
    }
}
