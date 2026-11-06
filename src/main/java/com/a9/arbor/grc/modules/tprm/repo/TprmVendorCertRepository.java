package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmVendorCert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface TprmVendorCertRepository extends JpaRepository<TprmVendorCert, Long> {
    List<TprmVendorCert> findByVendorIdOrderByIdAsc(Long vendorId);

    @Modifying
    void deleteByVendorId(Long vendorId);
}
