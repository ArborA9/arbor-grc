package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.privacy.domain.DpiaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaire;
import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaireQuestion;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResponseType;
import com.a9.arbor.grc.modules.privacy.repo.DpiaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.DpiaQuestionnaireQuestionRepository;
import com.a9.arbor.grc.modules.privacy.repo.DpiaQuestionnaireRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaQuestionnaireDtos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DpiaQuestionnaireService {

    public static final String DEFAULT_NAME = "DPIA Questionnaire";
    public static final String DEFAULT_DESCRIPTION = "Default DPIA questionnaire";

    /** Serializes first-time seed so parallel list/project loads cannot create two defaults. */
    private static final Object SEED_LOCK = new Object();

    private final DpiaQuestionnaireRepository questionnaireRepository;
    private final DpiaQuestionnaireQuestionRepository questionRepository;
    private final DpiaDepartmentRepository departmentRepository;
    private final DpiaSeedData seedData;
    private final ObjectMapper objectMapper;

    public DpiaQuestionnaireService(DpiaQuestionnaireRepository questionnaireRepository,
                                    DpiaQuestionnaireQuestionRepository questionRepository,
                                    DpiaDepartmentRepository departmentRepository,
                                    DpiaSeedData seedData,
                                    ObjectMapper objectMapper) {
        this.questionnaireRepository = questionnaireRepository;
        this.questionRepository = questionRepository;
        this.departmentRepository = departmentRepository;
        this.seedData = seedData;
        this.objectMapper = objectMapper;
    }

    /**
     * Ensures exactly one system-default questionnaire named {@link #DEFAULT_NAME}.
     * Collapses duplicates created by a race on first load and renames legacy "v3" titles.
     */
    @Transactional
    public void ensureDefaultSeeded(User actor) {
        synchronized (SEED_LOCK) {
            List<DpiaQuestionnaire> defaults = questionnaireRepository.findBySystemDefaultTrueOrderByIdAsc();
            if (defaults.isEmpty()) {
                createDefault(actor);
                return;
            }
            DpiaQuestionnaire keep = defaults.get(0);
            boolean dirty = false;
            if (!DEFAULT_NAME.equals(keep.getName())) {
                keep.setName(DEFAULT_NAME);
                dirty = true;
            }
            if (keep.getDescription() == null || keep.getDescription().isBlank()
                    || keep.getDescription().contains("v3")) {
                keep.setDescription(DEFAULT_DESCRIPTION);
                dirty = true;
            }
            if (!keep.isActive()) {
                keep.setActive(true);
                dirty = true;
            }
            if (dirty) {
                questionnaireRepository.save(keep);
            }
            for (int i = 1; i < defaults.size(); i++) {
                removeDuplicateDefault(keep.getId(), defaults.get(i));
            }
        }
    }

    private void createDefault(User actor) {
        DpiaQuestionnaire q = new DpiaQuestionnaire();
        q.setName(DEFAULT_NAME);
        q.setDescription(DEFAULT_DESCRIPTION);
        q.setSystemDefault(true);
        q.setActive(true);
        q.setCreatedByUserId(actor != null ? actor.getId() : 0L);
        questionnaireRepository.save(q);
        int order = 0;
        for (DpiaSeedData.SeedQuestion sq : seedData.load().questions()) {
            DpiaQuestionnaireQuestion row = new DpiaQuestionnaireQuestion();
            row.setQuestionnaireId(q.getId());
            row.setCode(sq.code());
            row.setSectionName(sq.section());
            row.setPrinciple(sq.principle());
            row.setQuestionText(sq.question());
            row.setGuidance(blankToNull(sq.guidance()));
            row.setResponseType(DpiaResponseType.valueOf(sq.responseType()));
            row.setOptionsJson(writeOptions(sq.options()));
            row.setSortOrder(order++);
            row.setRequired(true);
            questionRepository.save(row);
        }
    }

    private void removeDuplicateDefault(long keepId, DpiaQuestionnaire dup) {
        for (DpiaDepartment d : departmentRepository.findByQuestionnaireId(dup.getId())) {
            d.setQuestionnaireId(keepId);
            departmentRepository.save(d);
        }
        questionRepository.deleteByQuestionnaireId(dup.getId());
        questionnaireRepository.delete(dup);
    }

    @Transactional
    public List<DpiaQuestionnaireDtos.QuestionnaireResponse> list(User actor) {
        ensureDefaultSeeded(actor);
        return questionnaireRepository.findAllByOrderBySystemDefaultDescNameAsc().stream()
                .map(q -> DpiaQuestionnaireDtos.QuestionnaireResponse.summary(q,
                        (int) questionRepository.countByQuestionnaireId(q.getId())))
                .toList();
    }

    @Transactional
    public DpiaQuestionnaireDtos.QuestionnaireResponse get(User actor, long id) {
        ensureDefaultSeeded(actor);
        DpiaQuestionnaire q = require(id);
        return toDetail(q);
    }

    @Transactional
    public DpiaQuestionnaireDtos.QuestionnaireResponse create(User admin, DpiaQuestionnaireDtos.CreateQuestionnaireRequest req) {
        ensureDefaultSeeded(admin);
        String name = req.name().trim();
        if (questionnaireRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A questionnaire named \"" + name + "\" already exists");
        }
        DpiaQuestionnaire q = new DpiaQuestionnaire();
        q.setName(name);
        q.setDescription(blankToNull(req.description()));
        q.setSystemDefault(false);
        q.setActive(true);
        q.setCreatedByUserId(admin.getId());
        questionnaireRepository.save(q);
        return toDetail(q);
    }

    @Transactional
    public DpiaQuestionnaireDtos.QuestionnaireResponse update(User admin, long id,
                                                             DpiaQuestionnaireDtos.UpdateQuestionnaireRequest req) {
        DpiaQuestionnaire q = require(id);
        String name = req.name().trim();
        if (q.isSystemDefault()) {
            name = DEFAULT_NAME;
        }
        if (questionnaireRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A questionnaire named \"" + name + "\" already exists");
        }
        q.setName(name);
        q.setDescription(blankToNull(req.description()));
        if (req.active() != null) {
            if (q.isSystemDefault() && !req.active()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The default questionnaire cannot be deactivated");
            }
            q.setActive(req.active());
        }
        questionnaireRepository.save(q);
        return toDetail(q);
    }

    @Transactional
    public DpiaQuestionnaireDtos.QuestionResponse addQuestion(User admin, long questionnaireId,
                                                             DpiaQuestionnaireDtos.UpsertQuestionRequest req) {
        DpiaQuestionnaire q = require(questionnaireId);
        String code = req.code().trim();
        if (questionRepository.existsByQuestionnaireIdAndCodeIgnoreCase(questionnaireId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Question code \"" + code + "\" already exists on this questionnaire");
        }
        DpiaQuestionnaireQuestion row = new DpiaQuestionnaireQuestion();
        row.setQuestionnaireId(q.getId());
        applyQuestion(row, req, code);
        if (req.sortOrder() == null) {
            row.setSortOrder((int) questionRepository.countByQuestionnaireId(questionnaireId));
        }
        questionRepository.save(row);
        return toQuestion(row);
    }

    @Transactional
    public DpiaQuestionnaireDtos.QuestionResponse updateQuestion(User admin, long questionnaireId, long questionId,
                                                                DpiaQuestionnaireDtos.UpsertQuestionRequest req) {
        require(questionnaireId);
        DpiaQuestionnaireQuestion row = questionRepository.findByIdAndQuestionnaireId(questionId, questionnaireId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        String code = req.code().trim();
        if (questionRepository.existsByQuestionnaireIdAndCodeIgnoreCaseAndIdNot(questionnaireId, code, questionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Question code \"" + code + "\" already exists on this questionnaire");
        }
        applyQuestion(row, req, code);
        questionRepository.save(row);
        return toQuestion(row);
    }

    @Transactional
    public void deleteQuestion(User admin, long questionnaireId, long questionId) {
        DpiaQuestionnaire q = require(questionnaireId);
        DpiaQuestionnaireQuestion row = questionRepository.findByIdAndQuestionnaireId(questionId, questionnaireId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (q.isSystemDefault() && questionRepository.countByQuestionnaireId(questionnaireId) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The default questionnaire must keep at least one question");
        }
        questionRepository.delete(row);
    }

    @Transactional
    public void deleteQuestionnaire(User admin, long id) {
        ensureDefaultSeeded(admin);
        DpiaQuestionnaire q = require(id);
        if (q.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The default questionnaire cannot be deleted");
        }
        long inUse = departmentRepository.findByQuestionnaireId(id).size();
        if (inUse > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This questionnaire is assigned to " + inUse
                            + " DPIA department(s) and cannot be deleted");
        }
        questionRepository.deleteByQuestionnaireId(id);
        questionnaireRepository.delete(q);
    }

    public DpiaQuestionnaire requireActive(long id) {
        DpiaQuestionnaire q = require(id);
        if (!q.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questionnaire is inactive");
        }
        return q;
    }

    public DpiaQuestionnaire require(long id) {
        return questionnaireRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire not found"));
    }

    public List<DpiaQuestionnaireQuestion> listQuestions(long questionnaireId) {
        return questionRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(questionnaireId);
    }

    public DpiaQuestionnaire defaultOr(Long preferredId) {
        if (preferredId != null) {
            return requireActive(preferredId);
        }
        return questionnaireRepository.findFirstBySystemDefaultTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Default DPIA questionnaire is not seeded"));
    }

    private void applyQuestion(DpiaQuestionnaireQuestion row, DpiaQuestionnaireDtos.UpsertQuestionRequest req, String code) {
        row.setCode(code);
        row.setSectionName(req.sectionName().trim());
        row.setPrinciple(req.principle().trim());
        row.setQuestionText(req.questionText().trim());
        row.setGuidance(blankToNull(req.guidance()));
        row.setResponseType(req.responseType());
        row.setOptionsJson(writeOptions(req.options()));
        if (req.sortOrder() != null) {
            row.setSortOrder(req.sortOrder());
        }
        if (req.required() != null) {
            row.setRequired(req.required());
        }
        if (req.responseType() == DpiaResponseType.MULTI_SELECT
                && (req.options() == null || req.options().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Multi-select questions need options");
        }
    }

    private DpiaQuestionnaireDtos.QuestionnaireResponse toDetail(DpiaQuestionnaire q) {
        List<DpiaQuestionnaireDtos.QuestionResponse> questions = listQuestions(q.getId()).stream()
                .map(this::toQuestion).toList();
        return DpiaQuestionnaireDtos.QuestionnaireResponse.detail(q, questions);
    }

    private DpiaQuestionnaireDtos.QuestionResponse toQuestion(DpiaQuestionnaireQuestion q) {
        return DpiaQuestionnaireDtos.QuestionResponse.from(q, readOptions(q.getOptionsJson()));
    }

    private String writeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid options");
        }
    }

    private List<String> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
