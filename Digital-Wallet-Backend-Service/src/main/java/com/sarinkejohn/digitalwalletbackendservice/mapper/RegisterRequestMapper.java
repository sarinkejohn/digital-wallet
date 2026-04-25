package com.sarinkejohn.digitalwalletbackendservice.mapper;

import com.sarinkejohn.digitalwalletbackendservice.dto.RegisterRequest;
import com.sarinkejohn.digitalwalletbackendservice.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface RegisterRequestMapper {

    User toEntity(RegisterRequest request);
}