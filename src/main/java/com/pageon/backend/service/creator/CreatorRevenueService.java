package com.pageon.backend.service.creator;

import com.pageon.backend.dto.response.creator.content.ContentRevenue;
import com.pageon.backend.dto.response.creator.settlement.*;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.repository.creator.CreatorEarningRepository;
import com.pageon.backend.repository.creator.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorRevenueService {

    private static final double PLATFORM_FEE_RATE = 0.3;
    private static final double TAX_FEE_RATE = 0.033;

    private final CreatorRepository creatorRepository;
    private final SettlementRepository settlementRepository;
    private final CreatorEarningRepository creatorEarningRepository;


    public RevenueDetail getLatestRevenue(Long userId) {
        Creator creator = creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );

        return settlementRepository.findLatestSettlement(creator.getId()).orElse(null);
    }

    public RevenueDashboard getRevenueDashboard(Long userId) {
        Creator creator = getCreator(userId);

        LocalDateTime startDate = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        log.info("startDate:{}", startDate);
        Long totalPoint = creatorEarningRepository.sumMonthlyEarnings(creator.getId(), startDate);

        LocalDateTime payoutDate = startDate.plusMonths(1).withDayOfMonth(10);

        int platformFee = (int) (totalPoint * PLATFORM_FEE_RATE);
        int taxFee = (int) (totalPoint * TAX_FEE_RATE);
        int settledPoint = Math.toIntExact(totalPoint - platformFee - taxFee);

        return RevenueDashboard.builder()
                .totalPoint(totalPoint.intValue())
                .settledPoint(settledPoint)
                .payoutDate(payoutDate)
                .build();

    }

    public RevenueAnalytics getRevenueAnalytics(Long userId) {
        Creator creator = getCreator(userId);

        List<Object[]> allContentRevenue = creatorEarningRepository.findRevenueByContents(creator.getId());

        List<ContentRevenue> contents = new ArrayList<>();

        Long totalRevenue = creatorEarningRepository.sumTotalEarnings(creator.getId());

        if (totalRevenue == 0) {
            return RevenueAnalytics.builder().build();
        }

        Long rankTotalRevenue = 0L;

        for (int i = 0; i < allContentRevenue.size(); i++) {
            Object[] row = allContentRevenue.get(i);
            Long contentId = (Long) row[0];
            String contentTitle = (String) row[1];
            Long revenue = (Long) row[2];

            contents.add(new ContentRevenue(contentTitle, revenue, calculatePercentage(revenue, totalRevenue)));

            rankTotalRevenue += revenue;

        }

        Long etcRevenue = totalRevenue - rankTotalRevenue;
        contents.add(new ContentRevenue("기타", etcRevenue, calculatePercentage(etcRevenue, totalRevenue)));

        return RevenueAnalytics.builder()
                .totalRevenue(totalRevenue)
                .contents(contents)
                .build();
    }

    public RevenueTrend getDailyRevenue(Long userId) {
        Creator creator = getCreator(userId);

        LocalDateTime startDate = LocalDate.now().minusDays(6).atStartOfDay();

        List<DailyRevenue> dailyRevenues = creatorEarningRepository.findDailyRevenue(creator.getId(), startDate);

        Map<LocalDate, Long> revenueMap = dailyRevenues.stream()
                .collect(Collectors.toMap(DailyRevenue::getDate, DailyRevenue::getRevenue));

        if (dailyRevenues.size() != 7) {
            return RevenueTrend.of(fillMissingDates(revenueMap, startDate));
        }

        return RevenueTrend.of(dailyRevenues);
    }

    private List<DailyRevenue> fillMissingDates(Map<LocalDate, Long> revenueMap, LocalDateTime startDate) {
        List<DailyRevenue> fullTrend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = startDate.plusDays(i).toLocalDate();

            Long revenue = revenueMap.getOrDefault(targetDate, 0L);

            fullTrend.add(new DailyRevenue(targetDate, revenue));
        }

        return fullTrend;
    }

    private Double calculatePercentage(Long revenue, Long total) {
        if (total == 0) return 0.0;

        return  (double) revenue / total * 100;
    }

    private Creator getCreator(Long userId) {
        return creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );
    }

}
