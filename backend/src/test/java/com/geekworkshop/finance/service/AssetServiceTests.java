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
    @Mock OperationLogService operationLogService;

    private AssetService service;

    @BeforeEach
    void setUp() {
        service = new AssetService(assetRepository, acceptanceRepository, historyRepository,
                purchaseRepository, operationLogService);
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
    void actualUserClaimGeneratesReceiptAndUpdatesCustodian() {
        AppUser employee = user(3L, UserRole.EMPLOYEE);
        Asset asset = asset(20L, AssetStatus.IN_STOCK);
        when(assetRepository.findDetailById(20L)).thenReturn(Optional.of(asset));
        when(historyRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(any()))
                .thenReturn(Optional.empty());
        AssetClaimRequest request = new AssetClaimRequest();
        request.setUseLocation("科研楼 301");
        request.setRemark("项目使用");

        var response = service.claim(employee, 20L, request);

        assertEquals(AssetStatus.IN_USE, response.status());
        assertEquals(employee.getId(), response.custodianId());
        assertEquals("科研楼 301", response.location());
        verify(historyRepository).save(argThat(history ->
                history.getReceiptNumber().startsWith("LY")
                        && history.getAction() == AssetHistoryAction.CLAIMED));
    }

    @Test
    void assetAlreadyInUseCannotBeClaimedAgain() {
        Asset asset = asset(20L, AssetStatus.IN_USE);
        when(assetRepository.findDetailById(20L)).thenReturn(Optional.of(asset));
        AssetClaimRequest request = new AssetClaimRequest();
        request.setUseLocation("科研楼 301");

        assertThrows(BusinessException.class, () -> service.claim(user(3L, UserRole.EMPLOYEE), 20L, request));
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
