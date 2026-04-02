package com.pageon.backend.service.creator;

import com.pageon.backend.common.enums.EarningStatus;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.CreatorEarning;
import com.pageon.backend.entity.PointTransaction;
import com.pageon.backend.repository.creator.CreatorEarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreatorEarningService {
    private CreatorEarningRepository creatorEarningRepository;

    public void registerCreatorEarning(Content content, PointTransaction pointTransaction) {

        CreatorEarning creatorEarning = CreatorEarning.builder()
                .point(pointTransaction.getPoint())
                .content(content)
                .creator(content.getCreator())
                .pointTransactionId(pointTransaction.getId())
                .earningStatus(EarningStatus.EARNED)
                .build();

        creatorEarningRepository.save(creatorEarning);

    }
}
