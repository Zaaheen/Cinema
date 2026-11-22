package com.cinema.service.impl;

import com.cinema.common.PageResponse;
import com.cinema.dto.price.SeatPriceAdminResponse;
import com.cinema.dto.price.SeatPriceAuditResponse;
import com.cinema.dto.price.SeatPriceRequest;
import com.cinema.dto.price.SeatPriceUpdateRequest;
import com.cinema.entity.SeatPrice;
import com.cinema.entity.SeatPriceAuditLog;
import com.cinema.entity.User;
import com.cinema.exception.BusinessException;
import com.cinema.repository.SeatPriceAuditLogRepository;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.repository.UserRepository;
import com.cinema.service.SeatPriceAdminService;
import com.cinema.service.SeatPriceCanonicalizer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class SeatPriceAdminServiceImpl implements SeatPriceAdminService {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SeatPriceRepository seatPriceRepository;
    private final SeatPriceAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public SeatPriceAdminServiceImpl(SeatPriceRepository seatPriceRepository,
            SeatPriceAuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.seatPriceRepository = seatPriceRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SeatPriceAdminResponse> list(String status, String seatType, String movieVersion,
            LocalDate effectiveOn, int page, int size) {
        Page<SeatPrice> rules = seatPriceRepository.findAdminRules(normalizeStatus(status),
                normalizeSeatType(seatType, false), SeatPriceCanonicalizer.key(movieVersion), effectiveOn,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, List<SeatPriceAuditResponse>> history = historyByRuleId(rules.getContent().stream()
                .map(SeatPrice::getId).toList());
        return PageResponse.from(rules.map(rule -> SeatPriceAdminResponse.from(rule,
                effectiveState(rule, LocalDate.now(CINEMA_ZONE)), history.getOrDefault(rule.getId(), List.of()))));
    }

    @Override
    public SeatPriceAdminResponse createDraft(SeatPriceRequest request) {
        validateRule(request.seatType(), request.movieVersion(), request.amount(), request.validFrom(), request.validTo());
        User actor = currentAdmin();
        SeatPrice rule = new SeatPrice();
        apply(rule, request.seatType(), request.movieVersion(), request.amount(), request.validFrom(), request.validTo());
        rule.setStatus("DRAFT");
        rule.setOrigin("ADMIN_APPROVED");
        rule.setCreatedByUser(actor);
        rule.setUpdatedByUser(actor);
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());
        SeatPrice saved = saveWithConflictMapping(rule);
        SeatPriceAuditResponse audit = audit(saved, actor, "CREATE", null);
        return SeatPriceAdminResponse.from(saved, effectiveState(saved, LocalDate.now(CINEMA_ZONE)), List.of(audit));
    }

    @Override
    public SeatPriceAdminResponse update(Long id, SeatPriceUpdateRequest request) {
        SeatPrice existing = seatPriceRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quy tắc giá"));
        if (!"ADMIN_APPROVED".equals(existing.getOrigin())) {
            throw BusinessException.notFound("Không tìm thấy quy tắc giá");
        }
        if (!Objects.equals(existing.getRowVersion(), request.rowVersion())) {
            throw BusinessException.conflict("PRICE_RULE_CHANGED");
        }
        User actor = currentAdmin();
        PriceValues next = merged(existing, request);
        validateRule(next.seatType(), next.movieVersion(), next.amount(), next.validFrom(), next.validTo());
        String nextStatus = normalizeStatus(request.status());
        if (nextStatus == null) {
            nextStatus = existing.getStatus();
        }
        if ("ACTIVE".equals(existing.getStatus()) && pricingFieldsChanged(existing, next)) {
            return reviseActive(existing, next, nextStatus, actor);
        }
        if (!isStatus(nextStatus)) {
            throw BusinessException.badRequest("Trạng thái giá vé không hợp lệ");
        }
        String before = snapshot(existing);
        String previousStatus = existing.getStatus();
        apply(existing, next.seatType(), next.movieVersion(), next.amount(), next.validFrom(), next.validTo());
        existing.setStatus(nextStatus);
        existing.setUpdatedByUser(actor);
        existing.setUpdatedAt(Instant.now());
        SeatPrice saved = saveWithConflictMapping(existing);
        String action = "ACTIVE".equals(nextStatus) && !"ACTIVE".equals(previousStatus)
                ? "ACTIVATE" : "INACTIVE".equals(nextStatus) ? "DEACTIVATE" : "UPDATE";
        SeatPriceAuditResponse audit = audit(saved, actor, action, before);
        return SeatPriceAdminResponse.from(saved, effectiveState(saved, LocalDate.now(CINEMA_ZONE)), List.of(audit));
    }

    private SeatPriceAdminResponse reviseActive(SeatPrice prior, PriceValues next, String requestedStatus, User actor) {
        if (requestedStatus != null && !"ACTIVE".equals(requestedStatus)) {
            throw BusinessException.badRequest("Quy tắc ACTIVE chỉ được sửa bằng successor đang ACTIVE");
        }
        String before = snapshot(prior);
        prior.setStatus("INACTIVE");
        prior.setUpdatedByUser(actor);
        prior.setUpdatedAt(Instant.now());
        SeatPrice retired = saveWithConflictMapping(prior);
        audit(retired, actor, "REVISE", before);

        SeatPrice successor = new SeatPrice();
        apply(successor, next.seatType(), next.movieVersion(), next.amount(), next.validFrom(), next.validTo());
        successor.setStatus("ACTIVE");
        successor.setOrigin("ADMIN_APPROVED");
        successor.setCreatedByUser(actor);
        successor.setUpdatedByUser(actor);
        successor.setCreatedAt(Instant.now());
        successor.setUpdatedAt(Instant.now());
        SeatPrice saved = saveWithConflictMapping(successor);
        SeatPriceAuditResponse audit = audit(saved, actor, "CREATE", null);
        return SeatPriceAdminResponse.from(saved, effectiveState(saved, LocalDate.now(CINEMA_ZONE)), List.of(audit));
    }

    private SeatPrice saveWithConflictMapping(SeatPrice rule) {
        try {
            return seatPriceRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("PRICE_RULE_CONFLICT");
        }
    }

    private SeatPriceAuditResponse audit(SeatPrice rule, User actor, String action, String before) {
        SeatPriceAuditLog log = new SeatPriceAuditLog();
        log.setSeatPrice(rule);
        log.setRevision(rule.getRowVersion());
        log.setActorUser(actor);
        log.setAction(action);
        log.setBeforeState(before);
        log.setAfterState(snapshot(rule));
        log.setCreatedAt(Instant.now());
        return SeatPriceAuditResponse.from(auditLogRepository.save(log));
    }

    private Map<Long, List<SeatPriceAuditResponse>> historyByRuleId(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return auditLogRepository.findBySeatPriceIdInOrderByCreatedAtDescIdDesc(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getSeatPrice().getId(),
                        Collectors.mapping(SeatPriceAuditResponse::from, Collectors.toList())));
    }

    private User currentAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.unauthorized("Không tìm thấy tài khoản quản trị"));
    }

    private PriceValues merged(SeatPrice existing, SeatPriceUpdateRequest request) {
        return new PriceValues(request.seatType() == null ? existing.getSeatType() : request.seatType(),
                request.movieVersion() == null ? existing.getMovieVersion() : request.movieVersion(),
                request.amount() == null ? existing.getPrice() : request.amount(),
                request.validFrom() == null ? existing.getValidFrom() : request.validFrom(),
                request.validTo() == null ? existing.getValidTo() : request.validTo());
    }

    private boolean pricingFieldsChanged(SeatPrice existing, PriceValues values) {
        return !Objects.equals(existing.getSeatType(), normalizeSeatType(values.seatType(), true))
                || !Objects.equals(existing.getMovieVersionKey(), SeatPriceCanonicalizer.key(values.movieVersion()))
                || existing.getPrice().compareTo(values.amount()) != 0
                || !Objects.equals(existing.getValidFrom(), values.validFrom())
                || !Objects.equals(existing.getValidTo(), values.validTo());
    }

    private void apply(SeatPrice target, String seatType, String movieVersion, BigDecimal amount,
            LocalDate validFrom, LocalDate validTo) {
        target.setSeatType(normalizeSeatType(seatType, true));
        target.setMovieVersion(SeatPriceCanonicalizer.display(movieVersion));
        target.setMovieVersionKey(SeatPriceCanonicalizer.key(movieVersion));
        target.setPrice(amount);
        target.setValidFrom(validFrom);
        target.setValidTo(validTo);
    }

    private void validateRule(String seatType, String movieVersion, BigDecimal amount, LocalDate validFrom,
            LocalDate validTo) {
        normalizeSeatType(seatType, true);
        if (amount == null || amount.signum() <= 0) throw BusinessException.badRequest("Giá vé phải lớn hơn 0");
        if (validFrom == null || (validTo != null && validTo.isBefore(validFrom))) {
            throw BusinessException.badRequest("Khoảng thời gian áp dụng không hợp lệ");
        }
    }

    private String normalizeSeatType(String seatType, boolean required) {
        if (!StringUtils.hasText(seatType)) {
            if (required) throw BusinessException.badRequest("Loại ghế là bắt buộc");
            return null;
        }
        String normalized = seatType.trim().toUpperCase();
        if (!"STANDARD".equals(normalized) && !"VIP".equals(normalized)) {
            throw BusinessException.badRequest("Loại ghế không hợp lệ");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }

    private boolean isStatus(String status) {
        return "DRAFT".equals(status) || "ACTIVE".equals(status) || "INACTIVE".equals(status);
    }

    private String effectiveState(SeatPrice rule, LocalDate today) {
        if (!"ACTIVE".equals(rule.getStatus())) return "INACTIVE";
        if (rule.getValidFrom().isAfter(today)) return "UPCOMING";
        if (rule.getValidTo() != null && rule.getValidTo().isBefore(today)) return "EXPIRED";
        return "EFFECTIVE";
    }

    private String snapshot(SeatPrice price) {
        return "{\"seatType\":\"%s\",\"movieVersion\":%s,\"amount\":\"%s\",\"validFrom\":\"%s\",\"validTo\":%s,\"status\":\"%s\",\"origin\":\"%s\",\"rowVersion\":%s}"
                .formatted(jsonString(price.getSeatType()), nullableJsonString(price.getMovieVersion()),
                        price.getPrice(), price.getValidFrom(), nullableJsonString(price.getValidTo()),
                        jsonString(price.getStatus()), jsonString(price.getOrigin()), price.getRowVersion());
    }

    private String nullableJsonString(Object value) {
        return value == null ? "null" : "\"" + jsonString(value.toString()) + "\"";
    }

    private String jsonString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private record PriceValues(String seatType, String movieVersion, BigDecimal amount, LocalDate validFrom,
            LocalDate validTo) {}
}
