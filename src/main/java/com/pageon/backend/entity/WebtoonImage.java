package com.pageon.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@Table(name = "webtoon_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebtoonImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer sequence;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "webtoonEpisode_id")
    private WebtoonEpisode webtoonEpisode;

    public void addEpisode(WebtoonEpisode episode) {
        this.webtoonEpisode = episode;
    }
}
