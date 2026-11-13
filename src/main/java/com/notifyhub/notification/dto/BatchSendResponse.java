package com.notifyhub.notification.dto;

import java.util.List;

public record BatchSendResponse(int total, int accepted, int failed, List<BatchItemResult> results) {
}