package com.appointmentbooking.service;

import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.BranchResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.BranchMapper;
import com.appointmentbooking.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper     branchMapper;

    public List<BranchResponse> getAllBranches() {
        return branchMapper.toResponseList(branchRepository.findAllActive());
    }

    public BranchResponse getBranchById(Long id) {
        Branch branch = branchRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch"));
        return branchMapper.toResponse(branch);
    }
}
