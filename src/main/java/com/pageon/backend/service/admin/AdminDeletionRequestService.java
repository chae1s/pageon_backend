package com.pageon.backend.service.admin;

import com.pageon.backend.common.enums.DeleteStatus;
import com.pageon.backend.dto.response.admin.content.DeletionRequestDetail;
import com.pageon.backend.dto.response.admin.content.DeletionRequestSummary;
import com.pageon.backend.entity.ContentDeletionRequest;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentDeletionRequestRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDeletionRequestService {

    private final ContentDeletionRequestRepository contentDeletionRequestRepository;
    private final List<EpisodeProvider> providers;

    public Page<DeletionRequestSummary> getAllDeletionRequests(String status, Pageable pageable) {
        Page<ContentDeletionRequest> requests = contentDeletionRequestRepository.findAllByDeleteStatus(DeleteStatus.valueOf(status), pageable);

        return requests.map(DeletionRequestSummary::of);
    }

    public DeletionRequestDetail getDeletionRequestDetail(Long requestId) {
        return contentDeletionRequestRepository.findRequestById(requestId).orElseThrow(
                () -> new CustomException(ErrorCode.DELETION_REQUEST_NOT_FOUND)
        );
    }

    @Transactional
    public void approveDeletionRequest(Long requestId) {
        ContentDeletionRequest deletionRequest = contentDeletionRequestRepository.findById(requestId).orElseThrow(
                () -> new CustomException(ErrorCode.DELETION_REQUEST_NOT_FOUND)
        );

        deletionRequest.approveDeletion();
        deletionRequest.getContent().deletionCompleted();
        episodeDelete(deletionRequest.getContent().getDtype(), deletionRequest.getContent().getId());
    }

    private void episodeDelete(String dType, Long contentId) {
        String contentType = (dType.equals("WEBNOVEL")) ? "webnovels" : "webtoons";

        EpisodeProvider provider = getProvider(contentType);

        provider.deleteAllEpisode(contentId);
    }

    @Transactional
    public void rejectDeletionRequest(Long requestId) {
        ContentDeletionRequest deletionRequest = contentDeletionRequestRepository.findById(requestId).orElseThrow(
                () -> new CustomException(ErrorCode.DELETION_REQUEST_NOT_FOUND)
        );

        deletionRequest.rejectDeletion();
        deletionRequest.getContent().rejectDeletion();
    }

    private EpisodeProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
