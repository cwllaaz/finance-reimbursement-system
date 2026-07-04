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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

@Service
public class AssetService {
    private final AssetRepository assetRepository;
    private final AssetAcceptanceRepository acceptanceRepository;
    private final AssetHistoryRepository historyRepository;
    private final PurchaseApplicationRepository purchaseRepository;
    private final AppUserRepository appUserRepository;
    private final AssetClaimApplicationRepository claimApplicationRepository;
    private final OperationLogService operationLogService;

    public AssetService(
            AssetRepository assetRepository,
            AssetAcceptanceRepository acceptanceRepository,
            AssetHistoryRepository historyRepository,
            PurchaseApplicationRepository purchaseRepository,
            AppUserRepository appUserRepository,
            AssetClaimApplicationRepository claimApplicationRepository,
            OperationLogService operationLogService
    ) {
        this.assetRepository = assetRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.historyRepository = historyRepository;
        this.purchaseRepository = purchaseRepository;
        this.appUserRepository = appUserRepository;
        this.claimApplicationRepository = claimApplicationRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> list(AppUser user, String keyword, AssetStatus status) {
        requireAssetAccess(user);
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return assetRepository.findAllDetails().stream()
                .filter(asset -> canView(user, asset))
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
        requireAssetAccess(user);
        Asset asset = requireAsset(id);
        requireView(user, asset);
        List<AssetHistoryResponse> history = historyRepository.findByAssetIdOrderByCreatedAtAsc(id)
                .stream()
                .filter(record -> canViewHistory(user, record))
                .map(AssetHistoryResponse::fromEntity).toList();
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

    @Transactional(readOnly = true)
    public List<AssetUserOptionResponse> claimantOptions(AppUser user) {
        requireOffice(user);
        return appUserRepository.findAllWithDepartment().stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getEnabled()))
                .map(AssetUserOptionResponse::fromEntity)
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
        return new AssetAcceptanceResponse(
                acceptance.getAcceptanceNumber(),
                purchase.getApplicationNumber(),
                user.getId(),
                user.getRealName(),
                acceptance.getReceivedAt(),
                assets.stream().map(asset -> AssetResponse.fromEntity(asset, List.of())).toList());
    }

    @Transactional
    public AssetResponse claim(AppUser user, Long id, AssetClaimRequest request) {
        requireOffice(user);
        Asset asset = requireAsset(id);
        AppUser claimant = appUserRepository.findWithDepartmentById(request.getClaimantUserId())
                .orElseThrow(() -> new BusinessException("领用人不存在"));
        return assignAsset(user, asset, claimant, request.getUseLocation(), request.getRemark());
    }

