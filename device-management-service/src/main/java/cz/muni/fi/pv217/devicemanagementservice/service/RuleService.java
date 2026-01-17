package cz.muni.fi.pv217.devicemanagementservice.service;

import cz.muni.fi.pv217.devicemanagementservice.domain.Rule;
import cz.muni.fi.pv217.devicemanagementservice.dto.rule.CreateRuleRequest;
import cz.muni.fi.pv217.devicemanagementservice.dto.rule.UpdateRuleRequest;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.RuleNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.mapper.RuleMapper;
import cz.muni.fi.pv217.devicemanagementservice.repository.RuleRepository;
import jakarta.enterprise.context.ApplicationScoped; // <-- Correct Scope for Services
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RuleService {

    @Inject
    RuleRepository repository;

    @Inject
    RuleMapper mapper;

    @Transactional
    public Rule createRule(CreateRuleRequest request) {
        Rule rule = mapper.mapCreateRequestToRule(request, new Rule());
        repository.persist(rule);

        return rule;
    }

    // --- R: Read ---
    public Rule findRuleById(UUID id) {
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule with id: '" + id + "' not found"));
    }

    public List<Rule> findRuleByDeviceId(UUID deviceId) {
        return repository.findRulesByDeviceId(deviceId);
    }

    public List<Rule> findAllRules() {
        return repository.listAll();
    }


    @Transactional
    public Rule updateRule(UUID id, UpdateRuleRequest request) {
        Rule existingRule = repository.findByIdOptional(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule with id: '" + id + "' not found"));

        return mapper.mapUpdateRequestToRule(request, existingRule);
    }

    @Transactional
    public boolean deleteRule(UUID id) {
        return repository.deleteById(id);
    }

}