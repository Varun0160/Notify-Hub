package com.notifyhub.notification;

import com.notifyhub.common.exception.DuplicateResourceException;
import com.notifyhub.common.exception.IdempotencyConflictException;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.common.exception.RateLimitExceededException;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.notification.dto.BatchItemResult;
import com.notifyhub.notification.dto.BatchSendRequest;
import com.notifyhub.notification.dto.BatchSendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchSendService {

    private final IdempotentSendService idempotentSendService;

    // Deliberately NOT @Transactional: every item gets its own transaction inside the send call.
    public BatchSendResponse send(Long tenantId, BatchSendRequest request) {
        List<BatchSendRequest.Item> items = request.items();
        List<BatchItemResult> results = new ArrayList<>(items.size());
        int accepted = 0;

        for (int i = 0; i < items.size(); i++) {
            BatchSendRequest.Item item = items.get(i);
            try {
                SendResult result = idempotentSendService.send(
                        tenantId, item.notification(), item.idempotencyKey());
                results.add(BatchItemResult.accepted(i, result));
                accepted++;
            } catch (Exception ex) {
                results.add(toError(i, ex));       // one bad item never stops the others
            }
        }
        return new BatchSendResponse(items.size(), accepted, items.size() - accepted, results);
    }

    private BatchItemResult toError(int index, Exception ex) {
        if (ex instanceof ResourceNotFoundException) {
            return BatchItemResult.error(index, 404, ex.getMessage(), null);
        }
        if (ex instanceof InvalidRequestException) {
            return BatchItemResult.error(index, 400, ex.getMessage(), null);
        }
        if (ex instanceof IdempotencyConflictException) {
            return BatchItemResult.error(index, 422, ex.getMessage(), null);
        }
        if (ex instanceof RateLimitExceededException rl) {
            return BatchItemResult.error(index, 429, rl.getMessage(), rl.getRetryAfterSeconds());
        }
        if (ex instanceof DuplicateResourceException || ex instanceof DataIntegrityViolationException) {
            return BatchItemResult.error(index, 409, "Request conflicts with existing data", null);
        }
        log.error("Unexpected error in batch item {}", index, ex);
        return BatchItemResult.error(index, 500, "Internal error", null);   // never leak internals
    }
}