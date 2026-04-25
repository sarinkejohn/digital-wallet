package com.sarinkejohn.digitalwalletbackendservice.mapper;

import com.sarinkejohn.digitalwalletbackendservice.dto.TransactionDto;
import com.sarinkejohn.digitalwalletbackendservice.entity.Transactions;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionsMapper {
    TransactionDto toDto(Transactions transactions);
    Transactions toEntity(TransactionDto transactionDto);
}