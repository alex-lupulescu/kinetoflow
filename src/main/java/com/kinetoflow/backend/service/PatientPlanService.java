package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.AssignPlanRequestDto;
import com.kinetoflow.backend.dto.PatientPlanDto;
import com.kinetoflow.backend.dto.PatientPlanServiceItemDto;
import com.kinetoflow.backend.dto.ServiceItemRequest;
import com.kinetoflow.backend.dto.UpdatePlanItemQuantitiesRequest;
import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.PackageServiceItem;
import com.kinetoflow.backend.entity.PatientPlan;
import com.kinetoflow.backend.entity.PatientPlanServiceItem;
import com.kinetoflow.backend.entity.Service;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.repository.PackageRepository;
import com.kinetoflow.backend.repository.PatientPlanRepository;
import com.kinetoflow.backend.repository.PatientPlanServiceItemRepository;
import com.kinetoflow.backend.repository.ServiceRepository;
import com.kinetoflow.backend.repository.UserRepository;
import com.kinetoflow.backend.entity.Package;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ForbiddenException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class PatientPlanService {

    private final PatientPlanRepository patientPlanRepository;
    private final PatientPlanServiceItemRepository patientPlanServiceItemRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final PackageRepository packageRepository;

    // --- Helper Methods ---
    private Map<Long, Service> validateAndGetRequestServices(List<ServiceItemRequest> itemRequests, Company company) {
        if (itemRequests == null || itemRequests.isEmpty()) { throw new BadRequestException("Plan needs at least one item."); }
        Set<Long> ids = itemRequests.stream().map(ServiceItemRequest::serviceId).collect(Collectors.toSet());
        if (ids.size() != itemRequests.size()) { throw new BadRequestException("Duplicate services in request."); }
        List<Service> found = serviceRepository.findByIdInAndCompany(ids, company);
        Map<Long, Service> map = found.stream().collect(Collectors.toMap(Service::getId, Function.identity()));
        if (map.size() != ids.size()) { Set<Long> notFound = new HashSet<>(ids); notFound.removeAll(map.keySet()); throw new BadRequestException("Services not found/invalid: " + notFound); }
        List<String> inactive = found.stream().filter(s -> !s.getIsActive()).map(Service::getName).toList();
        if (!inactive.isEmpty()) { throw new BadRequestException("Cannot add inactive services: " + String.join(", ", inactive)); }
        return map;
    }

    private void validatePlanAccess(User requester, User patient) {
        boolean isOwner = requester.getId().equals(patient.getId());
        boolean isAdmin = requester.getRole() == UserRole.COMPANY_ADMIN && requester.getCompany() != null && patient.getCompany() != null && requester.getCompany().getId().equals(patient.getCompany().getId());
        boolean isAssignedMedic = requester.getRole() == UserRole.MEDIC && patient.getAssignedMedic() != null && requester.getId().equals(patient.getAssignedMedic().getId());
        if (!isOwner && !isAdmin && !isAssignedMedic) { log.warn("Auth fail: User {} accessing plan for patient {}", requester.getEmail(), patient.getEmail()); throw new ForbiddenException("No permission for this patient's plan."); }
        log.debug("Auth OK for user {} accessing plan for patient {}", requester.getEmail(), patient.getEmail());
    }

    private PatientPlan findPlanAndAuthorizeMedic(User medic, Long planId) {
        PatientPlan plan = patientPlanRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        User patient = plan.getPatient();
        if (patient == null || patient.getAssignedMedic() == null || !patient.getAssignedMedic().getId().equals(medic.getId())) { log.warn("Auth fail: Medic {} modifying plan {} for patient {} (not assigned).", medic.getEmail(), planId, patient != null ? patient.getEmail() : "null"); throw new ForbiddenException("Can only modify plans for assigned patients."); }
        return plan;
    }

    private PatientPlanServiceItem findPlanItemAndAuthorizeMedic(User medic, Long planItemId) {
        PatientPlanServiceItem item = patientPlanServiceItemRepository.findDetailsById(planItemId).orElseThrow(() -> new ResourceNotFoundException("Plan item not found: " + planItemId));
        PatientPlan plan = item.getPatientPlan(); if (plan == null) { throw new ResourceNotFoundException("Plan not found for item: " + planItemId); }
        User patient = plan.getPatient();
        if (patient == null || patient.getAssignedMedic() == null || !patient.getAssignedMedic().getId().equals(medic.getId())) { log.warn("Auth fail: Medic {} modifying item {} for patient {} (not assigned).", medic.getEmail(), planItemId, patient != null ? patient.getEmail() : "null"); throw new ForbiddenException("Can only modify plan items for assigned patients."); }
        return item;
    }

    @Transactional
    public PatientPlanDto assignPlanToPatient(User assigner, Long patientId, AssignPlanRequestDto request) {
        log.info("User {} assigning plan to patient {}", assigner.getEmail(), patientId);
        if ((request.packageId() == null && (request.serviceItems() == null || request.serviceItems().isEmpty())) || (request.packageId() != null && request.serviceItems() != null && !request.serviceItems().isEmpty())) { throw new BadRequestException("Provide either 'packageId' OR 'serviceItems'."); }
        Company company = assigner.getCompany(); if (company == null) { throw new BadRequestException("Assigner must belong to a company."); }
        User patient = userRepository.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
        if (patient.getRole() != UserRole.USER) { throw new BadRequestException("User is not a patient: " + patientId); }
        if (patient.getCompany() == null || !patient.getCompany().getId().equals(company.getId())) { throw new BadRequestException("Patient not in assigner's company."); }
        if (!patient.isActive()) { throw new BadRequestException("Cannot assign plan to inactive patient."); }

        PatientPlan plan = PatientPlan.builder()
                .patient(patient).assignedBy(assigner).company(company)
                .notes(request.notes())
                .assignedDate(request.assignedDate() != null ? request.assignedDate() : LocalDateTime.now())
                .expiryDate(request.expiryDate()).isActive(true).isArchived(false) // Set archive default
                .build();

        Set<PatientPlanServiceItem> serviceItems = new HashSet<>();
        if (request.packageId() != null) {
            Package originatingPackage = packageRepository.findByIdAndCompany(request.packageId(), company).orElseThrow(() -> new ResourceNotFoundException("Package not found: " + request.packageId()));
            if (!originatingPackage.getIsActive()) { throw new BadRequestException("Cannot assign inactive package."); }
            plan.setOriginatingPackage(originatingPackage);
            if (originatingPackage.getItems() == null || originatingPackage.getItems().isEmpty()) { throw new BadRequestException("Package template has no items."); }
            for (PackageServiceItem templateItem : originatingPackage.getItems()) {
                if (templateItem.getService() == null) { log.error("Data integrity issue: Package {} item has null service.", originatingPackage.getId()); continue; }
                if (!templateItem.getService().getIsActive()) { throw new BadRequestException("Package '" + originatingPackage.getName() + "' contains inactive service: " + templateItem.getService().getName()); }
                serviceItems.add(PatientPlanServiceItem.builder().patientPlan(plan).service(templateItem.getService()).totalQuantity(templateItem.getQuantity()).remainingQuantity(templateItem.getQuantity()).pricePerUnit(templateItem.getService().getPrice()).isItemActive(true).isArchived(false).build()); // Set archive default
            }
        } else {
            Map<Long, Service> validServicesMap = validateAndGetRequestServices(request.serviceItems(), company);
            for (ServiceItemRequest itemReq : request.serviceItems()) {
                Service service = validServicesMap.get(itemReq.serviceId());
                serviceItems.add(PatientPlanServiceItem.builder().patientPlan(plan).service(service).totalQuantity(itemReq.quantity()).remainingQuantity(itemReq.quantity()).pricePerUnit(service.getPrice()).isItemActive(true).isArchived(false).build()); // Set archive default
            }
        }
        if (serviceItems.isEmpty()) { throw new BadRequestException("Plan must contain at least one valid service item."); }
        plan.setServiceItems(new HashSet<>());
        serviceItems.forEach(plan::addServiceItem);

        PatientPlan savedPlan = patientPlanRepository.save(plan);
        log.info("Patient plan {} created for patient {}", savedPlan.getId(), patientId);
        PatientPlan resultPlan = patientPlanRepository.findById(savedPlan.getId()).orElseThrow(() -> new IllegalStateException("Saved plan not found: " + savedPlan.getId()));
        return PatientPlanDto.fromEntity(resultPlan);
    }

    @Transactional(readOnly = true)
    public List<PatientPlanDto> getPlansForPatient(User requester, Long patientId) {
        log.debug("User {} fetching non-archived plans for patient {}", requester.getEmail(), patientId);
        User patient = userRepository.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
        validatePlanAccess(requester, patient);
        // Use the repo method filtering archived = false
        List<PatientPlan> plans = patientPlanRepository.findByPatientAndIsArchivedFalseOrderByIdDesc(patient);
        return plans.stream().map(PatientPlanDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PatientPlanDto> getPatientPlanById(User patient, Long planId) {
        log.debug("Fetching plan ID {} for patient {}", planId, patient.getEmail());
        if (patient.getRole() != UserRole.USER) { throw new BadRequestException("User is not a patient."); }
        // This fetches even if archived, but DTO now includes the flag
        return patientPlanRepository.findByIdAndPatient(planId, patient).map(PatientPlanDto::fromEntity);
    }

    @Transactional
    public void updatePatientPlanStatusByMedic(User medic, Long planId, boolean isActive) {
        log.info("Medic {} updating ACTIVE status for plan ID {} to {}", medic.getEmail(), planId, isActive);
        PatientPlan plan = findPlanAndAuthorizeMedic(medic, planId);
        if (Boolean.TRUE.equals(plan.getIsArchived())) { throw new BadRequestException("Cannot update status of an archived plan."); } // Check archive status
        log.info("Current plan.isActive state BEFORE change: {}", plan.getIsActive());
        if (plan.getIsActive() == isActive) { log.warn("Plan {} active status unchanged.", planId); return; }

        if (isActive) { // Reactivating Plan
            log.info("Reactivating plan {}. Validating/reactivating items...", planId);
            boolean itemsChanged = false;
            for (PatientPlanServiceItem item : plan.getServiceItems()) {
                if (Boolean.TRUE.equals(item.getIsArchived())) continue; // Skip archived items
                if (item.getService() == null) { throw new BadRequestException("Item missing service definition."); }
                log.debug("Checking item ID {}, service ID {}, item active: {}, base service active: {}", item.getId(), item.getService().getId(), item.getIsItemActive(), item.getService().getIsActive());
                if (!item.getService().getIsActive()) {
                    if (item.getIsItemActive()) { log.warn("Service '{}' inactive. Deactivating item {}.", item.getService().getName(), item.getId()); item.setIsItemActive(false); itemsChanged = true; }
                } else {
                    if (!item.getIsItemActive()) { log.info("Reactivating item {} as base service is active.", item.getId()); item.setIsItemActive(true); itemsChanged = true; }
                }
            }
            if (itemsChanged) log.info("Item statuses adjusted during plan reactivation for plan {}.", planId);
            plan.setIsActive(true); log.info("Plan ID {} reactivated.", planId);
        } else { // Deactivating Plan
            log.info("Deactivating plan {} and its currently active items.", planId);
            plan.setIsActive(false);
            boolean itemsChanged = false;
            for (PatientPlanServiceItem item : plan.getServiceItems()) { if (item.getIsItemActive()) { item.setIsItemActive(false); log.debug("Deactivating item {} for plan {}", item.getId(), planId); itemsChanged = true; } }
            if(itemsChanged) log.info("Active items within Plan ID {} also deactivated.", planId); else log.info("Plan ID {} deactivated.", planId);
        }
        patientPlanRepository.save(plan);
    }

    @Transactional
    public PatientPlanServiceItemDto updatePatientPlanItemQuantitiesByMedic(User medic, Long planItemId, UpdatePlanItemQuantitiesRequest request) {
        log.info("Medic {} updating quantities for item ID {}", medic.getEmail(), planItemId);
        if(request.remainingQuantity() > request.totalQuantity()){ throw new BadRequestException("Remaining quantity cannot exceed total."); }
        if(request.totalQuantity() < 1){ throw new BadRequestException("Total quantity must be >= 1."); }
        if(request.remainingQuantity() < 0){ throw new BadRequestException("Remaining quantity cannot be negative."); }
        PatientPlanServiceItem item = findPlanItemAndAuthorizeMedic(medic, planItemId);
        if (Boolean.TRUE.equals(item.getIsArchived()) || Boolean.TRUE.equals(item.getPatientPlan().getIsArchived())) { throw new BadRequestException("Cannot modify quantities for archived plan/item."); }
        if (!item.getIsItemActive() || !item.getPatientPlan().getIsActive()) { throw new BadRequestException("Cannot modify quantities for inactive plan/item."); }
        log.warn("Manual quantity update item ID {} by {}. Old T/R: {}/{}. New T/R: {}/{}", planItemId, medic.getEmail(), item.getTotalQuantity(), item.getRemainingQuantity(), request.totalQuantity(), request.remainingQuantity());
        item.setTotalQuantity(request.totalQuantity());
        item.setRemainingQuantity(request.remainingQuantity());
        PatientPlanServiceItem updatedItem = patientPlanServiceItemRepository.save(item);
        log.info("Item ID {} quantities updated.", planItemId);
        return PatientPlanServiceItemDto.fromEntity(updatedItem);
    }

    @Transactional
    public void updatePatientPlanItemStatusByMedic(User medic, Long planItemId, boolean isItemActive) {
        log.info("Medic {} updating ACTIVE status for item ID {} to {}", medic.getEmail(), planItemId, isItemActive);
        PatientPlanServiceItem item = findPlanItemAndAuthorizeMedic(medic, planItemId);
        if (Boolean.TRUE.equals(item.getIsArchived()) || Boolean.TRUE.equals(item.getPatientPlan().getIsArchived())) { throw new BadRequestException("Cannot update status of an archived plan or item."); }
        if (item.getIsItemActive() == isItemActive) { log.warn("Item {} active status unchanged.", planItemId); return; }
        if (isItemActive) {
            if (!item.getPatientPlan().getIsActive()) { throw new BadRequestException("Parent plan inactive."); }
            if (item.getService() == null) { throw new BadRequestException("Service definition missing."); }
            Service service = serviceRepository.findById(item.getService().getId()).orElseThrow(() -> new ResourceNotFoundException("Associated service not found."));
            if (!service.getIsActive()) { throw new BadRequestException("Cannot activate item: Service '" + service.getName() + "' inactive."); }
            item.setIsItemActive(true); log.info("Item ID {} activated.", planItemId);
        } else {
            item.setIsItemActive(false); log.info("Item ID {} deactivated.", planItemId);
        }
        patientPlanServiceItemRepository.save(item);
    }

    // --- Archive Methods ---
    @Transactional
    public void archivePatientPlanByMedic(User medic, Long planId) {
        log.info("Medic {} ARCHIVING plan ID {}", medic.getEmail(), planId);
        PatientPlan plan = findPlanAndAuthorizeMedic(medic, planId);
        if (Boolean.TRUE.equals(plan.getIsArchived())) { log.warn("Plan {} is already archived.", planId); return; }
        plan.setIsArchived(true);
        plan.setIsActive(false); // Also deactivate
        if (plan.getServiceItems() != null) {
            for (PatientPlanServiceItem item : plan.getServiceItems()) {
                item.setIsArchived(true); item.setIsItemActive(false);
                log.debug("Archiving item {} during plan archive.", item.getId());
            }
        }
        patientPlanRepository.save(plan);
        log.info("Plan ID {} and its items archived successfully.", planId);
    }

    @Transactional
    public void archivePatientPlanItemByMedic(User medic, Long planItemId) {
        log.info("Medic {} ARCHIVING item ID {}", medic.getEmail(), planItemId);
        PatientPlanServiceItem item = findPlanItemAndAuthorizeMedic(medic, planItemId);
        if (Boolean.TRUE.equals(item.getIsArchived())) { log.warn("Plan item {} is already archived.", planItemId); return; }
        if (Boolean.TRUE.equals(item.getPatientPlan().getIsArchived())) { log.warn("Cannot archive item {} because parent plan is already archived.", planItemId); item.setIsArchived(true); item.setIsItemActive(false); patientPlanServiceItemRepository.save(item); return; } // Ensure consistency if reached
        item.setIsArchived(true);
        item.setIsItemActive(false); // Also deactivate
        patientPlanServiceItemRepository.save(item);
        log.info("Plan item ID {} archived successfully.", planItemId);
    }

}