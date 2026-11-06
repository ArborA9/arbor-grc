package com.a9.arbor.grc.modules.activity.web;

import com.a9.arbor.grc.modules.activity.service.ActivityTypeRegistry;
import com.a9.arbor.grc.modules.activity.web.dto.ActivityTypeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity-types")
public class ActivityTypeController {

    private final ActivityTypeRegistry registry;

    public ActivityTypeController(ActivityTypeRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<ActivityTypeResponse> list() {
        return registry.list();
    }
}
