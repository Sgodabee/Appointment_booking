package com.appointmentbooking.mapper;

import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.BranchResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-16T20:20:34+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class BranchMapperImpl implements BranchMapper {

    @Override
    public BranchResponse toResponse(Branch branch) {
        if ( branch == null ) {
            return null;
        }

        BranchResponse.BranchResponseBuilder branchResponse = BranchResponse.builder();

        branchResponse.id( branch.getId() );
        branchResponse.name( branch.getName() );
        branchResponse.address( branch.getAddress() );
        branchResponse.city( branch.getCity() );
        branchResponse.province( branch.getProvince() );
        branchResponse.phone( branch.getPhone() );
        branchResponse.email( branch.getEmail() );
        branchResponse.operatingHours( branch.getOperatingHours() );
        branchResponse.isActive( branch.getIsActive() );

        return branchResponse.build();
    }

    @Override
    public List<BranchResponse> toResponseList(List<Branch> branches) {
        if ( branches == null ) {
            return null;
        }

        List<BranchResponse> list = new ArrayList<BranchResponse>( branches.size() );
        for ( Branch branch : branches ) {
            list.add( toResponse( branch ) );
        }

        return list;
    }
}
