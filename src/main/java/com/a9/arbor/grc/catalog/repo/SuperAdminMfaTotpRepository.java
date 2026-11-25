package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.SuperAdminMfaTotp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminMfaTotpRepository extends JpaRepository<SuperAdminMfaTotp, Long> {
    Optional<SuperAdminMfaTotp> findBySuperAdminId(Long superAdminId);

    void deleteBySuperAdminId(Long superAdminId);
}
