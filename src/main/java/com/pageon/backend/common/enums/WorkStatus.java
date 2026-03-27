package com.pageon.backend.common.enums;

public enum WorkStatus {
    DRAFT,      // 임시 저장
    PENDING,    // 검토 중
    PUBLISHED,  // 연재 중
    DELETING,   // 삭제 진행 중
    DELETED     // 삭제 완료
}
