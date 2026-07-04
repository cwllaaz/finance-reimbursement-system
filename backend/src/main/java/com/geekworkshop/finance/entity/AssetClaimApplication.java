package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_claim_application")
public class AssetClaimApplication extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "applicant_id", nullable = false)
    private AppUser applicant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")
    private Department department;
    @Column(name = "use_location", nullable = false, length = 200)
    private String useLocation;
    @Column(nullable = false, length = 500)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AssetClaimStatus status = AssetClaimStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;
    @Column(name = "review_comment", length = 500)
    private String reviewComment;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public AppUser getApplicant() { return applicant; }
    public void setApplicant(AppUser applicant) { this.applicant = applicant; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getUseLocation() { return useLocation; }
    public void setUseLocation(String useLocation) { this.useLocation = useLocation; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AssetClaimStatus getStatus() { return status; }
    public void setStatus(AssetClaimStatus status) { this.status = status; }
    public AppUser getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(AppUser reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
