package com.dewple.recruitment.service;

import com.dewple.club.annotation.RequireClubPermission;
import com.dewple.club.entity.ClubDepartment;
import com.dewple.club.entity.ClubGeneration;
import com.dewple.club.repository.ClubDepartmentRepository;
import com.dewple.club.repository.ClubGenerationRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.Permission;
import com.dewple.common.enums.ProcessType;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.AutoComponentType;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.recruitment.entity.FormFieldType;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.repository.ApplicationRepository;
import com.dewple.recruitment.entity.RecruitmentDepartment;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.entity.RecruitmentProcess;
import com.dewple.recruitment.entity.RecruitmentSchema;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.repository.RecruitmentPostingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RecruitmentPostingRepository recruitmentPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final ClubDepartmentRepository clubDepartmentRepository;
    private final ClubGenerationRepository clubGenerationRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting createRecruitment(Long clubId, Long creatorId, CreateRecruitmentCommand command) {
        RecruitmentPosting posting = buildPostingWithRelations(clubId, creatorId, command, RecruitmentStatus.OPEN, 1L);
        return recruitmentPostingRepository.save(posting);
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting temporaryStorageRecruitment(Long clubId, Long creatorId, Long postingId, CreateRecruitmentCommand command) {
        if (postingId == null) {
            RecruitmentPosting posting = buildPostingWithRelations(clubId, creatorId, command, RecruitmentStatus.DRAFT, 0L);
            return recruitmentPostingRepository.save(posting);
        }

        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (posting.getRecruitmentStatus() != RecruitmentStatus.DRAFT) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_DRAFT);
        }

        ClubGeneration generation = resolveGeneration(clubId, command.generation());

        validateDates(command);

        if (Boolean.TRUE.equals(command.hasSecondInterview())) {
            validateInterviewSettings(command);
        }

        int totalCapacity = command.departments().stream()
                .mapToInt(CreateRecruitmentCommand.DepartmentInfo::count)
                .sum();

        posting.getRecruitmentDepartments().clear();
        posting.getRecruitmentProcesses().clear();

        posting.updateForDraft(
                command.title(), command.contentJson(), generation,
                totalCapacity, toStartOfDay(command.startDate()), toEndOfDay(command.endDate()),
                command.resultDate(), command.endOfGenerationDate(), command.hasSecondInterview()
        );

        List<String> departmentNames = new ArrayList<>();
        for (CreateRecruitmentCommand.DepartmentInfo deptInfo : command.departments()) {
            ClubDepartment clubDepartment = clubDepartmentRepository.findByIdAndClubId(deptInfo.id(), clubId)
                    .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.DEPARTMENT_NOT_FOUND));
            departmentNames.add(clubDepartment.getName());

            RecruitmentDepartment recruitmentDepartment = RecruitmentDepartment.builder()
                    .recruitment(posting)
                    .department(clubDepartment)
                    .count(deptInfo.count())
                    .build();

            posting.addRecruitmentDepartment(recruitmentDepartment);
        }

        RecruitmentProcess documentProcess = RecruitmentProcess.builder()
                .posting(posting)
                .processOrder(1)
                .name("서류 접수")
                .processType(ProcessType.DOCUMENT)
                .startAt(toStartOfDay(command.startDate()))
                .endAt(toEndOfDay(command.endDate()))
                .build();

        posting.addRecruitmentProcess(documentProcess);

        if (Boolean.TRUE.equals(command.hasSecondInterview())) {
            RecruitmentProcess interviewProcess = RecruitmentProcess.builder()
                    .posting(posting)
                    .processOrder(2)
                    .name("면접")
                    .processType(ProcessType.INTERVIEW)
                    .startAt(toStartOfDay(command.interviewStartDate()))
                    .endAt(toEndOfDay(command.interviewEndDate()))
                    .interviewStartTime(command.interviewStartTime())
                    .interviewEndTime(command.interviewEndTime())
                    .build();

            posting.addRecruitmentProcess(interviewProcess);
        }

        String applicationFormJson = injectAutoComponents(command.applicationFormJson(), command, departmentNames);

        RecruitmentSchema schema = RecruitmentSchema.builder()
                .recruitmentProcess(documentProcess)
                .version(0L)
                .applicationForm(applicationFormJson)
                .build();

        documentProcess.addRecruitmentSchema(schema);

        return posting;
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting publishRecruitment(Long clubId, Long creatorId, Long postingId) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (posting.getRecruitmentStatus() != RecruitmentStatus.DRAFT) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_DRAFT);
        }

        posting.changeRecruitmentStatus(RecruitmentStatus.OPEN);
        posting.setPublishRecruitmentVersion();

        // 스키마 버전도 1로 갱신
        posting.getRecruitmentProcesses().stream()
                .filter(p -> p.getProcessType() == ProcessType.DOCUMENT)
                .findFirst()
                .ifPresent(documentProcess -> documentProcess.getRecruitmentSchemas().stream()
                        .filter(s -> s.getVersion() == 0L)
                        .forEach(s -> s.updateVersion(1L)));

        return posting;
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting updateRecruitment(Long clubId, Long creatorId, Long postingId, UpdateRecruitmentCommand command) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (posting.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_OPEN);
        }

        posting.updateTitle(command.title());
        posting.updateContent(command.contentJson());

        // 최신 스키마를 찾아 기존 폼과 비교
        RecruitmentProcess documentProcess = posting.getRecruitmentProcesses().stream()
                .filter(p -> p.getProcessType() == ProcessType.DOCUMENT)
                .findFirst()
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        RecruitmentSchema latestSchema = documentProcess.getRecruitmentSchemas().stream()
                .reduce((a, b) -> a.getVersion() > b.getVersion() ? a : b)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_SCHEMA_NOT_FOUND));

        String oldFormJson = latestSchema.getApplicationForm();
        String newFormJson = command.applicationFormJson();

        try {
            JsonNode oldForm = objectMapper.readTree(oldFormJson);
            JsonNode newForm = objectMapper.readTree(newFormJson);

            // 기존 컴포넌트 수정 불가 검증 (추가/삭제만 허용)
            validateNoComponentModification(oldForm, newForm);

            // 삭제된 컴포넌트 key 파악
            Set<String> oldKeys = extractFormKeys(oldForm);
            Set<String> newKeys = extractFormKeys(newForm);
            Set<String> removedKeys = new HashSet<>(oldKeys);
            removedKeys.removeAll(newKeys);

            // 새로 추가된 필수 컴포넌트 key 파악
            Set<String> addedRequiredKeys = extractRequiredKeys(newForm);
            addedRequiredKeys.removeAll(oldKeys);

            // 기존 SUBMITTED 지원자 처리
            if (!removedKeys.isEmpty() || !addedRequiredKeys.isEmpty()) {
                processExistingApplications(postingId, removedKeys, addedRequiredKeys);
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_FORM_SERIALIZE_ERROR);
        }

        // 2차 면접 여부 변경 처리
        if (command.hasSecondInterview() != null
                && !command.hasSecondInterview().equals(posting.getHasSecondInterview())) {
            handleInterviewChange(posting, documentProcess, newFormJson, command.hasSecondInterview());
        }

        latestSchema.updateApplicationForm(newFormJson);

        return posting;
    }

    /**
     * 기존 컴포넌트의 question/config가 변경되지 않았는지 검증합니다.
     * 동일 key를 가진 컴포넌트의 내용이 달라졌으면 예외를 발생시킵니다.
     */
    private void validateNoComponentModification(JsonNode oldForm, JsonNode newForm) {
        Map<String, JsonNode> oldComponents = extractComponentsByKey(oldForm);
        Map<String, JsonNode> newComponents = extractComponentsByKey(newForm);

        for (Map.Entry<String, JsonNode> entry : oldComponents.entrySet()) {
            String key = entry.getKey();
            JsonNode newComponent = newComponents.get(key);
            if (newComponent != null && !entry.getValue().equals(newComponent)) {
                throw new BusinessException(RecruitmentErrorCode.COMPONENT_MODIFICATION_NOT_ALLOWED);
            }
        }
    }

    private Map<String, JsonNode> extractComponentsByKey(JsonNode formNode) {
        Map<String, JsonNode> map = new HashMap<>();
        for (String fieldType : FormFieldType.allFieldNames()) {
            JsonNode arrayNode = formNode.get(fieldType);
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode element : arrayNode) {
                    JsonNode keyNode = element.get("key");
                    if (keyNode != null && keyNode.isTextual()) {
                        map.put(keyNode.asText(), element);
                    }
                }
            }
        }
        return map;
    }

    private Set<String> extractFormKeys(JsonNode formNode) {
        Set<String> keys = new HashSet<>();
        for (String fieldType : FormFieldType.allFieldNames()) {
            JsonNode arrayNode = formNode.get(fieldType);
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode element : arrayNode) {
                    JsonNode keyNode = element.get("key");
                    if (keyNode != null && keyNode.isTextual()) {
                        keys.add(keyNode.asText());
                    }
                }
            }
        }
        return keys;
    }

    private Set<String> extractRequiredKeys(JsonNode formNode) {
        Set<String> keys = new HashSet<>();
        for (String fieldType : FormFieldType.allFieldNames()) {
            JsonNode arrayNode = formNode.get(fieldType);
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode element : arrayNode) {
                    JsonNode keyNode = element.get("key");
                    JsonNode requiredNode = element.get("required");
                    if (keyNode != null && keyNode.isTextual()
                            && requiredNode != null && requiredNode.asBoolean()) {
                        keys.add(keyNode.asText());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * 기존 SUBMITTED 지원자의 답변에서 삭제된 컴포넌트 key를 제거하고,
     * 새로 추가된 필수 컴포넌트가 있으면 TEMPORARY 상태로 전환합니다.
     */
    private void processExistingApplications(Long postingId, Set<String> removedKeys, Set<String> addedRequiredKeys) {
        List<Application> submittedApps = applicationRepository
                .findActiveByPostingIdAndStatus(postingId, ApplicationStatus.SUBMITTED);

        for (Application app : submittedApps) {
            // 삭제된 컴포넌트의 답변 제거
            if (!removedKeys.isEmpty()) {
                removeAnswerKeys(app, removedKeys);
            }

            // 추가된 필수 컴포넌트가 있으면 임시저장 상태로 전환
            if (!addedRequiredKeys.isEmpty()) {
                app.updateApplicationStatus(ApplicationStatus.TEMPORARY);
            }
        }
    }

    private void removeAnswerKeys(Application app, Set<String> keysToRemove) {
        try {
            JsonNode answersNode = objectMapper.readTree(app.getAnswers());
            if (answersNode.isArray()) {
                ArrayNode filtered = objectMapper.createArrayNode();
                for (JsonNode answer : answersNode) {
                    String key = answer.path("key").asText("");
                    if (!keysToRemove.contains(key)) {
                        filtered.add(answer);
                    }
                }
                app.updateAnswers(objectMapper.writeValueAsString(filtered));
            }
        } catch (JsonProcessingException e) {
            // 답변 파싱 실패 시 무시 (기존 답변 유지)
        }
    }

    /**
     * 2차 면접 여부 변경 시 when2meet 자동 컴포넌트를 추가/삭제합니다.
     */
    private void handleInterviewChange(RecruitmentPosting posting, RecruitmentProcess documentProcess,
                                        String newFormJson, boolean hasSecondInterview) {
        posting.updateHasSecondInterview(hasSecondInterview);

        if (!hasSecondInterview) {
            // 면접 프로세스 제거
            posting.getRecruitmentProcesses().stream()
                    .filter(p -> p.getProcessType() == ProcessType.INTERVIEW)
                    .findFirst()
                    .ifPresent(posting::removeRecruitmentProcess);
        }
        // 면접 활성화 시 when2meet 컴포넌트는 프론트에서 폼에 포함하여 전달
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting changeDeadline(Long clubId, Long userId, Long postingId, OffsetDateTime newEndAt) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (posting.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_OPEN);
        }

        posting.changeDeadline(newEndAt);

        return posting;
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting closeRecruitment(Long clubId, Long userId, Long postingId,
                                                Integer additionalAcceptanceDays) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (posting.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_ALREADY_CLOSED);
        }

        if (!posting.getEndAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_EXPIRED);
        }

        long waitlistedCount = applicationRepository.countByPostingIdAndStatus(
                postingId, ApplicationStatus.WAITLISTED);

        if (waitlistedCount > 0) {
            if (additionalAcceptanceDays == null) {
                throw new BusinessException(RecruitmentErrorCode.WAITLISTED_APPLICANTS_EXIST);
            }
            if (additionalAcceptanceDays < 1 || additionalAcceptanceDays > 14) {
                throw new BusinessException(RecruitmentErrorCode.INVALID_ADDITIONAL_ACCEPTANCE_PERIOD);
            }
            posting.setExtraAcceptanceEndDate(
                    OffsetDateTime.now(ZoneOffset.UTC).plusDays(additionalAcceptanceDays));
        }

        posting.changeRecruitmentStatus(RecruitmentStatus.CLOSED);

        return posting;
    }

    @Transactional(readOnly = true)
    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public String getPreviousApplicationForm(Long clubId, Long userId) {
        return recruitmentPostingRepository
                .findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(clubId, RecruitmentStatus.DRAFT)
                .flatMap(posting -> posting.getRecruitmentProcesses().stream()
                        .filter(p -> p.getProcessType() == ProcessType.DOCUMENT)
                        .findFirst())
                .flatMap(process -> process.getRecruitmentSchemas().stream()
                        .reduce((a, b) -> a.getVersion() > b.getVersion() ? a : b))
                .map(RecruitmentSchema::getApplicationForm)
                .orElse(null);
    }

    public void incrementViewCount(Long postingId) {
        int updated = recruitmentPostingRepository.incrementViewCount(postingId);
        if (updated == 0) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<PublicPostingListResult> getPublicPostingList(Long clubId, String status) {
        List<RecruitmentStatus> statuses;
        if ("OPEN".equalsIgnoreCase(status)) {
            statuses = List.of(RecruitmentStatus.OPEN);
        } else if ("CLOSED".equalsIgnoreCase(status)) {
            statuses = List.of(RecruitmentStatus.CLOSED, RecruitmentStatus.ARCHIVED);
        } else {
            throw new BusinessException(RecruitmentErrorCode.INVALID_STATUS_FILTER);
        }

        List<RecruitmentPosting> postings = recruitmentPostingRepository
                .findPostingsForPublicList(clubId, statuses, BaseStatus.ACTIVE);

        if ("OPEN".equalsIgnoreCase(status)) {
            postings.sort((a, b) -> a.getEndAt().compareTo(b.getEndAt()));
        } else {
            postings.sort((a, b) -> b.getEndAt().compareTo(a.getEndAt()));
        }

        return postings.stream()
                .map(this::toPublicListResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicPostingDetailResult getPublicPostingDetail(Long clubId, Long postingId) {
        RecruitmentPosting posting = recruitmentPostingRepository
                .findPostingDetailForPublic(postingId, clubId, BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        return toPublicDetailResult(posting);
    }

    private PublicPostingListResult toPublicListResult(RecruitmentPosting posting) {
        List<String> departments = posting.getRecruitmentDepartments().stream()
                .map(rd -> rd.getDepartment().getName())
                .toList();

        return new PublicPostingListResult(
                posting.getId(),
                posting.getGeneration().getGenerationNo(),
                posting.getTitle(),
                posting.getEndAt(),
                departments,
                posting.getViewCount(),
                posting.getRecruitmentStatus()
        );
    }

    private PublicPostingDetailResult toPublicDetailResult(RecruitmentPosting posting) {
        List<PublicPostingDetailResult.DepartmentInfo> departments = posting.getRecruitmentDepartments().stream()
                .map(rd -> new PublicPostingDetailResult.DepartmentInfo(
                        rd.getDepartment().getName(),
                        rd.getCount()))
                .toList();

        List<PublicPostingDetailResult.ProcessInfo> processes = posting.getRecruitmentProcesses().stream()
                .map(rp -> new PublicPostingDetailResult.ProcessInfo(
                        rp.getProcessOrder(),
                        rp.getName(),
                        rp.getProcessType().name(),
                        rp.getStartAt(),
                        rp.getEndAt()))
                .toList();

        String applicationForm = posting.getRecruitmentProcesses().stream()
                .filter(p -> p.getProcessType() == ProcessType.DOCUMENT)
                .findFirst()
                .flatMap(process -> process.getRecruitmentSchemas().stream()
                        .reduce((a, b) -> a.getVersion() > b.getVersion() ? a : b))
                .map(RecruitmentSchema::getApplicationForm)
                .orElse(null);

        return new PublicPostingDetailResult(
                posting.getId(),
                posting.getClub().getId(),
                posting.getClub().getName(),
                posting.getGeneration().getGenerationNo(),
                posting.getTitle(),
                posting.getContent(),
                posting.getThemeColor(),
                posting.getCapacity(),
                posting.getStartAt(),
                posting.getEndAt(),
                posting.getResultDate(),
                posting.getEndOfGenerationDate(),
                posting.getHasSecondInterview(),
                posting.getEmergencyContact(),
                posting.getFirstAnnouncementDate(),
                departments,
                processes,
                applicationForm,
                posting.getViewCount(),
                posting.getRecruitmentStatus()
        );
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting deleteRecruitment(Long clubId, Long userId, Long postingId) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        posting.changeRecruitmentStatus(RecruitmentStatus.CLOSED);
        posting.inactivate();

        return posting;
    }

    private RecruitmentPosting buildPostingWithRelations(Long clubId, Long creatorId,
                                                         CreateRecruitmentCommand command,
                                                         RecruitmentStatus status, Long version) {
        Club club = entityManager.getReference(Club.class, clubId);
        User creator = entityManager.getReference(User.class, creatorId);

        ClubGeneration generation = resolveGeneration(clubId, command.generation());

        validateDates(command);

        if (Boolean.TRUE.equals(command.hasSecondInterview())) {
            validateInterviewSettings(command);
        }

        int totalCapacity = command.departments().stream()
                .mapToInt(CreateRecruitmentCommand.DepartmentInfo::count)
                .sum();

        RecruitmentPosting posting = RecruitmentPosting.builder()
                .club(club)
                .generation(generation)
                .creator(creator)
                .title(command.title())
                .content(command.contentJson())
                .capacity(totalCapacity)
                .recruitmentStatus(status)
                .recentRecruitmentVersion(version)
                .startAt(toStartOfDay(command.startDate()))
                .endAt(toEndOfDay(command.endDate()))
                .resultDate(command.resultDate())
                .endOfGenerationDate(command.endOfGenerationDate())
                .hasSecondInterview(command.hasSecondInterview())
                .emergencyContact(command.emergencyContact())
                .firstAnnouncementDate(command.firstAnnouncementDate())
                .build();

        List<String> departmentNames = new ArrayList<>();
        for (CreateRecruitmentCommand.DepartmentInfo deptInfo : command.departments()) {
            ClubDepartment clubDepartment = clubDepartmentRepository.findByIdAndClubId(deptInfo.id(), clubId)
                    .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.DEPARTMENT_NOT_FOUND));
            departmentNames.add(clubDepartment.getName());

            RecruitmentDepartment recruitmentDepartment = RecruitmentDepartment.builder()
                    .recruitment(posting)
                    .department(clubDepartment)
                    .count(deptInfo.count())
                    .build();

            posting.addRecruitmentDepartment(recruitmentDepartment);
        }

        RecruitmentProcess documentProcess = RecruitmentProcess.builder()
                .posting(posting)
                .processOrder(1)
                .name("서류 접수")
                .processType(ProcessType.DOCUMENT)
                .startAt(toStartOfDay(command.startDate()))
                .endAt(toEndOfDay(command.endDate()))
                .build();

        posting.addRecruitmentProcess(documentProcess);

        if (Boolean.TRUE.equals(command.hasSecondInterview())) {
            RecruitmentProcess interviewProcess = RecruitmentProcess.builder()
                    .posting(posting)
                    .processOrder(2)
                    .name("면접")
                    .processType(ProcessType.INTERVIEW)
                    .startAt(toStartOfDay(command.interviewStartDate()))
                    .endAt(toEndOfDay(command.interviewEndDate()))
                    .interviewStartTime(command.interviewStartTime())
                    .interviewEndTime(command.interviewEndTime())
                    .build();

            posting.addRecruitmentProcess(interviewProcess);
        }

        String applicationFormJson = injectAutoComponents(command.applicationFormJson(), command, departmentNames);

        RecruitmentSchema schema = RecruitmentSchema.builder()
                .recruitmentProcess(documentProcess)
                .version(version)
                .applicationForm(applicationFormJson)
                .build();

        documentProcess.addRecruitmentSchema(schema);

        return posting;
    }

    /**
     * 조건에 따라 자동 컴포넌트를 지원서 양식 JSON에 주입합니다.
     * - 부서가 2개 이상: 부서 선택 다지선다 컴포넌트 (필수, 삭제 불가)
     * - 면접 필수: 면접 일정 when2meet 컴포넌트 (필수, 삭제 불가)
     */
    private String injectAutoComponents(String applicationFormJson,
                                        CreateRecruitmentCommand command,
                                        List<String> departmentNames) {
        try {
            ObjectNode formNode = (ObjectNode) objectMapper.readTree(applicationFormJson);

            if (departmentNames.size() > 1) {
                injectDepartmentSelectComponent(formNode, departmentNames);
            }

            if (Boolean.TRUE.equals(command.hasSecondInterview())) {
                injectInterviewScheduleComponent(formNode, command);
            }

            return objectMapper.writeValueAsString(formNode);
        } catch (JsonProcessingException e) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_FORM_SERIALIZE_ERROR);
        }
    }

    private void injectDepartmentSelectComponent(ObjectNode formNode, List<String> departmentNames) {
        ArrayNode choiceArray = formNode.withArray(FormFieldType.CHOICE.getFieldName());

        if (hasAutoComponent(choiceArray, AutoComponentType.DEPARTMENT_SELECT.getKeyPrefix())) {
            return;
        }

        ObjectNode component = objectMapper.createObjectNode();
        component.put("orderNumber", 0);
        component.put("key", AutoComponentType.DEPARTMENT_SELECT.getKeyPrefix() + UUID.randomUUID());
        component.put("question", "지원 부서를 선택해주세요.");
        ArrayNode options = component.putArray("options");
        departmentNames.forEach(options::add);
        component.put("others", false);
        component.putNull("maxOthersLength");
        component.put("required", true);

        choiceArray.insert(0, component);
    }

    private void injectInterviewScheduleComponent(ObjectNode formNode, CreateRecruitmentCommand command) {
        ArrayNode when2meetArray = formNode.withArray(FormFieldType.WHEN2MEET.getFieldName());

        if (hasAutoComponent(when2meetArray, AutoComponentType.INTERVIEW_SCHEDULE.getKeyPrefix())) {
            return;
        }

        ObjectNode component = objectMapper.createObjectNode();
        component.put("orderNumber", 0);
        component.put("key", AutoComponentType.INTERVIEW_SCHEDULE.getKeyPrefix() + UUID.randomUUID());
        component.put("question", "면접 가능한 시간을 선택해주세요.");
        component.putNull("maxSelect");
        component.put("timeZone", "Asia/Seoul");
        component.put("startTime", command.interviewStartTime().toString());
        component.put("endTime", command.interviewEndTime().toString());
        component.put("startDate", command.interviewStartDate().toString());
        component.put("endDate", command.interviewEndDate().toString());
        component.put("required", true);

        when2meetArray.insert(0, component);
    }

    private boolean hasAutoComponent(ArrayNode arrayNode, String prefix) {
        for (JsonNode node : arrayNode) {
            if (node.path("key").asText().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }


    private ClubGeneration resolveGeneration(Long clubId, Integer generationNo) {
        if (generationNo != null) {
            return clubGenerationRepository
                    .findByClubIdAndGenerationNo(clubId, generationNo)
                    .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.GENERATION_NOT_FOUND));
        }

        // 기수 자동 증가: 마지막 기수 + 1
        int nextGenerationNo = clubGenerationRepository
                .findFirstByClubIdOrderByGenerationNoDesc(clubId)
                .map(g -> g.getGenerationNo() + 1)
                .orElse(1);

        ClubGeneration newGeneration = ClubGeneration.builder()
                .club(entityManager.getReference(Club.class, clubId))
                .generationNo(nextGenerationNo)
                .startDate(LocalDate.now())
                .build();

        return clubGenerationRepository.save(newGeneration);
    }

    private void validateDates(CreateRecruitmentCommand command) {
        if (command.endDate().isBefore(command.startDate())) {
            throw new BusinessException(RecruitmentErrorCode.INVALID_DATE_RANGE,
                    "모집 마감일은 시작일 이후여야 합니다.");
        }
        if (command.resultDate().isBefore(command.endDate())) {
            throw new BusinessException(RecruitmentErrorCode.INVALID_DATE_RANGE,
                    "결과 발표일은 모집 마감일 이후여야 합니다.");
        }
    }

    private void validateInterviewSettings(CreateRecruitmentCommand command) {
        if (command.interviewStartDate() == null || command.interviewEndDate() == null) {
            throw new BusinessException(RecruitmentErrorCode.INVALID_INTERVIEW_SETTING,
                    "면접 시작일과 종료일은 필수입니다.");
        }
        if (command.interviewEndDate().isBefore(command.interviewStartDate())) {
            throw new BusinessException(RecruitmentErrorCode.INVALID_DATE_RANGE,
                    "면접 종료일은 시작일 이후여야 합니다.");
        }
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay(KST).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    private OffsetDateTime toEndOfDay(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(KST).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    /**
     * Service 계층에서 사용하는 Command 객체.
     * Controller의 DTO와 분리하여 모듈 간 의존성을 제거합니다.
     */
    public record CreateRecruitmentCommand(
            String title,
            Integer generation,
            String contentJson,
            List<DepartmentInfo> departments,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate resultDate,
            LocalDate endOfGenerationDate,
            Boolean hasSecondInterview,
            LocalDate interviewStartDate,
            LocalDate interviewEndDate,
            LocalTime interviewStartTime,
            LocalTime interviewEndTime,
            String applicationFormJson,
            String emergencyContact,
            LocalDate firstAnnouncementDate
    ) {

        public record DepartmentInfo(
                Long id,
                Integer count
        ) {
        }
    }

    public record UpdateRecruitmentCommand(
            String title,
            String contentJson,
            String applicationFormJson,
            Boolean hasSecondInterview
    ) {
    }
}
