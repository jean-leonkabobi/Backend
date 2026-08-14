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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final ZoneRepository zoneRepository;
    private final AuditLogService auditLogService;

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

    // Méthodes d'évaluation spécifiques (à implémenter selon la logique métier)
    private boolean evaluateIntrusionRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection d'intrusion
        return false;
    }

    private boolean evaluatePresenceRule(Rule rule, Object context) {
        // Vérifier si le temps de présence dépasse le seuil
        if (rule.getThresholdTime() != null && context instanceof Long) {
            long presenceTime = (Long) context;
            return presenceTime >= rule.getThresholdTime();
        }
        return false;
    }

    private boolean evaluateObjectRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection d'objet suspect
        return false;
    }

    private boolean evaluateSkimmerRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection de skimmer
        return false;
    }

    private boolean evaluateWeaponRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection d'arme
        return false;
    }

    private boolean evaluateFallRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection de chute
        return false;
    }

    private boolean evaluateLoiteringRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection de rôdeur
        return false;
    }

    private boolean evaluateMaskRule(Rule rule, Object context) {
        // TODO: Implémenter la logique de détection de masque
        return false;
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