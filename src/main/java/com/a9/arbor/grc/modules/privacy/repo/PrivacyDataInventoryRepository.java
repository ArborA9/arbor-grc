package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.PrivacyDataInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyDataInventoryRepository extends JpaRepository<PrivacyDataInventory, Long> {

    List<PrivacyDataInventory> findAllByOrderByUpdatedAtDesc();

    long countByDisplayIdStartingWith(String prefix);
}
