package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

    Optional<SuperAdmin> findByEmail(String email);
}
