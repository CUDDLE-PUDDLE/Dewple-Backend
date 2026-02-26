package com.dewple.recruitment.service;

import com.dewple.club.annotation.RequireClubPermission;
import com.dewple.club.entity.ClubDepartment;
import com.dewple.club.entity.ClubGeneration;
import com.dewple.club.repository.ClubDepartmentRepository;
import com.dewple.club.repository.ClubGenerationRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.EditWindowBasis;
import com.dewple.common.enums.Permission;
import com.dewple.common.enums.ProcessType;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.AutoComponentType;
import com.dewple.recruitment.entity.FormFieldType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RecruitmentPostingRepository recruitmentPostingRepository;
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

        ClubGeneration generation = clubGenerationRepository
                .findByClubIdAndGenerationNo(clubId, command.generation())
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.GENERATION_NOT_FOUND));

        validateDates(command);

        if (Boolean.TRUE.equals(command.isInterviewRequired())) {
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
                command.resultDate(), command.endOfGenerationDate(), command.isInterviewRequired()
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

        if (Boolean.TRUE.equals(command.isInterviewRequired())) {
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

        RecruitmentProcess documentProcess = posting.getRecruitmentProcesses().stream()
                .filter(p -> p.getProcessType() == ProcessType.DOCUMENT)
                .findFirst()
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        String latestFormJson = documentProcess.getRecruitmentSchemas().stream()
                .reduce((a, b) -> a.getVersion() > b.getVersion() ? a : b)
                .map(RecruitmentSchema::getApplicationForm)
                .orElse(null);

        validateApplicationFormUpdate(latestFormJson, command.applicationFormJson());

        posting.updateTitle(command.title());
        posting.updateContent(command.contentJson());
        posting.incrementVersion();

        RecruitmentSchema newSchema = RecruitmentSchema.builder()
                .recruitmentProcess(documentProcess)
                .version(posting.getRecentRecruitmentVersion())
                .applicationForm(command.applicationFormJson())
                .build();

        documentProcess.addRecruitmentSchema(newSchema);

        return posting;
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    public RecruitmentPosting closeRecruitment(Long clubId, Long userId, Long postingId) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (posting.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_ALREADY_CLOSED);
        }

        if (!posting.getEndAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_EXPIRED);
        }

        posting.changeRecruitmentStatus(RecruitmentStatus.CLOSED);

        return posting;
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

        ClubGeneration generation = clubGenerationRepository
                .findByClubIdAndGenerationNo(clubId, command.generation())
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.GENERATION_NOT_FOUND));

        validateDates(command);

        if (Boolean.TRUE.equals(command.isInterviewRequired())) {
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
                .editWindowBasis(EditWindowBasis.SUBMITTED)
                .editWindowDays(0)
                .capacity(totalCapacity)
                .recruitmentStatus(status)
                .recentRecruitmentVersion(version)
                .startAt(toStartOfDay(command.startDate()))
                .endAt(toEndOfDay(command.endDate()))
                .resultDate(command.resultDate())
                .endOfGenerationDate(command.endOfGenerationDate())
                .isInterviewRequired(command.isInterviewRequired())
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

        if (Boolean.TRUE.equals(command.isInterviewRequired())) {
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

            if (Boolean.TRUE.equals(command.isInterviewRequired())) {
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

    private void validateApplicationFormUpdate(String existingFormJson, String newFormJson) {
        if (existingFormJson == null) {
            return;
        }

        try {
            Set<String> existingKeys = extractFormKeys(objectMapper.readTree(existingFormJson));
            Set<String> newKeys = extractFormKeys(objectMapper.readTree(newFormJson));

            if (!newKeys.containsAll(existingKeys)) {
                throw new BusinessException(RecruitmentErrorCode.FORM_COMPONENT_REMOVAL_NOT_ALLOWED);
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_FORM_SERIALIZE_ERROR);
        }
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
            Boolean isInterviewRequired,
            LocalDate interviewStartDate,
            LocalDate interviewEndDate,
            LocalTime interviewStartTime,
            LocalTime interviewEndTime,
            String applicationFormJson
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
            String applicationFormJson
    ) {
    }
}
