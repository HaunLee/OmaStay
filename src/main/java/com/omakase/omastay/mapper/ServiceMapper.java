package com.omakase.omastay.mapper;

import com.omakase.omastay.dto.ServiceDTO;
import com.omakase.omastay.entity.Service;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ServiceMapper {
    ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

    ServiceDTO toServiceDTO(Service service);

    Service toService(ServiceDTO serviceDTO);

    List<ServiceDTO> toServiceDTOList(List<Service> serviceList);

    List<Service> toServiceList(List<ServiceDTO> serviceDTOList);
}