package com.sarinkejohn.digitalwalletbackendservice.mapper;

import com.sarinkejohn.digitalwalletbackendservice.dto.WalletDto;
import com.sarinkejohn.digitalwalletbackendservice.entity.Wallet;
import com.sarinkejohn.digitalwalletbackendservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    WalletDto toDto(Wallet wallet);

    @Mapping(target = "user.username", source = "walletDto.username")
    Wallet toEntity(WalletDto walletDto);
    
    // Helper method to set user on wallet
    default void setUser(Wallet wallet, WalletDto walletDto) {
        if (walletDto.userId() != null) {
            User user = new User();
            user.setId(walletDto.userId());
            user.setUsername(walletDto.username());
            wallet.setUser(user);
        }
    }
}