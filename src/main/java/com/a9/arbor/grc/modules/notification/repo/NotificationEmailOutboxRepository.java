package com.a9.arbor.grc.modules.notification.repo;

import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEmailOutboxRepository extends JpaRepository<NotificationEmailOutbox, Long> {

    Page<NotificationEmailOutbox> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