    @Transactional
    public AssetClaimApplicationResponse requestClaim(
            AppUser user, Long assetId, AssetClaimApplicationRequest request
    ) {
        if (!EnumSet.of(UserRole.EMPLOYEE, UserRole.DEPARTMENT_MANAGER).contains(user.getRole())) {
            throw new ForbiddenException("只有员工和部门负责人可以提交资产领用申请");
        }
        Asset asset = requireAsset(assetId);
        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw new BusinessException("只有库存中的资产可以申请领用");
        }
        if (claimApplicationRepository.existsByAssetIdAndStatus(assetId, AssetClaimStatus.PENDING)) {
            throw new BusinessException("该资产已有待处理的领用申请");
        }
        AssetClaimApplication application = new AssetClaimApplication();
        application.setAsset(asset);
        application.setApplicant(user);
        application.setDepartment(user.getDepartment());
        application.setUseLocation(request.getUseLocation().trim());
        application.setReason(request.getReason().trim());
        AssetClaimApplication saved = claimApplicationRepository.save(application);
        operationLogService.record(user, "资产管理", "申请领用资产", saved.getId(),
                asset.getAssetNumber(), "使用地点：" + application.getUseLocation());
        return AssetClaimApplicationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetClaimApplicationResponse> claimApplications(AppUser user) {
        requireAssetAccess(user);
        return claimApplicationRepository.findAllDetails().stream()
                .filter(value -> hasFullLedgerAccess(user)
                        || isUser(value.getApplicant(), user)
                        || (user.getRole() == UserRole.DEPARTMENT_MANAGER
                            && sameDepartment(value.getApplicant(), user)))
                .map(AssetClaimApplicationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AssetClaimApplicationResponse reviewClaim(
            AppUser user, Long requestId, AssetClaimReviewRequest request
    ) {
        requireOffice(user);
        AssetClaimApplication application = claimApplicationRepository.findDetailById(requestId)
                .orElseThrow(() -> new BusinessException("资产领用申请不存在"));
        if (application.getStatus() != AssetClaimStatus.PENDING) {
            throw new BusinessException("该领用申请已经处理");
        }
        if (!Boolean.TRUE.equals(request.getApproved()) && !StringUtils.hasText(request.getComment())) {
            throw new BusinessException("驳回领用申请时必须填写原因");
        }
        if (Boolean.TRUE.equals(request.getApproved())) {
            assignAsset(user, application.getAsset(), application.getApplicant(),
                    application.getUseLocation(), application.getReason());
            application.setStatus(AssetClaimStatus.APPROVED);
        } else {
            application.setStatus(AssetClaimStatus.REJECTED);
        }
        application.setReviewedBy(user);
        application.setReviewComment(request.getComment());
        application.setReviewedAt(LocalDateTime.now());
        claimApplicationRepository.save(application);
        operationLogService.record(user, "资产管理",
                Boolean.TRUE.equals(request.getApproved()) ? "通过资产领用申请" : "驳回资产领用申请",
                application.getId(), application.getAsset().getAssetNumber(), request.getComment());
        return AssetClaimApplicationResponse.fromEntity(application);
    }

    private AssetResponse assignAsset(
            AppUser user, Asset asset, AppUser claimant, String useLocation, String remark
    ) {
        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw new BusinessException("只有库存中的资产可以领用");
        }
        if (!Boolean.TRUE.equals(claimant.getEnabled())) {
            throw new BusinessException("不能选择已禁用的用户作为领用人");
        }
        AppUser acceptor = asset.getAcceptance().getAcceptedBy();
        if (acceptor != null && acceptor.getId().equals(claimant.getId())) {
            throw new BusinessException("领用人必须是实际资产使用者，不能与验收人相同");
        }
        String receiptNumber = nextReceiptNumber();
        LocalDateTime claimedAt = LocalDateTime.now();
        asset.setClaimedBy(claimant);
        asset.setClaimedAt(claimedAt);
        asset.setCustodian(claimant);
        asset.setLocation(useLocation.trim());
        asset.setStatus(AssetStatus.IN_USE);
        assetRepository.save(asset);
        saveHistory(asset, user, claimant, receiptNumber, AssetHistoryAction.CLAIMED,
                "办公室办理领用，实际使用人：" + claimant.getRealName() + optionalRemark(remark));
        operationLogService.record(user, "资产管理", "领用资产", asset.getId(), asset.getAssetNumber(),
                "领用单号 " + receiptNumber + "，实际使用人：" + claimant.getRealName()
                        + "，使用地点：" + asset.getLocation());
        return detail(user, asset.getId());
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

    private void requireAssetAccess(AppUser user) {
        if (user.getRole() == UserRole.CASHIER) {
            throw new ForbiddenException("出纳不能访问资产管理模块");
        }
    }

    private void requireView(AppUser user, Asset asset) {
        if (!canView(user, asset)) {
            throw new ForbiddenException("无权查看该资产");
        }
    }

    private boolean canView(AppUser user, Asset asset) {
        if (hasFullLedgerAccess(user)) {
            return true;
        }
        if (!EnumSet.of(UserRole.EMPLOYEE, UserRole.DEPARTMENT_MANAGER).contains(user.getRole())) {
            return false;
        }
        return asset.getStatus() == AssetStatus.IN_STOCK
                || isUser(asset.getCustodian(), user)
                || isUser(asset.getClaimedBy(), user)
                || (user.getRole() == UserRole.DEPARTMENT_MANAGER
                    && (sameDepartment(asset.getCustodian(), user)
                        || sameDepartment(asset.getClaimedBy(), user)));
    }

    private boolean canViewHistory(AppUser user, AssetHistory history) {
        if (hasFullLedgerAccess(user)) {
            return true;
        }
        return isUser(history.getCustodian(), user) || isUser(history.getOperator(), user)
                || (user.getRole() == UserRole.DEPARTMENT_MANAGER
                    && sameDepartment(history.getCustodian(), user));
    }

    private boolean hasFullLedgerAccess(AppUser user) {
        return EnumSet.of(
                UserRole.OFFICE,
                UserRole.FINANCE,
                UserRole.EXECUTIVE,
                UserRole.COMMITTEE,
                UserRole.ADMIN
        ).contains(user.getRole());
    }

    private boolean isUser(AppUser candidate, AppUser user) {
        return candidate != null && candidate.getId() != null && candidate.getId().equals(user.getId());
    }

    private boolean sameDepartment(AppUser candidate, AppUser user) {
        return candidate != null && candidate.getDepartment() != null
                && user.getDepartment() != null
                && candidate.getDepartment().getId().equals(user.getDepartment().getId());
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
