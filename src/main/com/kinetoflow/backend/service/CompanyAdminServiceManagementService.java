package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.*;
import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.Package;
import com.kinetoflow.backend.entity.PackageServiceItem;
import com.kinetoflow.backend.entity.Service;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import com.kinetoflow.backend.repository.PackageRepository;
import com.kinetoflow.backend.repository.ServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service specifically for managing Services and Packages by a Company Admin.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class CompanyAdminServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final PackageRepository packageRepository;
    // Inject EntityManager if using merge strategy later
    // @PersistenceContext
    // private EntityManager entityManager;

    // --- Service Management ---

    @Transactional
    public ServiceDto createService(User companyAdmin, CreateServiceRequest request) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.info("Admin {} creating service '{}' for company {}", companyAdmin.getEmail(), request.name(), company.getId());

        if (serviceRepository.existsByNameAndCompany(request.name(), company)) {
            throw new BadRequestException("A service with the name '" + request.name() + "' already exists in your company.");
        }

        Service service = new Service();
        service.setName(request.name());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        service.setCategory(request.category());
        service.setIsActive(request.isActive()); // Get status from request
        service.setCompany(company);

        Service savedService = serviceRepository.save(service);
        log.info("Service '{}' created with ID {}", savedService.getName(), savedService.getId());
        return ServiceDto.fromEntity(savedService);
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> getServices(User companyAdmin) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.debug("Admin {} fetching services for company {}", companyAdmin.getEmail(), company.getId());
        List<Service> services = serviceRepository.findByCompanyOrderByNameAsc(company);
        return services.stream().map(ServiceDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public ServiceDto updateService(User companyAdmin, Long serviceId, UpdateServiceRequest request) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.info("Admin {} updating service ID {} for company {}", companyAdmin.getEmail(), serviceId, company.getId());

        Service service = findServiceByIdAndCompany(serviceId, company); // Use helper

        // Check if name is being changed and if it conflicts
        if (!service.getName().equals(request.name()) &&
                serviceRepository.existsByNameAndCompanyAndIdNot(request.name(), company, serviceId)) {
            throw new BadRequestException("Another service with the name '" + request.name() + "' already exists in your company.");
        }

        // Update fields
        service.setName(request.name());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price()); // Allows setting null
        service.setCategory(request.category());
        // Handle activation/deactivation carefully via helper
        updateServiceStatusLogic(service, request.isActive()); // Use internal logic helper

        Service updatedService = serviceRepository.save(service);
        log.info("Service ID {} updated successfully.", serviceId);
        return ServiceDto.fromEntity(updatedService);
    }

    @Transactional
    public void updateServiceStatus(User companyAdmin, Long serviceId, boolean isActive) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.info("Admin {} updating status for service ID {} to {}", companyAdmin.getEmail(), serviceId, isActive);
        Service service = findServiceByIdAndCompany(serviceId, company); // Use helper
        updateServiceStatusLogic(service, isActive); // Call helper
        serviceRepository.save(service); // Save the change
        log.info("Service ID {} status updated successfully to {}.", serviceId, isActive);
    }

    // Helper to contain status update logic and checks
    private void updateServiceStatusLogic(Service service, boolean newStatus) {
        if (service.getIsActive() == newStatus) {
            log.debug("Service {} status already {}. No change needed.", service.getId(), newStatus);
            return; // No change needed
        }
        // If deactivating, check if it's used in active packages
        if (!newStatus && serviceRepository.isServiceInActivePackage(service.getId())) {
            log.warn("Attempted to deactivate service ID {} which is used in active packages.", service.getId());
            throw new BadRequestException("Cannot deactivate service. It is currently included in one or more active packages. Deactivate the relevant packages first.");
        }
        service.setIsActive(newStatus);
        log.debug("Service {} status set to {}", service.getId(), newStatus);
    }

    // --- Package Management ---

    @Transactional
    public PackageDto createPackage(User companyAdmin, CreatePackageRequest request) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.info("Admin {} creating package '{}' for company {}", companyAdmin.getEmail(), request.name(), company.getId());

        if (packageRepository.existsByNameAndCompany(request.name(), company)) {
            throw new BadRequestException("A package with this name already exists in your company.");
        }

        Package newPackage = new Package();
        newPackage.setName(request.name());
        newPackage.setDescription(request.description());
        newPackage.setTotalPrice(request.totalPrice());
        newPackage.setIsActive(request.isActive()); // Get status from request
        newPackage.setCompany(company);
        // Important: Save the package *first* to get an ID before adding items if items reference package ID directly
        // However, with CascadeType.ALL and bidirectional mapping, we can set items before saving.

        // Process and add items - validate services belong to company and are active
        Set<PackageServiceItem> items = mapAndValidateServiceItems(request.items(), newPackage, company);
        items.forEach(newPackage::addServiceItem); // Use helper to establish bidirectional link

        Package savedPackage = packageRepository.save(newPackage);
        log.info("Package '{}' created with ID {}", savedPackage.getName(), savedPackage.getId());

        // Fetch again or ensure DTO mapping works with potentially lazy items
        // Eager fetch in repository is generally better for get requests
        return PackageDto.fromEntity(savedPackage); // DTO mapping needs to handle items
    }

    @Transactional(readOnly = true)
    public List<PackageDto> getPackages(User companyAdmin) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.debug("Admin {} fetching packages for company {}", companyAdmin.getEmail(), company.getId());
        // Repository method uses EntityGraph to fetch items+services
        List<Package> packages = packageRepository.findByCompanyOrderByNameAsc(company);
        return packages.stream().map(PackageDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public PackageDto updatePackage(User companyAdmin, Long packageId, UpdatePackageRequest request) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.info("Admin {} updating package ID {} for company {}", companyAdmin.getEmail(), packageId, company.getId());

        // Fetch package WITH items eagerly using the repository method
        Package pkg = packageRepository.findByIdAndCompany(packageId, company)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + packageId + " in your company."));

        // Check name conflict
        if (!pkg.getName().equals(request.name()) &&
                packageRepository.existsByNameAndCompanyAndIdNot(request.name(), company, packageId)) {
            throw new BadRequestException("Another package with the name '" + request.name() + "' already exists in your company.");
        }

        // Update package fields
        pkg.setName(request.name());
        pkg.setDescription(request.description());
        pkg.setTotalPrice(request.totalPrice());
        updatePackageStatusLogic(pkg, request.isActive()); // Use helper for status

        // --- FIX: Sophisticated Item Update ---
        // 1. Validate requested services (belong to company, are active)
        Map<Long, Service> validServicesMap = validateAndGetRequestedServices(request.items(), company);

        // 2. Create a map of requested items for easy lookup {serviceId -> quantity}
        Map<Long, Integer> requestedItemsMap = request.items().stream()
                .collect(Collectors.toMap(
                        ServiceItemRequest::serviceId,
                        ServiceItemRequest::quantity,
                        (qty1, qty2) -> { // Handle duplicate service IDs in request, maybe take first or throw error
                            log.warn("Duplicate service ID {} found in update request for package {}", qty1, packageId);
                            // Decide strategy: throw error or take first/last? Let's throw for now.
                            throw new BadRequestException("Duplicate service ID found in request: " + qty1);
                            // return qty1; // Or take the first one encountered
                        }
                ));

        // 3. Create a map of current items in the package {serviceId -> PackageServiceItem}
        Map<Long, PackageServiceItem> currentItemsMap = pkg.getItems().stream()
                .collect(Collectors.toMap(item -> item.getService().getId(), Function.identity()));

        // 4. Iterate through request items: Update existing or add new
        Set<PackageServiceItem> finalItems = new HashSet<>();
        for (Map.Entry<Long, Integer> requestEntry : requestedItemsMap.entrySet()) {
            Long requestedServiceId = requestEntry.getKey();
            Integer requestedQuantity = requestEntry.getValue();
            Service service = validServicesMap.get(requestedServiceId); // Get validated service

            PackageServiceItem existingItem = currentItemsMap.get(requestedServiceId);

            if (existingItem != null) {
                // --- UPDATE EXISTING ---
                log.debug("Updating item for service {} in package {}", requestedServiceId, packageId);
                existingItem.setQuantity(requestedQuantity);
                finalItems.add(existingItem); // Keep the existing, managed item
                currentItemsMap.remove(requestedServiceId); // Remove from map to track removals later
            } else {
                // --- ADD NEW ---
                log.debug("Adding new item for service {} to package {}", requestedServiceId, packageId);
                PackageServiceItem newItem = new PackageServiceItem();
                newItem.setPack(pkg); // Link to package
                newItem.setService(service); // Link to validated service
                newItem.setQuantity(requestedQuantity);
                // Let Cascade PERSIST handle saving this new item when pkg is saved
                finalItems.add(newItem);
            }
        }

        Set<PackageServiceItem> itemsToRemove = new HashSet<>(currentItemsMap.values());
        itemsToRemove.forEach(pkg::removeServiceItem); // This removes from pkg.items AND sets item.setPack(null)


        Package savedPackage = packageRepository.save(pkg); // Save package and cascade changes to items
        log.info("Package ID {} updated successfully.", packageId);

        // Fetch again to ensure DTO mapping has correct state after save, especially for new item IDs
        Package resultPackage = packageRepository.findByIdAndCompany(savedPackage.getId(), company).orElseThrow();
        return PackageDto.fromEntity(resultPackage);
    }


    // --- Helper to Validate Requested Services ---
    private Map<Long, Service> validateAndGetRequestedServices(List<ServiceItemRequest> itemRequests, Company company) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new BadRequestException("Package must contain at least one service item.");
        }
        Set<Long> requestedServiceIds = itemRequests.stream()
                .map(ServiceItemRequest::serviceId)
                .collect(Collectors.toSet());

        if (requestedServiceIds.size() != itemRequests.size()) {
            throw new BadRequestException("Duplicate service IDs found in the package item list.");
        }

        List<Service> foundServicesList = serviceRepository.findByIdInAndCompany(requestedServiceIds, company);
        Map<Long, Service> foundServicesMap = foundServicesList.stream()
                .collect(Collectors.toMap(Service::getId, Function.identity()));

        // Check if all requested services were found for this company
        if (foundServicesMap.size() != requestedServiceIds.size()) {
            Set<Long> notFoundIds = new HashSet<>(requestedServiceIds);
            notFoundIds.removeAll(foundServicesMap.keySet());
            log.warn("Package item validation failed. Invalid/Wrong Company Service IDs: {}", notFoundIds);
            throw new BadRequestException("One or more specified services were not found in your company: " + notFoundIds);
        }

        // Check if all found services are active
        List<String> inactiveServiceNames = foundServicesList.stream()
                .filter(s -> !s.getIsActive())
                .map(Service::getName)
                .toList();

        if (!inactiveServiceNames.isEmpty()) {
            log.warn("Attempt to use inactive services in package update: {}", inactiveServiceNames);
            throw new BadRequestException("Cannot use inactive services in a package: " + String.join(", ", inactiveServiceNames));
        }

        return foundServicesMap; // Return map of validated, active services
    }

    @Transactional
    public void updatePackageStatus(User companyAdmin, Long packageId, boolean isActive) {
        Company company = getCompanyFromAdmin(companyAdmin);
        log.info("Admin {} updating status for package ID {} to {}", companyAdmin.getEmail(), packageId, isActive);
        Package pkg = findPackageByIdAndCompany(packageId, company); // Use helper
        updatePackageStatusLogic(pkg, isActive); // Call helper
        packageRepository.save(pkg); // Save the change
        log.info("Package ID {} status updated successfully to {}.", packageId, isActive);
    }

    // Helper to encapsulate package status update logic and checks
    private void updatePackageStatusLogic(Package pkg, boolean newStatus) {
        if (pkg.getIsActive() == newStatus) {
            log.debug("Package {} status already {}. No change needed.", pkg.getId(), newStatus);
            return; // No change
        }
        // If activating, check if all included services are active
        if (newStatus) {
            for (PackageServiceItem item : pkg.getItems()) { // Assumes items are loaded (eager or fetched)
                if (!item.getService().getIsActive()) {
                    throw new BadRequestException("Cannot activate package. Included service '" + item.getService().getName() + "' is inactive.");
                }
            }
        }
        // If deactivating, future check: check if assigned to active patient plans
        // if (!newStatus && packageRepository.isAssignedToActivePatientPlans(pkg.getId())) {
        //     throw new BadRequestException("Cannot deactivate package. It is currently assigned to active patient plans.");
        // }
        pkg.setIsActive(newStatus);
        log.debug("Package {} status set to {}", pkg.getId(), newStatus);
    }

    // --- Helper Methods ---

    // Gets the Company entity from the authenticated Company Admin user.
    private Company getCompanyFromAdmin(User companyAdmin) {
        if (companyAdmin == null || companyAdmin.getRole() != UserRole.COMPANY_ADMIN || companyAdmin.getCompany() == null) {
            log.error("Invalid user provided or user is not a Company Admin with an assigned company: {}", companyAdmin != null ? companyAdmin.getEmail() : "null");
            throw new BadRequestException("Action requires a valid Company Administrator profile.");
        }
        // Consider fetching company fresh if LazyInitialization becomes an issue elsewhere
        // return companyRepository.findById(companyAdmin.getCompany().getId()).orElseThrow(...)
        return companyAdmin.getCompany(); // Assuming proxy loaded by security is sufficient *if* accessed within tx
    }

    // Finds a Service by ID ensuring it belongs to the specified company.
    private Service findServiceByIdAndCompany(Long serviceId, Company company) {
        return serviceRepository.findByIdAndCompany(serviceId, company)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId + " in your company."));
    }

    // Finds a Package by ID ensuring it belongs to the specified company.
    private Package findPackageByIdAndCompany(Long packageId, Company company) {
        // Use the repository method that potentially uses an EntityGraph
        return packageRepository.findByIdAndCompany(packageId, company)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + packageId + " in your company."));
    }

    // Helper to map request items to entity items and validate services
    private Set<PackageServiceItem> mapAndValidateServiceItems(List<ServiceItemRequest> itemRequests, Package pkg, Company company) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new BadRequestException("Package must contain at least one service item.");
        }

        Set<Long> requestedServiceIds = itemRequests.stream()
                .map(ServiceItemRequest::serviceId)
                .collect(Collectors.toSet());

        if (requestedServiceIds.size() != itemRequests.size()) {
            throw new BadRequestException("Duplicate service IDs found in the package item list.");
        }

        // Fetch all requested, *active* services belonging to the company in one go
        List<Service> foundServicesList = serviceRepository.findByIdInAndCompany(requestedServiceIds, company);
        Map<Long, Service> foundServicesMap = foundServicesList.stream()
                .filter(Service::getIsActive) // Ensure only active services can be added/updated
                .collect(Collectors.toMap(Service::getId, Function.identity()));

        // Check if all requested *active* services were found for this company
        if (foundServicesMap.size() != requestedServiceIds.size()) {
            Set<Long> notFoundOrInactiveIds = new HashSet<>(requestedServiceIds);
            notFoundOrInactiveIds.removeAll(foundServicesMap.keySet());
            log.warn("Package item validation failed. Invalid/Inactive/Wrong Company Service IDs: {}", notFoundOrInactiveIds);
            throw new BadRequestException("One or more specified services are invalid, inactive, or do not belong to your company. Invalid/Inactive IDs: " + notFoundOrInactiveIds);
        }

        // Create the set of PackageServiceItem entities
        Set<PackageServiceItem> items = new HashSet<>();
        for (ServiceItemRequest itemReq : itemRequests) {
            Service service = foundServicesMap.get(itemReq.serviceId()); // Get the validated service
            // Should not be null due to checks above, but safety first
            if (service != null) {
                PackageServiceItem newItem = new PackageServiceItem();
                newItem.setPack(pkg); // Link to the package being created/updated
                newItem.setService(service);
                newItem.setQuantity(itemReq.quantity());
                items.add(newItem);
            }
        }
        return items;
    }

    // --- NEW: Get *Active* Services for Medic ---
    @Transactional(readOnly = true)
    public List<ServiceDto> getActiveServicesForCompany(Company company) {
        if (company == null) throw new BadRequestException("Company information required.");
        log.debug("Fetching active services for company {}", company.getId());
        List<Service> services = serviceRepository.findByCompanyAndIsActiveOrderByNameAsc(company, true);
        return services.stream().map(ServiceDto::fromEntity).collect(Collectors.toList());
    }

    // --- Package Management (Existing Methods) ---
    // createPackage, getPackages, updatePackage, updatePackageStatus...

    // --- NEW: Get *Active* Packages for Medic ---
    @Transactional(readOnly = true)
    public List<PackageDto> getActivePackagesForCompany(Company company) {
        if (company == null) throw new BadRequestException("Company information required.");
        log.debug("Fetching active packages for company {}", company.getId());
        // Assuming findByCompanyAndIsActiveOrderByNameAsc uses EntityGraph or handles lazy loading
        List<Package> packages = packageRepository.findByCompanyAndIsActiveOrderByNameAsc(company, true); // Need this repo method
        return packages.stream().map(PackageDto::fromEntity).collect(Collectors.toList());
    }
}