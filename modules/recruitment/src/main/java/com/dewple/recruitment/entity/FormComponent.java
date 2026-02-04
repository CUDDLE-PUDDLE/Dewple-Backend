package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.enums.InputType;
import com.dewple.club.entity.ClubDepartment;

@Entity
@Table(name = "form_component"
//, indexes = { @Index(name = "idx_form_component_sort", columnList = "club_id, sort_order") }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormComponent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private ClubDepartment department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FormComponent parent;

    @Column(name = "parent_select_number")
    private Long parentSelectNumber;

    @Column(name = "component_key", nullable = false, length = 32)
    private String componentKey;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 30)
    private InputType inputType;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @Column(name = "max_length")
    private Integer maxLength;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private String config;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    
    @Builder
    public FormComponent(Club club, ClubDepartment department, FormComponent parent,
                         Long parentSelectNumber, String componentKey, String question,
                         InputType inputType, Boolean required, Integer maxLength,
                         String config, Integer sortOrder) {
        this.club = club;
        this.department = department;
        this.parent = parent;
        this.parentSelectNumber = parentSelectNumber;
        this.componentKey = componentKey;
        this.question = question;
        this.inputType = inputType;
        this.required = required != null ? required : false;
        this.maxLength = maxLength;
        this.config = config;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
