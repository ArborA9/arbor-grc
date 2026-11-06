package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.DpiaQuestionnaireService;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaQuestionnaireDtos;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/privacy/dpia-questionnaires")
@RequiresModule(ModuleCode.PRIVACY)
public class DpiaQuestionnaireController {

    private static final String READ =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";
    private static final String ADMIN = "hasAnyRole('OWNER','ADMIN')";

    private final DpiaQuestionnaireService service;
    private final UserService userService;

    public DpiaQuestionnaireController(DpiaQuestionnaireService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ)
    public List<DpiaQuestionnaireDtos.QuestionnaireResponse> list(Authentication auth) {
        return service.list(user(auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ)
    public DpiaQuestionnaireDtos.QuestionnaireResponse get(Authentication auth, @PathVariable long id) {
        return service.get(user(auth), id);
    }

    @PostMapping
    @PreAuthorize(ADMIN)
    public DpiaQuestionnaireDtos.QuestionnaireResponse create(Authentication auth,
                                                              @Valid @RequestBody DpiaQuestionnaireDtos.CreateQuestionnaireRequest req) {
        return service.create(user(auth), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN)
    public DpiaQuestionnaireDtos.QuestionnaireResponse update(Authentication auth, @PathVariable long id,
                                                              @Valid @RequestBody DpiaQuestionnaireDtos.UpdateQuestionnaireRequest req) {
        return service.update(user(auth), id, req);
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize(ADMIN)
    public DpiaQuestionnaireDtos.QuestionResponse addQuestion(Authentication auth, @PathVariable long id,
                                                              @Valid @RequestBody DpiaQuestionnaireDtos.UpsertQuestionRequest req) {
        return service.addQuestion(user(auth), id, req);
    }

    @PutMapping("/{id}/questions/{questionId}")
    @PreAuthorize(ADMIN)
    public DpiaQuestionnaireDtos.QuestionResponse updateQuestion(Authentication auth, @PathVariable long id,
                                                                 @PathVariable long questionId,
                                                                 @Valid @RequestBody DpiaQuestionnaireDtos.UpsertQuestionRequest req) {
        return service.updateQuestion(user(auth), id, questionId, req);
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @PreAuthorize(ADMIN)
    public void deleteQuestion(Authentication auth, @PathVariable long id, @PathVariable long questionId) {
        service.deleteQuestion(user(auth), id, questionId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN)
    public void delete(Authentication auth, @PathVariable long id) {
        service.deleteQuestionnaire(user(auth), id);
    }

    private User user(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    }
}
