
package com.pageon.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.util.*;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@DiscriminatorValue("WEBTOON")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Webtoon extends Content {

    @Builder.Default
    @OneToMany(mappedBy = "webtoon", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<WebtoonEpisode> webtoonEpisodes = new ArrayList<>();


}
