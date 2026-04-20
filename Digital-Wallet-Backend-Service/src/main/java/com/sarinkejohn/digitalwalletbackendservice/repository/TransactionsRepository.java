package com.sarinkejohn.digitalwalletbackendservice.repository;

import com.sarinkejohn.digitalwalletbackendservice.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    List<Transactions> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId);
    Optional<Transactions> findByReference(String reference);
}