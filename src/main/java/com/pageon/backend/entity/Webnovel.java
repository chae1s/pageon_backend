package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.dto.request.ContentUpdateRequest;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@DiscriminatorValue("WEBNOVEL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Webnovel extends Content {


    @Builder.Default
    @OneToMany(mappedBy = "webnovel", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<WebnovelEpisode> webnovelEpisodes = new ArrayList<>();




}