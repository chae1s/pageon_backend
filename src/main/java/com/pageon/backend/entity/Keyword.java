package com.pageon.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "keyword", cascade = CascadeType.ALL)
    private List<ContentKeyword> contentKeywords = new ArrayList<>();

    private LocalDate startDate;
    private LocalDate endDate;

    public Keyword(Category category, String name) {
        this.category = category;
        this.name = name;
    }

    public void updateTime(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }


}