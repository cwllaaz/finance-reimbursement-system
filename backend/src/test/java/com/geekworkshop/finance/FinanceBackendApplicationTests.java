package com.geekworkshop.finance;

import com.geekworkshop.finance.config.DemoDataInitializer;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FinanceBackendApplicationTests {
	@Autowired DemoDataInitializer demoDataInitializer;
	@Autowired ReimbursementRepository reimbursementRepository;
	@Autowired ApprovalRecordRepository approvalRecordRepository;
	@Autowired AttachmentRepository attachmentRepository;
	@Autowired PurchaseApplicationRepository purchaseRepository;
	@Autowired PurchaseApprovalRecordRepository purchaseApprovalRepository;
	@Autowired PurchaseAttachmentRepository purchaseAttachmentRepository;
	@Autowired LaborApplicationRepository laborRepository;
	@Autowired LaborApprovalRecordRepository laborApprovalRepository;
	@Autowired LaborAttachmentRepository laborAttachmentRepository;
	@Autowired AdvanceApplicationRepository advanceRepository;
	@Autowired AdvanceApprovalRecordRepository advanceApprovalRepository;
	@Autowired AdvanceAttachmentRepository advanceAttachmentRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void demoDataIsCompleteAndIdempotent() throws Exception {
		long reimbursements = reimbursementRepository.count();
		long purchases = purchaseRepository.count();
		long laborCount = laborRepository.count();
		long advances = advanceRepository.count();
		long approvals = approvalRecordRepository.count()
				+ purchaseApprovalRepository.count()
				+ laborApprovalRepository.count()
				+ advanceApprovalRepository.count();

		demoDataInitializer.run();

		assertEquals(reimbursements, reimbursementRepository.count());
		assertEquals(purchases, purchaseRepository.count());
		assertEquals(laborCount, laborRepository.count());
		assertEquals(advances, advanceRepository.count());
		assertEquals(approvals, approvalRecordRepository.count()
				+ purchaseApprovalRepository.count()
				+ laborApprovalRepository.count()
				+ advanceApprovalRepository.count());

		Reimbursement reimbursement = reimbursementRepository.findByApprovalNumber("BX20260101004").orElseThrow();
		assertEquals(ReimbursementStatus.COMPLETED, reimbursement.getStatus());
		assertNotNull(reimbursement.getPaymentDate());
		assertNotNull(reimbursement.getPaymentVoucherNumber());
		assertTrue(attachmentRepository.existsByReimbursementIdAndAttachmentType(
				reimbursement.getId(), AttachmentType.MEETING_MINUTES));
		assertTrue(attachmentRepository.existsByReimbursementIdAndAttachmentType(
				reimbursement.getId(), AttachmentType.BANK_RECEIPT));
		assertEquals(5, approvalRecordRepository
				.findByReimbursementIdOrderByCreatedAtAsc(reimbursement.getId()).size());

		PurchaseApplication purchase = purchaseRepository.findByApplicationNumber("CG20260101004").orElseThrow();
		assertEquals(PurchaseStatus.COMPLETED, purchase.getStatus());
		assertTrue(purchaseAttachmentRepository.existsByPurchaseApplicationIdAndAttachmentType(
				purchase.getId(), PurchaseAttachmentType.MEETING_MINUTES));
		assertEquals(3, purchaseApprovalRepository
				.findByPurchaseApplicationIdOrderByCreatedAtAsc(purchase.getId()).size());

		LaborApplication labor = laborRepository.findByApplicationNumber("LW20260101004").orElseThrow();
		assertEquals(LaborStatus.COMPLETED, labor.getStatus());
		assertNotNull(labor.getPaymentVoucherNumber());
		assertTrue(laborAttachmentRepository.existsByLaborApplicationIdAndAttachmentType(
				labor.getId(), LaborAttachmentType.BANK_RECEIPT));
		assertEquals(5, laborApprovalRepository.findDetailsByApplicationId(labor.getId()).size());

		AdvanceApplication advance = advanceRepository.findByApplicationNumber("YF20260101004").orElseThrow();
		assertEquals(AdvanceStatus.COMPLETED, advance.getStatus());
		assertNotNull(advance.getPaymentVoucherNumber());
		assertTrue(advanceAttachmentRepository.existsByAdvanceApplicationIdAndAttachmentType(
				advance.getId(), AdvanceAttachmentType.BANK_RECEIPT));
		assertEquals(5, advanceApprovalRepository.findDetailsByApplicationId(advance.getId()).size());
	}
}
