package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.RuleRequest;
import com.banksecurity.backend.dto.response.RuleResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Rule;
import com.banksecurity.backend.model.Zone;
import com.banksecurity.backend.model.enums.RuleType;
import com.banksecurity.backend.repository.RuleRepository;
import com.banksecurity.backend.repository.ZoneRepository;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.RuleService;
import com.banksecurity.backend.util.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        // Valider la priorité
        if (request.getPriority() != null && !ValidationUtils.isValidPriority(request.getPriority())) {
            throw new BadRequestException("La priorité doit être entre 1 et 10");
        }

        // Valider la sensibilité
        if (request.getSensitivity() != null && !ValidationUtils.isValidSensitivity(request.getSensitivity())) {
            throw new BadRequestException("La sensibilité doit être entre 0 et 100");
        }

        Rule rule = Rule.builder()
                .name(request.getName())
                .type(request.getType())
                .parameters(request.getParameters())
                .thresholdTime(request.getThresholdTime())
                .sensitivity(request.getSensitivity() != null ? request.getSensitivity() : 50)
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .description(request.getDescription())
                .isActive(true)
                .build();

        // Associer la zone si fournie
        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
            rule.setZone(zone);
        }

        rule = ruleRepository.save(rule);

        auditLogService.logAction(null, "CREATE_RULE", "Création règle: " + rule.getName());
        log.info("Règle créée: {}", rule.getName());

        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public RuleResponse updateRule(UUID id, RuleRequest request) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));

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

        // Mettre à jour la zone si changée
        if (request.getZoneId() != null) {
            if (rule.getZone() == null || !rule.getZone().getId().equals(request.getZoneId())) {
                Zone zone = zoneRepository.findById(request.getZoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
                rule.setZone(zone);
            }
        }

        rule = ruleRepository.save(rule);

        auditLogService.logAction(null, "UPDATE_RULE", "Mise à jour règle: " + rule.getName());

        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public void deleteRule(UUID id) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));

        ruleRepository.delete(rule);

        auditLogService.logAction(null, "DELETE_RULE", "Suppression règle: " + rule.getName());
        log.info("Règle supprimée: {}", rule.getName());
    }

    @Override
    public RuleResponse getRuleById(UUID id) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));
        return mapToResponse(rule);
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
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", id));

        rule.setIsActive(isActive);
        rule = ruleRepository.save(rule);

        auditLogService.logAction(null, "TOGGLE_RULE_STATUS",
                "Statut règle " + rule.getName() + " -> " + (isActive ? "active" : "inactive"));

        return mapToResponse(rule);
    }

    // ==================== ÉVALUATION DES RÈGLES ====================

    @Override
    public boolean evaluateRule(UUID ruleId, Object context) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", ruleId));

        if (!rule.getIsActive()) {
            return false;
        }

        // Évaluation basée sur le type de règle
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

    // ==================== MÉTHODES DE DÉTECTION ====================

    /**
     * Détection d'intrusion dans une zone restreinte
     * Contexte attendu: Map avec "personDetected", "zoneId", "confidence"
     */
    private boolean evaluateIntrusionRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            log.warn("Contexte invalide pour la détection d'intrusion: {}", context);
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Vérifier si une personne est détectée
        Boolean personDetected = (Boolean) ctx.get("personDetected");
        if (personDetected == null || !personDetected) {
            return false;
        }

        // Vérifier la confiance de détection
        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        // Vérifier si l'intrusion est dans la zone de la règle
        if (rule.getZone() != null) {
            String detectedZoneId = (String) ctx.get("zoneId");
            if (detectedZoneId != null && !detectedZoneId.equals(rule.getZone().getId().toString())) {
                return false;
            }
        }

        log.info("Intrusion détectée par la règle: {}", rule.getName());
        return true;
    }

    /**
     * Détection de présence prolongée
     * Contexte attendu: Long (temps en secondes) ou Map avec "presenceTime"
     */
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

        // Vérifier si le temps de présence dépasse le seuil
        if (rule.getThresholdTime() != null) {
            boolean exceeded = presenceTime >= rule.getThresholdTime();
            if (exceeded) {
                log.info("Présence prolongée détectée: {} secondes (seuil: {})",
                        presenceTime, rule.getThresholdTime());
            }
            return exceeded;
        }

        return false;
    }

    /**
     * Détection d'objet suspect (colis abandonné, etc.)
     * Contexte attendu: Map avec "objectClass", "confidence", "stationaryTime"
     */
    private boolean evaluateObjectRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Classes d'objets suspects
        String objectClass = (String) ctx.get("objectClass");
        if (objectClass == null) {
            return false;
        }

        // Vérifier si l'objet est dans les classes suspectes
        List<String> suspiciousClasses = List.of("backpack", "suitcase", "bag", "package");
        boolean isSuspicious = suspiciousClasses.stream()
                .anyMatch(sc -> objectClass.toLowerCase().contains(sc.toLowerCase()));

        if (!isSuspicious) {
            return false;
        }

        // Vérifier la confiance
        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        // Vérifier si l'objet est stationnaire depuis un certain temps
        Long stationaryTime = getLongValue(ctx.get("stationaryTime"));
        if (rule.getThresholdTime() != null && stationaryTime != null) {
            if (stationaryTime < rule.getThresholdTime()) {
                return false;
            }
        }

        log.info("Objet suspect détecté: {} par la règle: {}", objectClass, rule.getName());
        return true;
    }

    /**
     * Détection de skimmer sur les DAB
     * Contexte attendu: Map avec "objectClass", "location", "confidence"
     */
    private boolean evaluateSkimmerRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Vérifier la classe de l'objet détecté
        String objectClass = (String) ctx.get("objectClass");
        if (objectClass == null) {
            return false;
        }

        // Classes suspectes pour un skimmer
        boolean isSkimmer = objectClass.toLowerCase().contains("skimmer") ||
                objectClass.toLowerCase().contains("device") ||
                objectClass.toLowerCase().contains("card_reader");

        if (!isSkimmer) {
            return false;
        }

        // Vérifier la confiance
        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        log.warn("Skimmer potentiel détecté par la règle: {}", rule.getName());
        return true;
    }

    /**
     * Détection d'arme
     * Contexte attendu: Map avec "objectClass", "confidence"
     */
    private boolean evaluateWeaponRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Classes d'armes
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

        // Vérifier la confiance
        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        log.error("Arme détectée: {} par la règle: {}", objectClass, rule.getName());
        return true;
    }

    /**
     * Détection de chute de personne
     * Contexte attendu: Map avec "fallDetected", "confidence", "duration"
     */
    private boolean evaluateFallRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Vérifier si une chute est détectée
        Boolean fallDetected = (Boolean) ctx.get("fallDetected");
        if (fallDetected == null || !fallDetected) {
            return false;
        }

        // Vérifier la confiance
        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        // Vérifier si la personne reste au sol depuis un certain temps
        Long duration = getLongValue(ctx.get("duration"));
        if (rule.getThresholdTime() != null && duration != null) {
            if (duration < rule.getThresholdTime()) {
                return false;
            }
        }

        log.warn("Chute détectée par la règle: {}", rule.getName());
        return true;
    }

    /**
     * Détection de rôdeur (mouvements suspects)
     * Contexte attendu: Map avec "trajectory", "loiteringTime", "movementPattern"
     */
    private boolean evaluateLoiteringRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Vérifier le temps de rôdeur
        Long loiteringTime = getLongValue(ctx.get("loiteringTime"));
        if (rule.getThresholdTime() != null && loiteringTime != null) {
            if (loiteringTime < rule.getThresholdTime()) {
                return false;
            }
        }

        // Vérifier le pattern de mouvement
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

        // Vérifier les allers-retours
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

    /**
     * Détection de masque
     * Contexte attendu: Map avec "maskDetected", "confidence"
     */
    private boolean evaluateMaskRule(Rule rule, Object context) {
        if (!(context instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) context;

        // Vérifier si un masque est détecté
        Boolean maskDetected = (Boolean) ctx.get("maskDetected");
        if (maskDetected == null || !maskDetected) {
            return false;
        }

        // Vérifier la confiance
        Double confidence = getDoubleValue(ctx.get("confidence"));
        double threshold = getSensitivityThreshold(rule.getSensitivity());
        if (confidence != null && confidence < threshold) {
            return false;
        }

        log.warn("Personne masquée détectée par la règle: {}", rule.getName());
        return true;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Calcule le seuil de confiance basé sur la sensibilité
     * Sensibilité élevée = seuil plus bas = détection plus sensible
     */
    private double getSensitivityThreshold(Integer sensitivity) {
        if (sensitivity == null) {
            return 0.5; // Seuil par défaut
        }

        // Convertir la sensibilité (0-100) en seuil de confiance (0.0-1.0)
        // Sensibilité 100 -> seuil 0.1 (très sensible)
        // Sensibilité 0 -> seuil 0.9 (peu sensible)
        return 1.0 - (sensitivity / 100.0) * 0.8;
    }

    /**
     * Convertit un objet en Double de manière sécurisée
     */
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

    /**
     * Convertit un objet en Long de manière sécurisée
     */
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

    /**
     * Convertit un objet en Integer de manière sécurisée
     */
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

    /**
     * Convertit une entité Rule en RuleResponse
     */
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