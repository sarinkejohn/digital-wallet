package com.sarinkejohn.digitalwalletbackendservice.mapper;

import com.sarinkejohn.digitalwalletbackendservice.dto.TopUpRequestDto;
import com.sarinkejohn.digitalwalletbackendservice.entity.TopUpRequest;
import org.mapstruct.Mapper;

@Mapper
public interface TopUpRequestMapper {

    TopUpRequest toEntity(TopUpRequestDto dto);
    TopUpRequestDto toDto(TopUpRequest entity);
}