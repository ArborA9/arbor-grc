package com.a9.arbor.grc.modules.notification.repo;

import com.a9.arbor.grc.modules.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findFirstByTriggerTypeAndActiveTrueOrderByVersionDesc(String triggerType);

    List<NotificationTemplate> findAllByOrderByTriggerTypeAscVersionDesc();
}
