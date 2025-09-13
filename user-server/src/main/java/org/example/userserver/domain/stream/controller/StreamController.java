package org.example.userserver.domain.stream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.userserver.domain.stream.dto.ReadStreamListResponseDto;
import org.example.userserver.domain.stream.service.StreamService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
