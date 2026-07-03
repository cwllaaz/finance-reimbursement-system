package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.AssetAcceptanceRequest;
import com.geekworkshop.finance.dto.AssetClaimRequest;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTests {
    @Mock AssetRepository assetRepository;
    @Mock AssetAcceptanceRepository acceptanceRepository;
    @Mock AssetHistoryRepository historyRepository;
    @Mock PurchaseApplicationRepository purchaseRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock OperationLogService operationLogService;

    private AssetService service;

    @BeforeEach
    void setUp() {
        service = new AssetService(assetRepository, acceptanceRepository, historyRepository,
                purchaseRepository, appUserRepository, operationLogService);
        lenient().when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(acceptanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(historyRepository.findByAssetIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
    }

    @Test
    void onlyOfficeCanAcceptAssets() {
        AppUser employee = user(1L, UserRole.EMPLOYEE);
        AssetAcceptanceRequest request = acceptanceRequest();

        assertThrows(ForbiddenException.class, () -> service.acceptInbound(employee, request));
    }

    @Test
    void fullLedgerRolesCanViewAllAssetsButCashierCannotAccessModule() {
        Asset first = asset(20L, AssetStatus.IN_STOCK);
        Asset second = asset(21L, AssetStatus.IN_USE);
        when(assetRepository.findAllDetails()).thenReturn(List.of(first, second));

        for (UserRole role : List.of(
                UserRole.OFFICE, UserRole.FINANCE, UserRole.EXECUTIVE,
                UserRole.COMMITTEE, UserRole.ADMIN)) {
            assertEquals(2, service.list(user((long) role.ordinal() + 10, role), null, null).size());
        }
        assertThrows(ForbiddenException.class,
                () -> service.list(user(99L, UserRole.CASHIER), null, null));
        verify(assetRepository, times(5)).findAllDetails();
    }

    @Test
    void employeeAndManagerOnlySeeAvailableOrPersonallyHeldAssets() {
        AppUser employee = user(3L, UserRole.EMPLOYEE);
        Asset available = asset(20L, AssetStatus.IN_STOCK);
        Asset own = asset(21L, AssetStatus.IN_USE);
        own.setClaimedBy(employee);
        own.setCustodian(employee);
        Asset anotherUsers = asset(22L, AssetStatus.IN_USE);
        anotherUsers.setClaimedBy(user(4L, UserRole.EMPLOYEE));
        anotherUsers.setCustodian(anotherUsers.getClaimedBy());
        when(assetRepository.findAllDetails()).thenReturn(List.of(available, own, anotherUsers));

        assertEquals(2, service.list(employee, null, null).size());

        AppUser manager = user(5L, UserRole.DEPARTMENT_MANAGER);
        assertEquals(1, service.list(manager, null, null).size());
    }

    @Test
    void employeeCannotOpenAnotherUsersAssetDetail() {
        Asset anotherUsers = asset(22L, AssetStatus.IN_USE);
        anotherUsers.setCustodian(user(4L, UserRole.EMPLOYEE));
        when(assetRepository.findDetailById(22L)).thenReturn(Optional.of(anotherUsers));

        assertThrows(ForbiddenException.class,
                () -> service.detail(user(3L, UserRole.EMPLOYEE), 22L));
        verify(historyRepository, never()).findByAssetIdOrderByCreatedAtAsc(22L);
    }

    @Test
    void personalAssetDetailOnlyContainsRelatedHistory() {
        AppUser employee = user(3L, UserRole.EMPLOYEE);
        AppUser office = user(2L, UserRole.OFFICE);
        Asset own = asset(21L, AssetStatus.IN_USE);
        own.setClaimedBy(employee);
        own.setCustodian(employee);
        AssetHistory inbound = history(own, office, null, AssetHistoryAction.ACCEPTED_INBOUND);
        AssetHistory claimed = history(own, office, employee, AssetHistoryAction.CLAIMED);
        when(assetRepository.findDetailById(21L)).thenReturn(Optional.of(own));
        when(historyRepository.findByAssetIdOrderByCreatedAtAsc(21L)).thenReturn(List.of(inbound, claimed));

        var response = service.detail(employee, 21L);

        assertEquals(1, response.history().size());
        assertEquals(AssetHistoryAction.CLAIMED, response.history().getFirst().action());
    }

    @Test
    void financeExecutiveAndCommitteeCannotAcceptOrAssignAssets() {
        AssetAcceptanceRequest acceptance = acceptanceRequest();
        AssetClaimRequest claim = new AssetClaimRequest();
        claim.setClaimantUserId(3L);
        claim.setUseLocation("Room 301");

        for (UserRole role : List.of(UserRole.FINANCE, UserRole.EXECUTIVE, UserRole.COMMITTEE)) {
            AppUser viewer = user((long) role.ordinal() + 20, role);
            assertThrows(ForbiddenException.class, () -> service.acceptInbound(viewer, acceptance));
            assertThrows(ForbiddenException.class, () -> service.claim(viewer, 20L, claim));
        }
    }

    @Test
    void completedPurchaseCreatesAssetLedger() {
        AppUser office = user(2L, UserRole.OFFICE);
        PurchaseApplication purchase = completedPurchase(10L);
        when(purchaseRepository.findDetailById(10L)).thenReturn(Optional.of(purchase));
        when(acceptanceRepository.existsByPurchaseApplicationId(10L)).thenReturn(false);
        when(acceptanceRepository.findTopByAcceptanceNumberStartingWithOrderByAcceptanceNumberDesc(any()))
                .thenReturn(Optional.empty());
        when(assetRepository.findTopByAssetNumberStartingWithOrderByAssetNumberDesc(any()))
                .thenReturn(Optional.empty());

        var response = service.acceptInbound(office, acceptanceRequest());

        assertTrue(response.acceptanceNumber().startsWith("YS"));
        assertEquals(1, response.assets().size());
        assertTrue(response.assets().getFirst().assetNumber().startsWith("ZC"));
        assertEquals(AssetStatus.IN_STOCK, response.assets().getFirst().status());
        assertEquals(office.getId(), response.acceptedById());
        assertEquals(office.getRealName(), response.acceptedByName());
        verify(historyRepository).save(any(AssetHistory.class));
    }

    @Test
    void purchaseCannotBeAcceptedTwice() {
        AppUser office = user(2L, UserRole.OFFICE);
        PurchaseApplication purchase = completedPurchase(10L);
        when(purchaseRepository.findDetailById(10L)).thenReturn(Optional.of(purchase));
        when(acceptanceRepository.existsByPurchaseApplicationId(10L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.acceptInbound(office, acceptanceRequest()));
    }

    @Test
    void officeSelectsActualUserAndClaimGeneratesReceipt() {
        AppUser office = user(2L, UserRole.OFFICE);
        AppUser employee = user(3L, UserRole.EMPLOYEE);
        Asset asset = asset(20L, AssetStatus.IN_STOCK);
        when(assetRepository.findDetailById(20L)).thenReturn(Optional.of(asset));
        when(appUserRepository.findWithDepartmentById(3L)).thenReturn(Optional.of(employee));
        when(historyRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(any()))
                .thenReturn(Optional.empty());
        AssetClaimRequest request = new AssetClaimRequest();
        request.setClaimantUserId(3L);
        request.setUseLocation("科研楼 301");
        request.setRemark("项目使用");

        var response = service.claim(office, 20L, request);

        assertEquals(AssetStatus.IN_USE, response.status());
        assertEquals(employee.getId(), response.custodianId());
        assertEquals(employee.getId(), response.claimantId());
        assertNotNull(response.claimedAt());
        assertEquals("科研楼 301", response.location());
        verify(historyRepository).save(argThat(history ->
                history.getReceiptNumber().startsWith("LY")
                        && history.getAction() == AssetHistoryAction.CLAIMED
                        && history.getOperator().getId().equals(office.getId())
                        && history.getCustodian().getId().equals(employee.getId())));
    }

    @Test
    void employeeCannotDirectlyAssignAssetClaimant() {
        AssetClaimRequest request = new AssetClaimRequest();
        request.setClaimantUserId(3L);
        request.setUseLocation("科研楼 301");

        assertThrows(ForbiddenException.class,
                () -> service.claim(user(3L, UserRole.EMPLOYEE), 20L, request));
    }

    @Test
    void claimantCannotBeTheAcceptanceOperator() {
        AppUser office = user(2L, UserRole.OFFICE);
        Asset asset = asset(20L, AssetStatus.IN_STOCK);
        when(assetRepository.findDetailById(20L)).thenReturn(Optional.of(asset));
        when(appUserRepository.findWithDepartmentById(2L)).thenReturn(Optional.of(office));
        AssetClaimRequest request = new AssetClaimRequest();
        request.setClaimantUserId(2L);
        request.setUseLocation("办公室");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.claim(office, 20L, request));

        assertTrue(exception.getMessage().contains("不能与验收人相同"));
    }

    @Test
    void assetAlreadyInUseCannotBeClaimedAgain() {
        AppUser office = user(2L, UserRole.OFFICE);
        Asset asset = asset(20L, AssetStatus.IN_USE);
        when(assetRepository.findDetailById(20L)).thenReturn(Optional.of(asset));
        AssetClaimRequest request = new AssetClaimRequest();
        request.setClaimantUserId(3L);
        request.setUseLocation("科研楼 301");

        assertThrows(BusinessException.class, () -> service.claim(office, 20L, request));
    }

    private AssetAcceptanceRequest acceptanceRequest() {
        AssetAcceptanceRequest request = new AssetAcceptanceRequest();
        request.setPurchaseApplicationId(10L);
        request.setReceivedAt(LocalDateTime.of(2026, 6, 30, 10, 0));
        request.setStorageLocation("资产库 A 区");
        return request;
    }

    private PurchaseApplication completedPurchase(Long id) {
        PurchaseApplication purchase = new PurchaseApplication();
        ReflectionTestUtils.setField(purchase, "id", id);
        purchase.setApplicationNumber("CG20260630001");
        purchase.setApplicant(user(8L, UserRole.EMPLOYEE));
        purchase.setDepartment(purchase.getApplicant().getDepartment());
        purchase.setPurchaseMethod("询价采购");
        purchase.setPurchaseReason("设备购置");
        purchase.setAmount(new BigDecimal("3000"));
        purchase.setStatus(PurchaseStatus.COMPLETED);
        PurchaseItem item = new PurchaseItem();
        ReflectionTestUtils.setField(item, "id", 11L);
        item.setPurchaseApplication(purchase);
        item.setItemName("显示器");
        item.setSpecification("27 英寸");
        item.setManufacturer("示例厂商");
        item.setUnitPrice(new BigDecimal("1500"));
        item.setQuantity(2);
        item.setTotalPrice(new BigDecimal("3000"));
        purchase.getItems().add(item);
        return purchase;
    }

    private Asset asset(Long id, AssetStatus status) {
        PurchaseApplication purchase = completedPurchase(10L);
        AssetAcceptance acceptance = new AssetAcceptance();
        acceptance.setAcceptanceNumber("YS20260630001");
        acceptance.setPurchaseApplication(purchase);
        acceptance.setAcceptedBy(user(2L, UserRole.OFFICE));
        acceptance.setReceivedAt(LocalDateTime.now());
        acceptance.setStorageLocation("资产库");
        Asset asset = new Asset();
        ReflectionTestUtils.setField(asset, "id", id);
        asset.setAssetNumber("ZC20260630001");
        asset.setAcceptance(acceptance);
        asset.setPurchaseItem(purchase.getItems().getFirst());
        asset.setItemName("显示器");
        asset.setQuantity(2);
        asset.setTotalPrice(new BigDecimal("3000"));
        asset.setReceivedAt(LocalDateTime.now());
        asset.setLocation("资产库");
        asset.setStatus(status);
        return asset;
    }

    private AssetHistory history(
            Asset asset, AppUser operator, AppUser custodian, AssetHistoryAction action
    ) {
        AssetHistory history = new AssetHistory();
        history.setAsset(asset);
        history.setOperator(operator);
        history.setCustodian(custodian);
        history.setAction(action);
        history.setLocation(asset.getLocation());
        history.setAssetStatus(asset.getStatus());
        return history;
    }

    private AppUser user(Long id, UserRole role) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", 1L);
        department.setName("科研管理部");
        department.setCode("RD");
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(role.name().toLowerCase());
        user.setRealName(role.name());
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }
}
