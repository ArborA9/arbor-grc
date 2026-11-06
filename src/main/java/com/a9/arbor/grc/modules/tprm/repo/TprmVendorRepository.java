package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.VendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TprmVendorRepository extends JpaRepository<TprmVendor, Long> {
    List<TprmVendor> findAllByOrderByNameAsc();

    List<TprmVendor> findByStatus(VendorStatus status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
