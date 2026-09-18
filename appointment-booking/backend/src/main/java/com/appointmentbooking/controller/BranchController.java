package com.appointmentbooking.controller;

import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.dto.response.BranchResponse;
import com.appointmentbooking.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BranchController — read-only REST interface for Capitec branch information.
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    /**
     * Retrieve all active Capitec branches.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAllBranches()));
    }

    /**
     * Retrieve a single branch by its database ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranchById(id)));
    }
}
