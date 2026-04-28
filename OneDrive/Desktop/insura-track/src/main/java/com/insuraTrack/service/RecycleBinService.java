package com.insuraTrack.service;

import com.insuraTrack.dto.RecycleBinItem;
import com.insuraTrack.model.*;
import com.insuraTrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleBinService {

    private final PolicyRepository policyRepository;
    private final CompanyRepository companyRepository;
    private final InsuranceProviderRepository providerRepository;
    private final InsuranceTypeRepository insuranceTypeRepository;
    private final HypothecationRepository hypothecationRepository;
    private final InsuranceItemRepository insuranceItemRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RecycleBinItem> getAllDeleted() {
        List<RecycleBinItem> items = new ArrayList<>();

        // Deleted Policies
        policyRepository.findAllByDeletedTrue().forEach(p ->
                items.add(RecycleBinItem.builder()
                        .id(p.getId())
                        .type("POLICY")
                        .name(p.getPolicyNumber())
                        .policyNumber(p.getPolicyNumber())
                        .assignedTo(p.getUser() != null ? p.getUser().getName() : "")
                        .companyName(p.getCompany() != null ? p.getCompany().getName() : "")
                        .providerName(p.getProvider() != null ? p.getProvider().getName() : "")
                        .insuranceTypeName(p.getInsuranceType() != null ? p.getInsuranceType().getName() : "")
                        .description(p.getDescription())
                        .deletedBy(p.getDeletedBy())
                        .deletedAt(p.getDeletedAt() != null ? p.getDeletedAt().toString() : "")
                        .build())
        );

        // Deleted Companies
        companyRepository.findAllByDeletedTrue().forEach(c ->
                items.add(RecycleBinItem.builder()
                        .id(c.getId())
                        .type("COMPANY")
                        .name(c.getName())
                        .assignedTo("")
                        .companyName(c.getName())
                        .providerName("")
                        .insuranceTypeName("")
                        .description(c.getAddress() + ", " + c.getCity() + ", " + c.getState())
                        .deletedBy(c.getDeletedBy())
                        .deletedAt(c.getDeletedAt() != null ? c.getDeletedAt().toString() : "")
                        .build())
        );

        // Deleted Providers
        providerRepository.findAllByDeletedTrue().forEach(pr ->
                items.add(RecycleBinItem.builder()
                        .id(pr.getId())
                        .type("PROVIDER")
                        .name(pr.getName())
                        .assignedTo("")
                        .companyName("")
                        .providerName(pr.getName())
                        .insuranceTypeName("")
                        .description(pr.getContactInfo())
                        .deletedBy(pr.getDeletedBy())
                        .deletedAt(pr.getDeletedAt() != null ? pr.getDeletedAt().toString() : "")
                        .build())
        );

        // Deleted Insurance Types
        insuranceTypeRepository.findAllByDeletedTrue().forEach(it ->
                items.add(RecycleBinItem.builder()
                        .id(it.getId())
                        .type("INSURANCE_TYPE")
                        .name(it.getName())
                        .assignedTo("")
                        .companyName("")
                        .providerName("")
                        .insuranceTypeName(it.getName())
                        .description(it.getDescription())
                        .deletedBy(it.getDeletedBy())
                        .deletedAt(it.getDeletedAt() != null ? it.getDeletedAt().toString() : "")
                        .build())
        );

        // Deleted Hypothecations
        hypothecationRepository.findAllByDeletedTrue().forEach(h ->
                items.add(RecycleBinItem.builder()
                        .id(h.getId())
                        .type("HYPOTHECATION")
                        .name(h.getBankName())
                        .assignedTo(h.getEmployeeName())
                        .companyName("")
                        .providerName("")
                        .insuranceTypeName("")
                        .description("Mobile: " + h.getMobileNumber() + ", Email: " + h.getEmail())
                        .deletedBy(h.getDeletedBy())
                        .deletedAt(h.getDeletedAt() != null ? h.getDeletedAt().toString() : "")
                        .build())
        );

        // Deleted Insurance Items
        insuranceItemRepository.findAllByDeletedTrue().forEach(ii ->
                items.add(RecycleBinItem.builder()
                        .id(ii.getId())
                        .type("INSURANCE_ITEM")
                        .name(ii.getName())
                        .assignedTo("")
                        .companyName("")
                        .providerName("")
                        .insuranceTypeName(ii.getInsuranceType() != null ? ii.getInsuranceType().getName() : "")
                        .description(ii.getDescription())
                        .deletedBy(ii.getDeletedBy())
                        .deletedAt(ii.getDeletedAt() != null ? ii.getDeletedAt().toString() : "")
                        .build())
        );

        // Deleted Users
        userRepository.findAllByDeletedTrue().forEach(u ->
                items.add(RecycleBinItem.builder()
                        .id(u.getId())
                        .type("USER")
                        .name(u.getName())
                        .assignedTo("")
                        .companyName("")
                        .providerName("")
                        .insuranceTypeName("")
                        .description("Email: " + u.getEmail() + ", Role: " + u.getRole())
                        .deletedBy(u.getDeletedBy())
                        .deletedAt(u.getDeletedAt() != null ? u.getDeletedAt().toString() : "")
                        .build())
        );

        // Sort by deletion date (most recent first)
        items.sort(Comparator.comparing(RecycleBinItem::getDeletedAt).reversed());
        return items;
    }

    @Transactional
    public void restore(String id) {
        log.info("Restoring item with ID: {}", id);

        if (tryRestorePolicy(id)) return;
        if (tryRestoreCompany(id)) return;
        if (tryRestoreProvider(id)) return;
        if (tryRestoreInsuranceType(id)) return;
        if (tryRestoreHypothecation(id)) return;
        if (tryRestoreInsuranceItem(id)) return;
        if (tryRestoreUser(id)) return;

        throw new RuntimeException("Item not found in recycle bin: " + id);
    }

    // ✅ FIXED: Direct field update without calling entity.restore()
    private boolean tryRestorePolicy(String id) {
        return policyRepository.findById(id)
                .filter(p -> p.isDeleted())
                .map(p -> {
                    p.setDeleted(false);
                    p.setDeletedBy(null);
                    p.setDeletedAt(null);
                    p.setUpdatedAt(LocalDateTime.now());
                    policyRepository.save(p);
                    log.info("Restored policy: {}", p.getPolicyNumber());
                    return true;
                }).orElse(false);
    }

    private boolean tryRestoreCompany(String id) {
        return companyRepository.findById(id)
                .filter(c -> c.isDeleted())
                .map(c -> {
                    c.setDeleted(false);
                    c.setDeletedBy(null);
                    c.setDeletedAt(null);
                    c.setActive(true);
                    c.setUpdatedAt(LocalDateTime.now());
                    companyRepository.save(c);
                    log.info("Restored company: {}", c.getName());
                    return true;
                }).orElse(false);
    }

    // ✅ FIXED: Direct field update for Provider
    private boolean tryRestoreProvider(String id) {
        return providerRepository.findById(id)
                .filter(p -> p.isDeleted())
                .map(p -> {
                    p.setDeleted(false);
                    p.setDeletedBy(null);
                    p.setDeletedAt(null);
                    p.setActive(true);
                    p.setUpdatedAt(LocalDateTime.now());
                    providerRepository.save(p);
                    log.info("Restored provider: {}", p.getName());
                    return true;
                }).orElse(false);
    }

    private boolean tryRestoreInsuranceType(String id) {
        return insuranceTypeRepository.findById(id)
                .filter(t -> t.isDeleted())
                .map(t -> {
                    t.setDeleted(false);
                    t.setDeletedBy(null);
                    t.setDeletedAt(null);
                    t.setUpdatedAt(LocalDateTime.now());
                    insuranceTypeRepository.save(t);
                    log.info("Restored insurance type: {}", t.getName());
                    return true;
                }).orElse(false);
    }

    private boolean tryRestoreHypothecation(String id) {
        return hypothecationRepository.findById(id)
                .filter(h -> h.isDeleted())
                .map(h -> {
                    h.setDeleted(false);
                    h.setDeletedBy(null);
                    h.setDeletedAt(null);
                    h.setUpdatedAt(LocalDateTime.now());
                    hypothecationRepository.save(h);
                    log.info("Restored hypothecation: {}", h.getBankName());
                    return true;
                }).orElse(false);
    }

    private boolean tryRestoreInsuranceItem(String id) {
        return insuranceItemRepository.findById(id)
                .filter(i -> i.isDeleted())
                .map(i -> {
                    i.setDeleted(false);
                    i.setDeletedBy(null);
                    i.setDeletedAt(null);
                    i.setUpdatedAt(LocalDateTime.now());
                    insuranceItemRepository.save(i);
                    log.info("Restored insurance item: {}", i.getName());
                    return true;
                }).orElse(false);
    }

    private boolean tryRestoreUser(String id) {
        return userRepository.findById(id)
                .filter(u -> u.isDeleted())
                .map(u -> {
                    u.setDeleted(false);
                    u.setDeletedBy(null);
                    u.setDeletedAt(null);
                    u.setActive(true);
                    u.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(u);
                    log.info("Restored user: {}", u.getName());
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void permanentDelete(String id) {
        if (policyRepository.findById(id).filter(p -> p.isDeleted()).isPresent()) {
            policyRepository.deleteById(id);
            log.info("Permanently deleted policy: {}", id);
            return;
        }
        if (companyRepository.findById(id).filter(c -> c.isDeleted()).isPresent()) {
            companyRepository.deleteById(id);
            log.info("Permanently deleted company: {}", id);
            return;
        }
        if (providerRepository.findById(id).filter(p -> p.isDeleted()).isPresent()) {
            providerRepository.deleteById(id);
            log.info("Permanently deleted provider: {}", id);
            return;
        }
        if (insuranceTypeRepository.findById(id).filter(t -> t.isDeleted()).isPresent()) {
            insuranceTypeRepository.deleteById(id);
            log.info("Permanently deleted insurance type: {}", id);
            return;
        }
        if (hypothecationRepository.findById(id).filter(h -> h.isDeleted()).isPresent()) {
            hypothecationRepository.deleteById(id);
            log.info("Permanently deleted hypothecation: {}", id);
            return;
        }
        if (insuranceItemRepository.findById(id).filter(i -> i.isDeleted()).isPresent()) {
            insuranceItemRepository.deleteById(id);
            log.info("Permanently deleted insurance item: {}", id);
            return;
        }
        if (userRepository.findById(id).filter(u -> u.isDeleted()).isPresent()) {
            userRepository.deleteById(id);
            log.info("Permanently deleted user: {}", id);
            return;
        }
        throw new RuntimeException("Item not found: " + id);
    }

    @Transactional
    public void emptyRecycleBin() {
        log.info("Emptying recycle bin...");

        policyRepository.findAllByDeletedTrue().forEach(p -> {
            policyRepository.deleteById(p.getId());
            log.info("Permanently deleted policy: {}", p.getPolicyNumber());
        });

        companyRepository.findAllByDeletedTrue().forEach(c -> {
            companyRepository.deleteById(c.getId());
            log.info("Permanently deleted company: {}", c.getName());
        });

        providerRepository.findAllByDeletedTrue().forEach(p -> {
            providerRepository.deleteById(p.getId());
            log.info("Permanently deleted provider: {}", p.getName());
        });

        insuranceTypeRepository.findAllByDeletedTrue().forEach(t -> {
            insuranceTypeRepository.deleteById(t.getId());
            log.info("Permanently deleted insurance type: {}", t.getName());
        });

        hypothecationRepository.findAllByDeletedTrue().forEach(h -> {
            hypothecationRepository.deleteById(h.getId());
            log.info("Permanently deleted hypothecation: {}", h.getBankName());
        });

        insuranceItemRepository.findAllByDeletedTrue().forEach(i -> {
            insuranceItemRepository.deleteById(i.getId());
            log.info("Permanently deleted insurance item: {}", i.getName());
        });

        userRepository.findAllByDeletedTrue().forEach(u -> {
            userRepository.deleteById(u.getId());
            log.info("Permanently deleted user: {}", u.getName());
        });

        log.info("Recycle bin emptied successfully");
    }
}