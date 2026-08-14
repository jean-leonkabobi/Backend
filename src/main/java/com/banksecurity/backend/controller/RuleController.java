package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.request.RuleRequest;
import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.dto.response.RuleResponse;
import com.banksecurity.backend.model.enums.RuleType;
import com.banksecurity.backend.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
@Tag(name = "Règles", description = "Gestion des règles de détection")
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    @Operation(summary = "Récupérer toutes les règles")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getAllRules() {
        List<RuleResponse> rules = ruleService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success("Règles récupérées", rules));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une règle par ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<RuleResponse>> getRuleById(@PathVariable UUID id) {
        RuleResponse rule = ruleService.getRuleById(id);
        return ResponseEntity.ok(ApiResponse.success("Règle récupérée", rule));
    }

    @GetMapping("/zone/{zoneId}")
    @Operation(summary = "Récupérer les règles d'une zone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getRulesByZone(@PathVariable UUID zoneId) {
        List<RuleResponse> rules = ruleService.getRulesByZone(zoneId);
        return ResponseEntity.ok(ApiResponse.success("Règles récupérées", rules));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Récupérer les règles par type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getRulesByType(@PathVariable RuleType type) {
        List<RuleResponse> rules = ruleService.getRulesByType(type);
        return ResponseEntity.ok(ApiResponse.success("Règles récupérées", rules));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle règle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RuleResponse>> createRule(@Valid @RequestBody RuleRequest request) {
        RuleResponse rule = ruleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Règle créée", rule));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une règle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(@PathVariable UUID id,
                                                                @Valid @RequestBody RuleRequest request) {
        RuleResponse rule = ruleService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.success("Règle mise à jour", rule));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une règle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Règle supprimée", null));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Activer/désactiver une règle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RuleResponse>> toggleRule(@PathVariable UUID id,
                                                                @RequestParam boolean isActive) {
        RuleResponse rule = ruleService.toggleRuleStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Statut de la règle mis à jour", rule));
    }
}