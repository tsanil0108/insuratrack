package com.insuraTrack.service;

import com.insuraTrack.dto.RecycleBinItem;
import com.insuraTrack.model.*;
import com.insuraTrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecycleBinService {

    private final PolicyRepository            policyRepository;
    private final CompanyRepository           companyRepository;
    private final InsuranceProviderRepository providerRepository;
    private final PremiumPaymentRepository    paymentRepository;
    private final InsuranceTypeRepository     insuranceTypeRepository; // ← NEW

    @Transactional(readOnly = true)
    public List<RecycleBinItem> getAllDeleted() {
        List<RecycleBinItem> items = new ArrayList<>();

        policyRepository.findAllDeleted().forEach(p ->
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

        companyRepository.findAllDeleted().forEach(c ->
                items.add(RecycleBinItem.builder()
                        .id(c.getId())
                        .type("COMPANY")
                        .name(c.getName())
                        .assignedTo("")
                        .companyName(c.getName())
                        .providerName("")
                        .insuranceTypeName("")
                        .description(c.getAddress())
                        .deletedBy(c.getDeletedBy())
                        .deletedAt(c.getDeletedAt() != null ? c.getDeletedAt().toString() : "")
                        .build())
        );

        providerRepository.findAllDeleted().forEach(pr ->
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

        insuranceTypeRepository.findAllDeleted().forEach(it ->
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

        paymentRepository.findAllDeleted().forEach(pay ->
                items.add(RecycleBinItem.builder()
                        .id(pay.getId())
                        .type("PAYMENT")
                        .name("₹" + String.format("%.0f", pay.getAmount()))
                        .policyNumber(pay.getPolicy() != null ? pay.getPolicy().getPolicyNumber() : "")
                        .assignedTo(pay.getPolicy() != null && pay.getPolicy().getUser() != null
                                ? pay.getPolicy().getUser().getName() : "")
                        .companyName(pay.getPolicy() != null && pay.getPolicy().getCompany() != null
                                ? pay.getPolicy().getCompany().getName() : "")
                        .providerName(pay.getPolicy() != null && pay.getPolicy().getProvider() != null
                                ? pay.getPolicy().getProvider().getName() : "")
                        .insuranceTypeName(pay.getPolicy() != null && pay.getPolicy().getInsuranceType() != null
                                ? pay.getPolicy().getInsuranceType().getName() : "")
                        .description("Due: " + pay.getDueDate())
                        .deletedBy(pay.getDeletedBy())
                        .deletedAt(pay.getDeletedAt() != null ? pay.getDeletedAt().toString() : "")
                        .build())
        );

        items.sort(Comparator.comparing(RecycleBinItem::getDeletedAt).reversed());
        return items;
    }

    @Transactional
    public void restore(String id) {
        if (tryRestorePolicy(id))       return;
        if (tryRestoreCompany(id))      return;
        if (tryRestoreProvider(id))     return;
        if (tryRestorePayment(id))      return;
        if (tryRestoreInsuranceType(id)) return;
        throw new RuntimeException("Item not found in recycle bin: " + id);
    }

    private boolean tryRestorePolicy(String id) {
        return policyRepository.findDeletedById(id).map(p -> {
            p.restore(); policyRepository.save(p); return true;
        }).orElse(false);
    }

    private boolean tryRestoreCompany(String id) {
        return companyRepository.findDeletedById(id).map(c -> {
            c.restore(); companyRepository.save(c); return true;
        }).orElse(false);
    }

    private boolean tryRestoreProvider(String id) {
        return providerRepository.findDeletedById(id).map(p -> {
            p.restore(); providerRepository.save(p); return true;
        }).orElse(false);
    }

    private boolean tryRestorePayment(String id) {
        return paymentRepository.findDeletedById(id).map(p -> {
            p.restore(); paymentRepository.save(p); return true;
        }).orElse(false);
    }

    private boolean tryRestoreInsuranceType(String id) {
        return insuranceTypeRepository.findDeletedById(id).map(t -> {
            t.restore(); insuranceTypeRepository.save(t); return true;
        }).orElse(false);
    }

    @Transactional
    public void permanentDelete(String id) {
        if (policyRepository.findDeletedById(id).isPresent()) {
            policyRepository.deleteById(id); return;
        }
        if (companyRepository.findDeletedById(id).isPresent()) {
            companyRepository.deleteById(id); return;
        }
        if (providerRepository.findDeletedById(id).isPresent()) {
            providerRepository.deleteById(id); return;
        }
        if (paymentRepository.findDeletedById(id).isPresent()) {
            paymentRepository.deleteById(id); return;
        }
        if (insuranceTypeRepository.findDeletedById(id).isPresent()) {
            insuranceTypeRepository.deleteById(id); return;
        }
        throw new RuntimeException("Item not found: " + id);
    }

    @Transactional
    public void emptyRecycleBin() {
        policyRepository.findAllDeleted().forEach(p       -> policyRepository.deleteById(p.getId()));
        companyRepository.findAllDeleted().forEach(c      -> companyRepository.deleteById(c.getId()));
        providerRepository.findAllDeleted().forEach(p     -> providerRepository.deleteById(p.getId()));
        paymentRepository.findAllDeleted().forEach(p      -> paymentRepository.deleteById(p.getId()));
        insuranceTypeRepository.findAllDeleted().forEach(t -> insuranceTypeRepository.deleteById(t.getId()));
    }
}