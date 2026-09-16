package com.appointmentbooking.mapper;

import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.BranchResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BranchMapper {
    BranchResponse toResponse(Branch branch);
    List<BranchResponse> toResponseList(List<Branch> branches);
}
