package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "advance_offset_record")
public class AdvanceOffsetRecord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "advance_application_id", nullable = false)
    private AdvanceApplication advanceApplication;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operator_id", nullable = false)
    private AppUser operator;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(length = 500)
    private String comment;
    public Long getId() { return id; }
    public AdvanceApplication getAdvanceApplication() { return advanceApplication; }
    public void setAdvanceApplication(AdvanceApplication value) { advanceApplication = value; }
    public AppUser getOperator() { return operator; }
    public void setOperator(AppUser value) { operator = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
