package com.notifyhub.notification;

import com.notifyhub.common.PageResponse;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long tenantId, NotificationStatus status,
                                                   Channel channel, String recipientId,
                                                   Instant from, Instant to,
                                                   Integer limit, String cursor) {
        int pageSize = (limit == null) ? DEFAULT_LIMIT : limit;
        if (pageSize < 1 || pageSize > MAX_LIMIT) {
            throw new InvalidRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidRequestException("'from' must be before 'to'");
        }
        Long beforeId = PageCursor.decode(cursor);
        String recipient = (recipientId == null || recipientId.isBlank()) ? null : recipientId.trim();

        // Ask for one extra row: if it exists there is a next page.
        List<Notification> rows = notificationRepository.search(
                tenantId, beforeId, status, channel, recipient, from, to,
                PageRequest.of(0, pageSize + 1));

        boolean hasMore = rows.size() > pageSize;
        List<Notification> page = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasMore ? PageCursor.encode(page.get(pageSize - 1).getId()) : null;

        return new PageResponse<>(page.stream().map(NotificationResponse::from).toList(), nextCursor);
    }
}