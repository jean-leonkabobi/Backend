package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.request.RuleRequest;
import com.banksecurity.backend.dto.response.RuleResponse;
import com.banksecurity.backend.model.enums.RuleType;

import java.util.List;
import java.util.UUID;

public interface RuleService {

    /**
     * Crée une nouvelle règle
     */
    RuleResponse createRule(RuleRequest request);

    /**
     * Met à jour une règle existante
     */
    RuleResponse updateRule(UUID id, RuleRequest request);

    /**
     * Supprime une règle
     */
    void deleteRule(UUID id);

    /**
     * Récupère une règle par son ID
     */
    RuleResponse getRuleById(UUID id);

    /**
     * Récupère toutes les règles
     */
    List<RuleResponse> getAllRules();

    /**
     * Récupère les règles par zone
     */
    List<RuleResponse> getRulesByZone(UUID zoneId);

    /**
     * Récupère les règles par type
     */
    List<RuleResponse> getRulesByType(RuleType type);

    /**
     * Active ou désactive une règle
     */
    RuleResponse toggleRuleStatus(UUID id, boolean isActive);

    /**
     * Vérifie si une règle est déclenchée
     */
    boolean evaluateRule(UUID ruleId, Object context);

    /**
     * Récupère les règles actives
     */
    List<RuleResponse> getActiveRules();

    /**
     * Récupère le nombre total de règles
     */
    long countRules();
}