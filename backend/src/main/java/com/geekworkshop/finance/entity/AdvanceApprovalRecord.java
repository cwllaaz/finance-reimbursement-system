package com.geekworkshop.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "advance_approval_record")
public class AdvanceApprovalRecord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "advance_application_id", nullable = false)
    private AdvanceApplication advanceApplication;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approver_id")
    private AppUser approver;
    @Column(name = "approval_node", nullable = false, length = 60)
    private String approvalNode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ApprovalAction action;
    @Column(length = 500)
    private String comment;
    public Long getId() { return id; }
    public AdvanceApplication getAdvanceApplication() { return advanceApplication; }
    public void setAdvanceApplication(AdvanceApplication value) { advanceApplication = value; }
    public AppUser getApprover() { return approver; }
    public void setApprover(AppUser value) { approver = value; }
    public String getApprovalNode() { return approvalNode; }
    public void setApprovalNode(String value) { approvalNode = value; }
    public ApprovalAction getAction() { return action; }
    public void setAction(ApprovalAction value) { action = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
