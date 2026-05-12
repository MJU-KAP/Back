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

    @Column(name = "category", length = 50)
    private String category;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_skills", nullable = false, columnDefinition = "jsonb")
    private List<String> requiredSkills = new ArrayList<>();

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "origin_url", columnDefinition = "TEXT")
    private String originUrl;

    public void update(String title, String category, List<String> requiredSkills, LocalDate deadline) {
        this.title = title;
        this.category = category;
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
        this.deadline = deadline;
    }
}
