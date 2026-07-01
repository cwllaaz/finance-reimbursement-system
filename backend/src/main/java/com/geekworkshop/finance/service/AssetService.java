package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AssetService {
    private final AssetRepository assetRepository;
    private final AssetAcceptanceRepository acceptanceRepository;
    private final AssetHistoryRepository historyRepository;
    private final PurchaseApplicationRepository purchaseRepository;
    private final OperationLogService operationLogService;

    public AssetService(
            AssetRepository assetRepository,
            AssetAcceptanceRepository acceptanceRepository,
            AssetHistoryRepository historyRepository,
            PurchaseApplicationRepository purchaseRepository,
            OperationLogService operationLogService
    ) {
        this.assetRepository = assetRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.historyRepository = historyRepository;
        this.purchaseRepository = purchaseRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> list(AppUser user, String keyword, AssetStatus status) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return assetRepository.findAllDetails().stream()
                .filter(asset -> status == null || asset.getStatus() == status)
                .filter(asset -> normalized == null
                        || asset.getAssetNumber().toLowerCase().contains(normalized)
                        || asset.getItemName().toLowerCase().contains(normalized)
                        || (asset.getCustodian() != null
                            && asset.getCustodian().getRealName().toLowerCase().contains(normalized))
                        || asset.getLocation().toLowerCase().contains(normalized))
                .map(asset -> AssetResponse.fromEntity(asset, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse detail(AppUser user, Long id) {
        Asset asset = requireAsset(id);
        List<AssetHistoryResponse> history = historyRepository.findByAssetIdOrderByCreatedAtAsc(id)
                .stream().map(AssetHistoryResponse::fromEntity).toList();
        return AssetResponse.fromEntity(asset, history);
    }

    @Transactional(readOnly = true)
    public List<PurchaseApplicationResponse> eligiblePurchases(AppUser user) {
        requireOffice(user);
        return purchaseRepository.findAllDetails().stream()
                .filter(purchase -> purchase.getStatus() == PurchaseStatus.COMPLETED)
                .filter(purchase -> !acceptanceRepository.existsByPurchaseApplicationId(purchase.getId()))
                .map(purchase -> PurchaseApplicationResponse.fromEntity(purchase, List.of(), List.of()))
                .toList();
    }

    @Transactional
    public AssetAcceptanceResponse acceptInbound(AppUser user, AssetAcceptanceRequest request) {
        requireOffice(user);
        PurchaseApplication purchase = purchaseRepository.findDetailById(request.getPurchaseApplicationId())
                .orElseThrow(() -> new BusinessException("关联申购单不存在"));
        if (purchase.getStatus() != PurchaseStatus.COMPLETED) {
            throw new BusinessException("只有审批完成的申购单可以验收入库");
        }
        if (acceptanceRepository.existsByPurchaseApplicationId(purchase.getId())) {
            throw new BusinessException("该申购单已经完成验收入库");
        }

        AssetAcceptance acceptance = new AssetAcceptance();
        acceptance.setAcceptanceNumber(nextAcceptanceNumber());
        acceptance.setPurchaseApplication(purchase);
        acceptance.setAcceptedBy(user);
        acceptance.setReceivedAt(request.getReceivedAt());
        acceptance.setStorageLocation(request.getStorageLocation().trim());
        acceptance.setRemark(request.getRemark());
        acceptanceRepository.save(acceptance);

        List<Asset> assets = purchase.getItems().stream().map(item -> {
            Asset asset = new Asset();
            asset.setAssetNumber(nextAssetNumber());
            asset.setAcceptance(acceptance);
            asset.setPurchaseItem(item);
            asset.setItemName(item.getItemName());
            asset.setSpecification(item.getSpecification());
            asset.setManufacturer(item.getManufacturer());
            asset.setQuantity(item.getQuantity());
            asset.setTotalPrice(item.getTotalPrice());
            asset.setReceivedAt(request.getReceivedAt());
            asset.setLocation(request.getStorageLocation().trim());
            asset.setStatus(AssetStatus.IN_STOCK);
            Asset saved = assetRepository.save(asset);
            saveHistory(saved, user, null, null, AssetHistoryAction.ACCEPTED_INBOUND,
                    "办公室验收入库" + optionalRemark(request.getRemark()));
            return saved;
        }).toList();

        purchase.setAssetAcceptanceNumber(acceptance.getAcceptanceNumber());
        purchaseRepository.save(purchase);
        operationLogService.record(user, "资产管理", "验收入库", acceptance.getId(),
                acceptance.getAcceptanceNumber(), "关联申购单 " + purchase.getApplicationNumber()
                        + "，生成资产 " + assets.size() + " 项");
        return new AssetAcceptanceResponse(acceptance.getAcceptanceNumber(), purchase.getApplicationNumber(),
                assets.stream().map(asset -> AssetResponse.fromEntity(asset, List.of())).toList());
    }

    @Transactional
    public AssetResponse claim(AppUser user, Long id, AssetClaimRequest request) {
        Asset asset = requireAsset(id);
        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw new BusinessException("只有库存中的资产可以领用");
        }
        String receiptNumber = nextReceiptNumber();
        asset.setCustodian(user);
        asset.setLocation(request.getUseLocation().trim());
        asset.setStatus(AssetStatus.IN_USE);
        assetRepository.save(asset);
        saveHistory(asset, user, user, receiptNumber, AssetHistoryAction.CLAIMED,
                "使用人领用" + optionalRemark(request.getRemark()));
        operationLogService.record(user, "资产管理", "领用资产", asset.getId(), asset.getAssetNumber(),
                "领用单号 " + receiptNumber + "，使用地点：" + asset.getLocation());
        return detail(user, id);
    }

    private void saveHistory(
            Asset asset, AppUser operator, AppUser custodian, String receiptNumber,
            AssetHistoryAction action, String remark
    ) {
        AssetHistory history = new AssetHistory();
        history.setAsset(asset);
        history.setReceiptNumber(receiptNumber);
        history.setAction(action);
        history.setOperator(operator);
        history.setCustodian(custodian);
        history.setLocation(asset.getLocation());
        history.setAssetStatus(asset.getStatus());
        history.setRemark(remark);
        historyRepository.save(history);
    }

    private Asset requireAsset(Long id) {
        return assetRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("资产不存在"));
    }

    private void requireOffice(AppUser user) {
        if (user.getRole() != UserRole.OFFICE && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("只有办公室或管理员可以办理验收入库");
        }
    }

    private synchronized String nextAssetNumber() {
        String prefix = "ZC" + today();
        int sequence = assetRepository.findTopByAssetNumberStartingWithOrderByAssetNumberDesc(prefix)
                .map(Asset::getAssetNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", sequence);
    }

    private synchronized String nextReceiptNumber() {
        String prefix = "LY" + today();
        int sequence = historyRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(prefix)
                .map(AssetHistory::getReceiptNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", sequence);
    }

    private synchronized String nextAcceptanceNumber() {
        String prefix = "YS" + today();
        int sequence = acceptanceRepository.findTopByAcceptanceNumberStartingWithOrderByAcceptanceNumberDesc(prefix)
                .map(AssetAcceptance::getAcceptanceNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", sequence);
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String optionalRemark(String remark) {
        return StringUtils.hasText(remark) ? "：" + remark.trim() : "";
    }
}
