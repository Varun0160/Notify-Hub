package com.notifyhub.template.dto;

import java.util.Map;

public record PreviewRequest(Integer version, Map<String, String> variables) {
}