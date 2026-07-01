package com.geekworkshop.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "labor_approval_record")
public class LaborApprovalRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labor_application_id", nullable = false)
    private LaborApplication laborApplication;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private AppUser approver;
    @Column(name = "approval_node", nullable = false, length = 60)
    private String approvalNode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalAction action;
    @Column(length = 500)
    private String comment;

    public Long getId() { return id; }
    public LaborApplication getLaborApplication() { return laborApplication; }
    public void setLaborApplication(LaborApplication laborApplication) { this.laborApplication = laborApplication; }
    public AppUser getApprover() { return approver; }
    public void setApprover(AppUser approver) { this.approver = approver; }
    public String getApprovalNode() { return approvalNode; }
    public void setApprovalNode(String approvalNode) { this.approvalNode = approvalNode; }
    public ApprovalAction getAction() { return action; }
    public void setAction(ApprovalAction action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
