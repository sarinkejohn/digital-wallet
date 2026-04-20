package com.sarinkejohn.digitalwalletbackendservice.repository;

import com.sarinkejohn.digitalwalletbackendservice.entity.TopUpRequest;
import com.sarinkejohn.digitalwalletbackendservice.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopUpRequestRepository extends JpaRepository<TopUpRequest, Long> {
    List<TopUpRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TopUpRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
}