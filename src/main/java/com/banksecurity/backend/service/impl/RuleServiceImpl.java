package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.RuleRequest;
import com.banksecurity.backend.dto.response.RuleResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ForbiddenException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Rule;
import com.banksecurity.backend.model.Zone;
import com.banksecurity.backend.model.enums.RuleType;
import com.banksecurity.backend.repository.RuleRepository;
import com.banksecurity.backend.repository.ZoneRepository;
import com.banksecurity.backend.security.UserPrincipal;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.RuleService;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final ZoneRepository zoneRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== MÉTHODES CRUD ====================

    @Override
    @Transactional
    public RuleResponse createRule(RuleRequest request) {
        if (request.getPriority() != null && !ValidationUtils.isValidPriority(request.getPriority())) {
            throw new BadRequestException("La priorité doit être entre 1 et " + Constants.MAX_RULE_PRIORITY);
        }

        // ✅ ValidationUtils.isInvalidSensitivity (méthode négative)
        if (request.getSensitivity() != null && ValidationUtils.isInvalidSensitivity(request.getSensitivity())) {
            throw new BadRequestException("La sensibilité doit être entre 0 et 100");
        }

        Rule rule = Rule.builder()
                .name(request.getName())
                .type(request.getType())
                .parameters(request.getParameters())
                .thresholdTime(request.getThresholdTime())
                .sensitivity(request.getSensitivity() != null ? request.getSensitivity() : Constants.DEFAULT_ZONE_SENSITIVITY)
                .priority(request.getPriority() != null ? request.getPriority() : Constants.DEFAULT_RULE_PRIORITY)
                .description(request.getDescription())
                .isActive(true)
                .build();

        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
            rule.setZone(zone);
        }

        rule = ruleRepository.save(rule);

        auditLogService.logAction(null, Constants.AUDIT_ACTION_CREATE + "_RULE", "Création règle: " + rule.getName());
        log.info("Règle créée: {}", rule.getName());

        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public RuleResponse updateRule(UUID id, RuleRequest request) {
        try {
            Rule rule = ruleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));

            // ✅ ValidationUtils.isInvalidSensitivity (méthode négative)
            if (request.getSensitivity() != null && ValidationUtils.isInvalidSensitivity(request.getSensitivity())) {
                throw new BadRequestException("La sensibilité doit être entre 0 et 100");
            }

            rule.setName(request.getName());
            rule.setType(request.getType());
            rule.setParameters(request.getParameters());
            rule.setThresholdTime(request.getThresholdTime());
            rule.setDescription(request.getDescription());

            if (request.getSensitivity() != null) {
                rule.setSensitivity(request.getSensitivity());
            }

            if (request.getPriority() != null) {
                rule.setPriority(request.getPriority());
            }

            if (request.getZoneId() != null) {
                if (rule.getZone() == null || !rule.getZone().getId().equals(request.getZoneId())) {
                    Zone zone = zoneRepository.findById(request.getZoneId())
                            .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
                    rule.setZone(zone);
                }
            }

            rule = ruleRepository.save(rule);

            auditLogService.logAction(null, Constants.AUDIT_ACTION_UPDATE + "_RULE", "Mise à jour règle: " + rule.getName());

            return mapToResponse(rule);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la règle: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour de la règle: " + id, e);
        }
    }

    @Override
    @Transactional
    public void deleteRule(UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Vous n'avez pas les permissions pour supprimer une règle");
        }

        try {
            Rule rule = ruleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));

            ruleRepository.delete(rule);

            auditLogService.logAction(null, Constants.AUDIT_ACTION_DELETE + "_RULE", "Suppression règle: " + rule.getName());
            log.info("Règle supprimée: {}", rule.getName());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la règle: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la suppression de la règle: " + id, e);
        }
    }

    @Override
    public RuleResponse getRuleById(UUID id) {
        try {
            Rule rule = ruleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));
            return mapToResponse(rule);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la règle: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la récupération de la règle: " + id, e);
        }
    }

    @Override
    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RuleResponse> getRulesByZone(UUID zoneId) {
        return ruleRepository.findByZoneId(zoneId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RuleResponse> getRulesByType(RuleType type) {
        return ruleRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RuleResponse toggleRuleStatus(UUID id, boolean isActive) {
        try {
            Rule rule = ruleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));

            rule.setIsActive(isActive);
            rule = ruleRepository.save(rule);

            auditLogService.logAction(null, "TOGGLE_RULE_STATUS",
                    "Statut règle " + rule.getName() + " -> " + (isActive ? "active" : "inactive"));

            return mapToResponse(rule);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du changement de statut de la règle: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du changement de statut de la règle: " + id, e);
        }
    }

    // ==================== ÉVALUATION DES RÈGLES ====================

    @Override
    public boolean evaluateRule(UUID ruleId, Object context) {
        try {
            Rule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", ruleId));

            if (!rule.getIsActive()) {
                return false;
            }

            switch (rule.getType()) {
                case INTRUSION:
                    return evaluateIntrusionRule(rule, context);
                case PRESENCE_PROLONGEE:
                    return evaluatePresenceRule(rule, context);
                case OBJET_SUSPECT:
                    return evaluateObjectRule(rule, context);
                case SKIMMER:
                    return evaluateSkimmerRule(rule, context);
                case ARME:
                    return evaluateWeaponRule(rule, context);
                case CHUTE:
                    return evaluateFallRule(rule, context);
                case RODEUR:
                    return evaluateLoiteringRule(rule, context);
                case MASQUE:
                    return evaluateMaskRule(rule, context);
                default:
                    return false;
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation de la règle: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de l'évaluation de la règle: " + ruleId, e);
        }
    }

    @Override
    public List<RuleResponse> getActiveRules() {
        return ruleRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countRules() {
        return ruleRepository.count();
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    public List<RuleResponse> getInactiveRules() {
        return ruleRepository.findByIsActiveFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RuleResponse> getRulesByZoneAndType(UUID zoneId, RuleType type) {
        return ruleRepository.findByZoneIdAndType(zoneId, type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RuleResponse getRuleWithZone(UUID id) {
        Rule rule = ruleRepository.findByIdWithZone(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));
        return mapToResponse(rule);
    }

    public List<RuleResponse> getRulesByPriorityGreaterThan(Integer priority) {
        return ruleRepository.findByPriorityGreaterThanEqual(priority).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long countActiveRulesByType(RuleType type) {
        return ruleRepository.countActiveByType(type);
    }

    public List<RuleResponse> getRulesByThresholdTimeGreaterThan(Integer thresholdTime) {
        return ruleRepository.findByThresholdTimeGreaterThan(thresholdTime).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RuleResponse> getAllRulesOrderedByPriority() {
        return ruleRepository.findAllOrderByPriorityDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RuleResponse> getActiveRulesWithZones() {
        return ruleRepository.findActiveRulesWithZones().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== MÉTHODES DE DÉTECTION ====================

    private boolean evaluateIntrusionRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            log.warn("Contexte invalide pour la détection d'intrusion: {}", context);
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        Boolean personDetected = (Boolean) ctx.get("personDetected");
        if (personDetected == null || !personDetected) {
            return false;
        }

        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        if (rule.getZone() != null) {
            String detectedZoneId = (String) ctx.get("zoneId");
            if (detectedZoneId != null && !detectedZoneId.equals(rule.getZone().getId().toString())) {
                return false;
            }
        }

        log.info("Intrusion détectée par la règle: {}", rule.getName());
        return true;
    }

    private boolean evaluatePresenceRule(Rule rule, Object context) {
        long presenceTime = 0;

        if (context instanceof Long) {
            presenceTime = (Long) context;
        } else if (context instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ctx = (Map<String, Object>) context;
            Object timeObj = ctx.get("presenceTime");
            if (timeObj instanceof Number) {
                presenceTime = ((Number) timeObj).longValue();
            }
        }

        long threshold = rule.getThresholdTime() != null
                ? rule.getThresholdTime()
                : Constants.DEFAULT_PRESENCE_THRESHOLD;

        boolean exceeded = presenceTime >= threshold;
        if (exceeded) {
            log.info("Présence prolongée détectée: {} secondes (seuil: {})",
                    presenceTime, threshold);
        }
        return exceeded;
    }

    private boolean evaluateObjectRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        String objectClass = (String) ctx.get("objectClass");
        if (objectClass == null) {
            return false;
        }

        List<String> suspiciousClasses = List.of("backpack", "suitcase", "bag", "package");
        boolean isSuspicious = suspiciousClasses.stream()
                .anyMatch(sc -> objectClass.toLowerCase().contains(sc.toLowerCase()));

        if (!isSuspicious) {
            return false;
        }

        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        Long stationaryTime = getLongValue(ctx.get("stationaryTime"));
        if (rule.getThresholdTime() != null && stationaryTime != null) {
            if (stationaryTime < rule.getThresholdTime()) {
                return false;
            }
        }

        log.info("Objet suspect détecté: {} par la règle: {}", objectClass, rule.getName());
        return true;
    }

    private boolean evaluateSkimmerRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        String objectClass = (String) ctx.get("objectClass");
        if (objectClass == null) {
            return false;
        }

        boolean isSkimmer = objectClass.toLowerCase().contains("skimmer") ||
                objectClass.toLowerCase().contains("device") ||
                objectClass.toLowerCase().contains("card_reader");

        if (!isSkimmer) {
            return false;
        }

        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        log.warn("Skimmer potentiel détecté par la règle: {}", rule.getName());
        return true;
    }

    private boolean evaluateWeaponRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        String objectClass = (String) ctx.get("objectClass");
        if (objectClass == null) {
            return false;
        }

        List<String> weaponClasses = List.of("knife", "gun", "pistol", "rifle", "weapon");
        boolean isWeapon = weaponClasses.stream()
                .anyMatch(wc -> objectClass.toLowerCase().contains(wc.toLowerCase()));

        if (!isWeapon) {
            return false;
        }

        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        log.error("Arme détectée: {} par la règle: {}", objectClass, rule.getName());
        return true;
    }

    private boolean evaluateFallRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        Boolean fallDetected = (Boolean) ctx.get("fallDetected");
        if (fallDetected == null || !fallDetected) {
            return false;
        }

        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        Long duration = getLongValue(ctx.get("duration"));
        if (rule.getThresholdTime() != null && duration != null) {
            if (duration < rule.getThresholdTime()) {
                return false;
            }
        }

        log.warn("Chute détectée par la règle: {}", rule.getName());
        return true;
    }

    private boolean evaluateLoiteringRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        Long loiteringTime = getLongValue(ctx.get("loiteringTime"));
        if (rule.getThresholdTime() != null && loiteringTime != null) {
            if (loiteringTime < rule.getThresholdTime()) {
                return false;
            }
        }

        String movementPattern = (String) ctx.get("movementPattern");
        if (movementPattern != null) {
            List<String> suspiciousPatterns = List.of("circular", "repetitive", "zigzag");
            boolean isSuspicious = suspiciousPatterns.stream()
                    .anyMatch(sp -> movementPattern.toLowerCase().contains(sp));

            if (isSuspicious) {
                log.info("Rôdeur détecté avec pattern {}: par la règle: {}",
                        movementPattern, rule.getName());
                return true;
            }
        }

        Integer directionChanges = getIntegerValue(ctx.get("directionChanges"));
        if (directionChanges != null && rule.getParameters() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = objectMapper.readValue(rule.getParameters(), Map.class);
                Integer maxDirectionChanges = (Integer) params.get("maxDirectionChanges");
                if (maxDirectionChanges != null && directionChanges >= maxDirectionChanges) {
                    log.info("Rôdeur détecté avec {} changements de direction", directionChanges);
                    return true;
                }
            } catch (Exception e) {
                log.error("Erreur lors du parsing des paramètres de la règle: {}", e.getMessage());
            }
        }

        return loiteringTime != null && rule.getThresholdTime() != null &&
                loiteringTime >= rule.getThresholdTime();
    }

    private boolean evaluateMaskRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        Boolean maskDetected = (Boolean) ctx.get("maskDetected");
        if (maskDetected == null || !maskDetected) {
            return false;
        }

        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        log.warn("Personne masquée détectée par la règle: {}", rule.getName());
        return true;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private double getSensitivityThreshold(Integer sensitivity) {
        if (sensitivity == null) {
            return Constants.DEFAULT_CONFIDENCE_THRESHOLD;
        }
        return 1.0 - (sensitivity / 100.0) * 0.8;
    }

    private Double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long getLongValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer getIntegerValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private RuleResponse mapToResponse(Rule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .zoneId(rule.getZone() != null ? rule.getZone().getId() : null)
                .zoneName(rule.getZone() != null ? rule.getZone().getName() : null)
                .type(rule.getType())
                .parameters(rule.getParameters())
                .thresholdTime(rule.getThresholdTime())
                .sensitivity(rule.getSensitivity())
                .priority(rule.getPriority())
                .isActive(rule.getIsActive())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}