package com.appointmentbooking.service;

import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.BranchResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.BranchMapper;
import com.appointmentbooking.repository.BranchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BranchService")
class BranchServiceTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchMapper     branchMapper;

    @InjectMocks
    private BranchService branchService;

    private final Branch mockBranch = Branch.builder()
            .id(1L).name("Cape Town City Centre")
            .city("Cape Town").province("Western Cape")
            .isActive(true).build();

    private final BranchResponse mockResponse = BranchResponse.builder()
            .id(1L).name("Cape Town City Centre")
            .city("Cape Town").province("Western Cape").build();

    @Test
    @DisplayName("getAllBranches returns list of BranchResponse")
    void getAllBranches() {
        when(branchRepository.findAllActive()).thenReturn(List.of(mockBranch));
        when(branchMapper.toResponseList(anyList())).thenReturn(List.of(mockResponse));

        List<BranchResponse> result = branchService.getAllBranches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cape Town City Centre");
    }

    @Test
    @DisplayName("getAllBranches returns empty list when no branches")
    void getAllBranchesEmpty() {
        when(branchRepository.findAllActive()).thenReturn(List.of());
        when(branchMapper.toResponseList(anyList())).thenReturn(List.of());

        assertThat(branchService.getAllBranches()).isEmpty();
    }

    @Test
    @DisplayName("getBranchById returns BranchResponse for valid ID")
    void getBranchById() {
        when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
        when(branchMapper.toResponse(mockBranch)).thenReturn(mockResponse);

        BranchResponse result = branchService.getBranchById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getBranchById throws ResourceNotFoundException for unknown ID")
    void getBranchByIdNotFound() {
        when(branchRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.getBranchById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Branch");
    }
}
