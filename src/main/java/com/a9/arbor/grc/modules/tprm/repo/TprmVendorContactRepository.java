package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmVendorContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface TprmVendorContactRepository extends JpaRepository<TprmVendorContact, Long> {
    List<TprmVendorContact> findByVendorIdOrderByIdAsc(Long vendorId);

    @Modifying
    void deleteByVendorId(Long vendorId);
}
