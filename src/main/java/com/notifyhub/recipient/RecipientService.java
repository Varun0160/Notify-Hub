package com.notifyhub.recipient;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.exception.DuplicateResourceException;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.recipient.dto.CreateRecipientRequest;
import com.notifyhub.recipient.dto.PreferenceResponse;
import com.notifyhub.recipient.dto.RecipientResponse;
import com.notifyhub.recipient.dto.UpdateRecipientRequest;
import com.notifyhub.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final RecipientPreferenceRepository preferenceRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public RecipientResponse create(Long tenantId, CreateRecipientRequest request) {
        String externalId = request.externalId().trim();
        if (recipientRepository.existsByTenantIdAndExternalId(tenantId, externalId)) {
            throw new DuplicateResourceException("Recipient already exists: " + externalId);
        }

        String timezone = (request.timezone() == null || request.timezone().isBlank())
                ? "UTC" : request.timezone().trim();
        validate(request.email(), request.phone(), request.deviceToken(),
                timezone, request.quietHoursStart(), request.quietHoursEnd());

        Recipient recipient = new Recipient(tenantRepository.getReferenceById(tenantId), externalId);
        apply(recipient, request.email(), request.phone(), request.deviceToken(),
                timezone, request.quietHoursStart(), request.quietHoursEnd());

        return RecipientResponse.from(recipientRepository.save(recipient));
    }

    @Transactional(readOnly = true)
    public RecipientResponse get(Long tenantId, String recipientId) {
        return RecipientResponse.from(findOwned(tenantId, recipientId));
    }

    @Transactional
    public RecipientResponse update(Long tenantId, String recipientId, UpdateRecipientRequest request) {
        Recipient recipient = findOwned(tenantId, recipientId);

        String timezone = (request.timezone() == null || request.timezone().isBlank())
                ? "UTC" : request.timezone().trim();
        validate(request.email(), request.phone(), request.deviceToken(),
                timezone, request.quietHoursStart(), request.quietHoursEnd());

        apply(recipient, request.email(), request.phone(), request.deviceToken(),
                timezone, request.quietHoursStart(), request.quietHoursEnd());
        return RecipientResponse.from(recipient);       // dirty checking saves it at commit
    }

    @Transactional(readOnly = true)
    public List<PreferenceResponse> getPreferences(Long tenantId, String recipientId) {
        Recipient recipient = findOwned(tenantId, recipientId);

        Map<Channel, Boolean> stored = new EnumMap<>(Channel.class);
        preferenceRepository.findByRecipientId(recipient.getId())
                .forEach(p -> stored.put(p.getChannel(), p.isEnabled()));

        // No row means "never changed", which means enabled.
        return java.util.Arrays.stream(Channel.values())
                .map(ch -> new PreferenceResponse(ch, stored.getOrDefault(ch, true)))
                .toList();
    }

    @Transactional
    public PreferenceResponse setPreference(Long tenantId, String recipientId,
                                            Channel channel, boolean enabled) {
        Recipient recipient = findOwned(tenantId, recipientId);

        RecipientPreference preference = preferenceRepository
                .findByRecipientIdAndChannel(recipient.getId(), channel)
                .orElseGet(() -> new RecipientPreference(recipient, channel, enabled));
        preference.setEnabled(enabled);
        preferenceRepository.save(preference);

        return new PreferenceResponse(channel, enabled);
    }

    // The isolation point: every lookup is scoped to the caller's tenant.
    private Recipient findOwned(Long tenantId, String recipientId) {
        return recipientRepository.findByPublicIdAndTenantId(recipientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found: " + recipientId));
    }

    private void validate(String email, String phone, String deviceToken,
                          String timezone, LocalTime quietStart, LocalTime quietEnd) {
        if (isBlank(email) && isBlank(phone) && isBlank(deviceToken)) {
            throw new InvalidRequestException(
                    "At least one of email, phone or deviceToken is required");
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new InvalidRequestException(
                    "Unknown timezone '" + timezone + "'. Use an IANA name such as Asia/Kolkata");
        }
        if ((quietStart == null) != (quietEnd == null)) {
            throw new InvalidRequestException(
                    "quietHoursStart and quietHoursEnd must be provided together");
        }
        if (quietStart != null && quietStart.equals(quietEnd)) {
            throw new InvalidRequestException("quietHoursStart and quietHoursEnd must differ");
        }
    }

    private void apply(Recipient r, String email, String phone, String deviceToken,
                       String timezone, LocalTime quietStart, LocalTime quietEnd) {
        r.setEmail(isBlank(email) ? null : email.trim().toLowerCase());
        r.setPhone(isBlank(phone) ? null : phone.trim());
        r.setDeviceToken(isBlank(deviceToken) ? null : deviceToken.trim());
        r.setTimezone(timezone);
        r.setQuietHoursStart(quietStart);
        r.setQuietHoursEnd(quietEnd);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}