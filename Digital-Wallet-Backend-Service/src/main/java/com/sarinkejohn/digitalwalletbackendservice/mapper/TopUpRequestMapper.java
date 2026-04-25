package com.sarinkejohn.digitalwalletbackendservice.mapper;

import com.sarinkejohn.digitalwalletbackendservice.dto.TopUpRequestDto;
import com.sarinkejohn.digitalwalletbackendservice.dto.TopUpResponseDto;
import com.sarinkejohn.digitalwalletbackendservice.entity.TopUpRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TopUpRequestMapper {

    TopUpRequest toEntity(TopUpRequestDto dto);
    TopUpResponseDto toDto(TopUpRequest entity);
}