package com.pageon.backend.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableUtil {

    public static Pageable searchPageable(Pageable pageable, String sort) {
        Sort sortOrder = switch (sort) {
            case "latest" -> Sort.by(Sort.Order.desc("episodeUpdatedAt"));
            case "rating" -> Sort.by(Sort.Order.desc("totalAverageRating"));
            default -> Sort.by(Sort.Order.desc("viewCount"));
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
    }

    public static Pageable interestPageable(Pageable pageable, String sort) {
        Sort sortOrder = switch (sort) {
            case "update" -> Sort.by(Sort.Order.desc("c.episodeUpdatedAt"));
            case "title" -> Sort.by(Sort.Order.asc("c.title"));
            default -> Sort.by(Sort.Order.desc("i.createdAt"));
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
    }

    public static Pageable readingHistoryPageable(Pageable pageable, String sort) {

        Sort sortOrder = switch (sort) {

            case "recently_read" -> Sort.by(Sort.Order.desc("r.lastReadAt"));
            default -> Sort.by(Sort.Order.desc("c.episodeUpdatedAt"));
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
    }

    public static Pageable commentPageable(Pageable pageable, String sort) {
        Sort sortOrder = switch (sort) {
            case "latest" -> Sort.by(Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("likeCount"));
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
    }

    public static Pageable redisPageable(int size, String sort) {
        Sort sortOrder = Sort.by(Sort.Order.desc(sort));

        return PageRequest.of(0, size, sortOrder);
    }

    public static Pageable moreContentPageable(Pageable pageable, String sort) {
        Sort sortOrder = Sort.by(Sort.Order.desc(sort));

        return PageRequest.of(pageable.getPageNumber(), 60, sortOrder);
    }


    public static Pageable creatorContentPageable(Pageable pageable, String sort) {

        Sort sortOrder;

        if (sort.equals("update")) {
            sortOrder = Sort.by(Sort.Order.desc("publishedAt"));
        } else {
            sortOrder = Sort.by(Sort.Order.desc("episodeUpdatedAt"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
    }

    public static Pageable dashboardPageable(Pageable pageable, String sort) {
        Sort sortOrder = switch (sort) {
            case "latest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "published" -> Sort.by(Sort.Order.desc("publishedAt"));
            default -> Sort.by(Sort.Order.desc("viewCount"));
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
    }
}
