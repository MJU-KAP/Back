package com.example.NextPlan.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "external_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ExternalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ext_id")
    private Integer extId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_skills", nullable = false, columnDefinition = "jsonb")
    private List<String> requiredSkills = new ArrayList<>();

    @Column(name = "recruit_end_date")
    private LocalDate recruitEndDate;

    @Column(name = "origin_url", columnDefinition = "TEXT")
    private String originUrl;

    @Column(name = "homepage_url", columnDefinition = "TEXT")
    private String homepageUrl;

    @Column(name = "company_type", length = 100)
    private String companyType;

    @Column(name = "target_audience", length = 255)
    private String targetAudience;

    @Column(name = "recruit_start_date")
    private LocalDate recruitStartDate;

    @Column(name = "image_uri", columnDefinition = "TEXT")
    private String imageUri;

    @Column(name = "activity_benefit", length = 255)
    private String activityBenefit;

    @Column(name = "extra_benefit", length = 255)
    private String extraBenefit;

    @Column(name = "activity_start_date")
    private LocalDate activityStartDate;

    @Column(name = "activity_end_date")
    private LocalDate activityEndDate;

    @Column(name = "recruit_count")
    private Integer recruitCount;

    @Column(name = "activity_region", length = 255)
    private String activityRegion;

    @Column(name = "interest_field", length = 255)
    private String interestField;

    @Column(name = "award_scale", length = 255)
    private String awardScale;

    @Column(name = "contest_field", length = 255)
    private String contestField;

    @Column(name = "description", columnDefinition = "TEXT")
    private String detail;

    public void update(
            String title,
            String category,
            List<String> requiredSkills,
            LocalDate recruitEndDate,
            String originUrl,
            String homepageUrl,
            String companyType,
            String targetAudience,
            LocalDate recruitStartDate,
            String imageUri,
            String activityBenefit,
            String extraBenefit,
            LocalDate activityStartDate,
            LocalDate activityEndDate,
            Integer recruitCount,
            String activityRegion,
            String interestField,
            String awardScale,
            String contestField,
            String detail
    ) {
        this.title = title;
        this.category = category;
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
        this.recruitEndDate = recruitEndDate;
        this.originUrl = originUrl;
        this.homepageUrl = homepageUrl;
        this.companyType = companyType;
        this.targetAudience = targetAudience;
        this.recruitStartDate = recruitStartDate;
        this.imageUri = imageUri;
        this.activityBenefit = activityBenefit;
        this.extraBenefit = extraBenefit;
        this.activityStartDate = activityStartDate;
        this.activityEndDate = activityEndDate;
        this.recruitCount = recruitCount;
        this.activityRegion = activityRegion;
        this.interestField = interestField;
        this.awardScale = awardScale;
        this.contestField = contestField;
        this.detail = detail;
    }
}
