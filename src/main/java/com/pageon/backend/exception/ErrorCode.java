package com.pageon.backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 회원
    USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND("존재하지 않는 권한입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    PASSWORD_POLICY_VIOLATION("비밀번호는 8자 이상, 영문, 숫자, 특수문자(!@-#$%&^)를 모두 포함해야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_PROVIDER_TYPE("지원하지 않는 OAuth Provider입니다.", HttpStatus.BAD_REQUEST),
    OAUTH_UNLINK_FAILED("OAuth 연결 해제에 실패했습니다.", HttpStatus.BAD_REQUEST),
    CREATOR_PERMISSION_DENIED("creator 권한이 존재하지 않습니다.", HttpStatus.FORBIDDEN),
    COMMENT_FORBIDDEN("본인 댓글만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    INSUFFICIENT_POINTS("포인트가 부족합니다.", HttpStatus.BAD_REQUEST),

    // 본인인증
    INVALID_VERIFICATION_METHOD("지원하지 않는 본인인증 방식입니다.", HttpStatus.BAD_REQUEST),
    IDENTITY_VERIFICATION_ID_NOT_MATCH("전달된 인증 ID가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    OTP_PAYLOAD_NOT_FOUND("OTP 정보가 존재하지 않거나 만료되었습니다.", HttpStatus.NOT_FOUND),
    OTP_NOT_MATCH("전달된 OTP가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    IDENTITY_ALREADY_VERIFIED("이미 본인인증을 완료한 사용자입니다.", HttpStatus.CONFLICT),
    PHONE_NUMBER_ALREADY_VERIFIED("해당 전화번호는 이미 본인인증에 사용되었습니다.", HttpStatus.CONFLICT),
    INVALID_IDENTITY_NUMBER("유효하지 않은 주민등록번호 형식입니다.", HttpStatus.BAD_REQUEST),

    // 메세지 전송
    MESSAGE_SEND_FAILED("문자 메시지 전송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 작가
    AUTHENTICATION_REQUIRED_TO_REGISTER_AS_CREATOR("작가 등록을 위해서는 본인인증이 필요합니다.", HttpStatus.FORBIDDEN),
    PEN_NAME_REQUIRED("필명은 반드시 입력해야 합니다.", HttpStatus.BAD_REQUEST),
    ALREADY_HAS_CREATOR_ROLE("이미 창작자 권한이 존재합니다.", HttpStatus.BAD_REQUEST),
    AI_POLICY_NOT_AGREED("AI 콘텐츠 등록 약관에 동의하지 않았습니다.", HttpStatus.BAD_REQUEST),
    CREATOR_NOT_FOUND("존재하지 않는 작가입니다.", HttpStatus.NOT_FOUND),
    NOT_CREATOR_OF_WEBTOON("웹툰 업로드 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_CREATOR_OF_WEBNOVEL("웹소설 업로드 권한이 없습니다.",  HttpStatus.FORBIDDEN),
    CREATOR_UNAUTHORIZED_ACCESS("해당 콘텐츠의 작성자가 아닙니다.", HttpStatus.FORBIDDEN),
    INVALID_PUBLISHED_AT("연재 시작일이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    // 토큰
    TOKEN_GENERATION_FAILED("Refresh Token 또는 Access Token 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    REFRESH_TOKEN_NOT_FOUND("Refresh Token이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_USER_MISMATCH("토큰 사용자 정보가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_ACCESS_TOKEN_NOT_FOUND("소셜 로그인 Access Token이 존재하지 않습니다.", HttpStatus.NOT_FOUND ),

    // 외부 시스템 오류
    REDIS_CONNECTION_FAILED("Redis 연결에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    MAIL_SEND_FAILED("메일 전송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    S3_UPLOAD_FAILED("S3 업로드 중 오류가 발생했습니다.",HttpStatus.INTERNAL_SERVER_ERROR),
    S3_DELETE_FAILED("S3 데이터 삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // file
    FILE_PROCESSING_ERROR("파일의 MIME 타입을 읽는 데 실패했습니다.", HttpStatus.BAD_REQUEST),

    // request
    INVALID_INPUT("입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATION_REQUEST("중복된 요청입니다.", HttpStatus.BAD_REQUEST),

    // 작품
    WEBNOVEL_NOT_FOUND("존재하지 않는 웹소설입니다.", HttpStatus.NOT_FOUND),
    WEBTOON_NOT_FOUND("존재하지 않는 웹툰입니다.", HttpStatus.NOT_FOUND),
    CONTENT_NOT_FOUND("존재하지 않는 콘텐츠입니다.", HttpStatus.NOT_FOUND),
    CONTENT_IS_DELETED("삭제된 콘텐츠입니다.", HttpStatus.NOT_FOUND),
    EPISODE_NOT_FOUND("해당 에피소드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CONTENT_TYPE("지원하지 않는 콘텐츠 타입입니다. webnovel 또는 webtoon만 가능합니다.", HttpStatus.BAD_REQUEST),
    INTEREST_NOT_FOUND("해당 사용자와 콘텐츠의 관심 정보가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    EPISODE_RATING_NOT_FOUND("해당 에피소드에 저장된 평점이 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_TEXT_IS_BLANK("댓글 내용이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    EPISODE_IS_DELETED("삭제된 에피소드입니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("존재하지 않는 댓글입니다.", HttpStatus.NOT_FOUND),
    COMMENT_ALREADY_DELETED("이미 삭제된 댓글입니다.", HttpStatus.CONFLICT),
    COMMENT_ALREADY_LIKED("이미 좋아요한 댓글입니다.", HttpStatus.CONFLICT),
    COMMENT_LIKE_NOT_FOUND("사용자가 좋아요를 하지 않은 댓글입니다.", HttpStatus.NOT_FOUND),
    EPISODE_ALREADY_PURCHASE("이미 구매한 에피소드입니다.", HttpStatus.CONFLICT),
    EPISODE_ALREADY_RENTAL("이미 대여한 에피소드입니다.", HttpStatus.CONFLICT),
    INVALID_PURCHASE_TYPE("해당 구매 방식은 지원하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_KEYWORD("유효한 키워드가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    INVALID_SEARCH_QUERY("유효한 검색어가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("카테고리가 존재하지 않습니다.", HttpStatus.NOT_FOUND ),


    //
    INVALID_SERIAL_DAY("해당하는 요일이 없습니다.", HttpStatus.BAD_REQUEST),

    POINT_TRANSACTION_NOT_FOUND("결제 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_PAYMENT_CONFIRM("이미 처리된 결제입니다.", HttpStatus.BAD_REQUEST),
    AMOUNT_NOT_MATCH("결제 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED("결제 처리에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_TEMP_CODE("임시 코드가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOSS_CLIENT_ERROR("토스 API 연결에 실패하였습니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_POINTS_FOR_REFUND("환불할 포인트 잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    REFUND_PERIOD_EXPIRED("환불 가능 기간이 지났습니다.", HttpStatus.BAD_REQUEST),
    REFUND_STATUS_INVALID("취소되지 않은 결제입니다.", HttpStatus.BAD_REQUEST),
    REFUND_API_FAILED("환불 API 호출에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_API_FAILED("결제 API 호출에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_NOT_COMPLETED("완료된 결제만 취소할 수 있습니다.", HttpStatus.BAD_REQUEST),
    DELETION_REQUEST_NOT_FOUND("콘텐츠 삭제 요청이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    INVALID_CANCEL_DELETE_REQUEST("콘텐츠 삭제 요청을 취소할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_URL("유효한 파일 경로가 아닙니다.", HttpStatus.NOT_FOUND),
    FILE_EMPTY("업로드된 파일이 없습니다.", HttpStatus.BAD_REQUEST),
    NOT_CONTENT_OWNER("콘텐츠 작성자가 아닙니다.", HttpStatus.FORBIDDEN),
    DUPLICATE_SEQUENCE("웹툰 에피소드 이미지 순서가 중복되었습니다.", HttpStatus.BAD_REQUEST),
    INVALID_SEQUENCE("웹툰 에피소드 이미지 순서가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_FOUND("웹툰 에피소드 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_BANK_CODE("유효하지 않은 은행 코드입니다.", HttpStatus.BAD_REQUEST),
    INVALID_BANK_ACCOUNT("계좌 인증에 실패했습니다.", HttpStatus.BAD_REQUEST),
    ENCRYPTION_FAILED("계좌번호 암호화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    DECRYPTION_FAILED("계좌번호 복호화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    BANK_ACCOUNT_NOT_FOUND("계좌 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REVENUE_NOT_FOUND("수익 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;




    private final String errorMessage;
    private final HttpStatus httpStatus;

}
