package com.insuraTrack.controller;

import com.insuraTrack.dto.RecycleBinItem;
import com.insuraTrack.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recycle-bin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    @GetMapping
    public List<RecycleBinItem> getAll() {
        return recycleBinService.getAllDeleted();
    }

    // FIX: changed from @PutMapping to @PostMapping to match frontend api.post call
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        recycleBinService.restore(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> permanentDelete(@PathVariable String id) {
        recycleBinService.permanentDelete(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/empty")
    public ResponseEntity<Void> emptyBin() {
        recycleBinService.emptyRecycleBin();
        return ResponseEntity.ok().build();
    }
}