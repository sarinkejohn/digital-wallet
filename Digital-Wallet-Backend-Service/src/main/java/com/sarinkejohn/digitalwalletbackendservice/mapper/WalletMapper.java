package com.sarinkejohn.digitalwalletbackendservice.mapper;

import com.sarinkejohn.digitalwalletbackendservice.dto.WalletDto;
import com.sarinkejohn.digitalwalletbackendservice.entity.User;
import com.sarinkejohn.digitalwalletbackendservice.entity.Wallet;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "balance", target = "balance")
    WalletDto toDto(Wallet wallet);

    Wallet toEntity(WalletDto walletDto);

    @AfterMapping
    default void afterMapping(WalletDto walletDto, @MappingTarget Wallet wallet) {
        if (walletDto != null && walletDto.userId() != null) {
            User user = new User();
            user.setId(walletDto.userId());
            user.setUsername(walletDto.username());
            wallet.setUser(user);
        }
    }
}