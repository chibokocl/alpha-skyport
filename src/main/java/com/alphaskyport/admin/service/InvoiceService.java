package com.alphaskyport.admin.service;

import com.alphaskyport.admin.dto.InvoiceDTOs.InvoiceListResponse;
import com.alphaskyport.admin.dto.InvoiceDTOs.InvoiceResponse;
import com.alphaskyport.admin.dto.InvoiceDTOs.InvoiceSummary;
import com.alphaskyport.admin.dto.InvoiceDTOs.CreateInvoiceRequest;
import com.alphaskyport.admin.dto.InvoiceDTOs.RecordPaymentRequest;
import com.alphaskyport.admin.dto.InvoiceDTOs.LineItemRequest;
import com.alphaskyport.admin.dto.InvoiceDTOs.LineItemResponse;
import com.alphaskyport.admin.dto.InvoiceDTOs.PaymentSummary;
import com.alphaskyport.admin.model.*;
import com.alphaskyport.admin.repository.*;
import com.alphaskyport.admin.exception.AdminException;
import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class InvoiceService {

        private final InvoiceRepository invoiceRepository;
        private final PaymentRepository paymentRepository;
        private final UserRepository userRepository;
        private final ShipmentRepository shipmentRepository;
        private final AdminActivityService activityService;

        @Transactional(readOnly = true)
        public InvoiceResponse getInvoiceById(UUID invoiceId) {
                Invoice invoice = invoiceRepository.findByIdWithLineItems(invoiceId)
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Invoice not found: " + invoiceId));
                return mapToResponse(invoice);
        }

        @Transactional(readOnly = true)
        public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
                Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Invoice not found: " + invoiceNumber));
                return mapToResponse(invoice);
        }

        @Transactional(readOnly = true)
        public InvoiceListResponse getInvoices(InvoiceStatus status, Pageable pageable) {
                Page<Invoice> page;
                if (status != null) {
                        page = invoiceRepository.findByStatus(status, pageable);
                } else {
                        page = invoiceRepository.findAll(pageable);
                }

                List<InvoiceSummary> summaries = page.getContent().stream()
                                .map(this::mapToSummary)
                                .collect(Collectors.toList());

                return InvoiceListResponse.builder()
                                .invoices(summaries)
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build();
        }

        @Transactional(readOnly = true)
        public InvoiceListResponse getInvoicesByUser(UUID userId, Pageable pageable) {
                Page<Invoice> page = invoiceRepository.findByUser_UserId(userId, pageable);

                List<InvoiceSummary> summaries = page.getContent().stream()
                                .map(this::mapToSummary)
                                .collect(Collectors.toList());

                return InvoiceListResponse.builder()
                                .invoices(summaries)
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build();
        }

        @Transactional(readOnly = true)
        public List<InvoiceResponse> getOverdueInvoices() {
                return invoiceRepository.findOverdueInvoices(LocalDate.now()).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public InvoiceResponse createInvoice(CreateInvoiceRequest request, AdminUser createdBy) {
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "User not found: " + request.getUserId()));

                Shipment shipment = null;
                if (request.getShipmentId() != null) {
                        shipment = shipmentRepository.findById(request.getShipmentId())
                                        .orElseThrow(() -> new AdminException.NotFoundException(
                                                        "Shipment not found: " + request.getShipmentId()));
                }

                String invoiceNumber = invoiceRepository.generateInvoiceNumber();

                Invoice invoice = Invoice.builder()
                                .invoiceNumber(invoiceNumber)
                                .user(user)
                                .shipment(shipment)
                                .status(InvoiceStatus.DRAFT)
                                .issueDate(request.getIssueDate())
                                .dueDate(request.getDueDate())
                                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                                .notes(request.getNotes())
                                .createdBy(createdBy)
                                .subtotal(BigDecimal.ZERO)
                                .totalAmount(BigDecimal.ZERO)
                                .build();

                // Add line items
                for (LineItemRequest itemReq : request.getLineItems()) {
                        InvoiceLineItem item = InvoiceLineItem.builder()
                                        .description(itemReq.getDescription())
                                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity()
                                                        : BigDecimal.ONE)
                                        .unitPrice(itemReq.getUnitPrice())
                                        .taxRate(itemReq.getTaxRate() != null ? itemReq.getTaxRate() : BigDecimal.ZERO)
                                        .lineTotal(itemReq.getUnitPrice().multiply(
                                                        itemReq.getQuantity() != null ? itemReq.getQuantity()
                                                                        : BigDecimal.ONE))
                                        .build();
                        invoice.addLineItem(item);
                }

                invoice = invoiceRepository.save(invoice);

                activityService.logActivity(createdBy, "CREATE_INVOICE", "Invoice", invoice.getInvoiceId().toString(),
                                "Created invoice: " + invoice.getInvoiceNumber(), null, null);

                log.info("Invoice created: {} for user: {}", invoice.getInvoiceNumber(), user.getEmail());
                return mapToResponse(invoice);
        }

        @Transactional
        public InvoiceResponse sendInvoice(UUID invoiceId, AdminUser sentBy) {
                Invoice invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Invoice not found: " + invoiceId));

                if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                        throw new AdminException.InvalidStateException("Only draft invoices can be sent");
                }

                invoice.setStatus(InvoiceStatus.SENT);
                invoice = invoiceRepository.save(invoice);

                log.info("Invoice sent. Email notification would be sent to customer here.");

                activityService.logActivity(sentBy, "SEND_INVOICE", "Invoice", invoice.getInvoiceId().toString(),
                                "Sent invoice: " + invoice.getInvoiceNumber(), null, null);

                return mapToResponse(invoice);
        }

        @Transactional
        public InvoiceResponse recordPayment(RecordPaymentRequest request, AdminUser recordedBy) {
                Invoice invoice = invoiceRepository.findByIdWithPayments(request.getInvoiceId())
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Invoice not found: " + request.getInvoiceId()));

                if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.REFUNDED) {
                        throw new AdminException.InvalidStateException(
                                        "Cannot record payment for cancelled/refunded invoice");
                }

                Payment payment = Payment.builder()
                                .invoice(invoice)
                                .amount(request.getAmount())
                                .paymentMethod(request.getPaymentMethod())
                                .paymentReference(request.getPaymentReference())
                                .paymentDate(request.getPaymentDate())
                                .status(PaymentStatus.COMPLETED)
                                .notes(request.getNotes())
                                .recordedBy(recordedBy)
                                .build();

                paymentRepository.save(payment);

                // Update invoice paid amount
                BigDecimal totalPaid = paymentRepository.sumCompletedPaymentsForInvoice(invoice.getInvoiceId());
                invoice.setPaidAmount(totalPaid != null ? totalPaid : BigDecimal.ZERO);

                // Update invoice status
                if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                        invoice.setStatus(InvoiceStatus.PAID);
                        invoice.setPaidDate(request.getPaymentDate());
                } else if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                        invoice.setStatus(InvoiceStatus.PARTIAL);
                }

                invoice = invoiceRepository.save(invoice);

                activityService.logActivity(recordedBy, "RECORD_PAYMENT", "Invoice", invoice.getInvoiceId().toString(),
                                "Recorded payment of " + request.getAmount() + " for invoice: "
                                                + invoice.getInvoiceNumber(),
                                null, null);

                return mapToResponse(invoice);
        }

        @Transactional
        public InvoiceResponse cancelInvoice(UUID invoiceId, String reason, AdminUser cancelledBy) {
                Invoice invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Invoice not found: " + invoiceId));

                if (invoice.getStatus() == InvoiceStatus.PAID) {
                        throw new AdminException.InvalidStateException("Cannot cancel a paid invoice");
                }

                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoice.setNotes(
                                (invoice.getNotes() != null ? invoice.getNotes() + "\n" : "") + "Cancelled: " + reason);
                invoice = invoiceRepository.save(invoice);

                activityService.logActivity(cancelledBy, "CANCEL_INVOICE", "Invoice", invoice.getInvoiceId().toString(),
                                "Cancelled invoice: " + invoice.getInvoiceNumber() + " - " + reason, null, null);

                return mapToResponse(invoice);
        }

        @Transactional(readOnly = true)
        public BigDecimal getTotalOutstanding() {
                BigDecimal total = invoiceRepository.sumOutstandingAmount();
                return total != null ? total : BigDecimal.ZERO;
        }

        private InvoiceResponse mapToResponse(Invoice invoice) {
                List<LineItemResponse> lineItems = invoice.getLineItems().stream()
                                .map(item -> LineItemResponse.builder()
                                                .lineItemId(item.getLineItemId())
                                                .description(item.getDescription())
                                                .quantity(item.getQuantity())
                                                .unitPrice(item.getUnitPrice())
                                                .taxRate(item.getTaxRate())
                                                .lineTotal(item.getLineTotal())
                                                .build())
                                .collect(Collectors.toList());

                List<PaymentSummary> payments = invoice.getPayments().stream()
                                .map(p -> PaymentSummary.builder()
                                                .paymentId(p.getPaymentId())
                                                .amount(p.getAmount())
                                                .method(p.getPaymentMethod())
                                                .paymentDate(p.getPaymentDate())
                                                .reference(p.getPaymentReference())
                                                .build())
                                .collect(Collectors.toList());

                return InvoiceResponse.builder()
                                .invoiceId(invoice.getInvoiceId())
                                .invoiceNumber(invoice.getInvoiceNumber())
                                .userId(invoice.getUser().getUserId())
                                .customerName(invoice.getUser().getFirstName() + " " + invoice.getUser().getLastName())
                                .customerEmail(invoice.getUser().getEmail())
                                .shipmentId(invoice.getShipment() != null ? invoice.getShipment().getShipmentId()
                                                : null)
                                .trackingNumber(invoice.getShipment() != null
                                                ? invoice.getShipment().getTrackingNumber()
                                                : null)
                                .status(invoice.getStatus())
                                .subtotal(invoice.getSubtotal())
                                .taxAmount(invoice.getTaxAmount())
                                .totalAmount(invoice.getTotalAmount())
                                .paidAmount(invoice.getPaidAmount())
                                .outstandingAmount(invoice.getOutstandingAmount())
                                .currency(invoice.getCurrency())
                                .issueDate(invoice.getIssueDate())
                                .dueDate(invoice.getDueDate())
                                .paidDate(invoice.getPaidDate())
                                .overdue(invoice.isOverdue())
                                .notes(invoice.getNotes())
                                .createdAt(invoice.getCreatedAt())
                                .lineItems(lineItems)
                                .payments(payments)
                                .build();
        }

        private InvoiceSummary mapToSummary(Invoice invoice) {
                return InvoiceSummary.builder()
                                .invoiceId(invoice.getInvoiceId())
                                .invoiceNumber(invoice.getInvoiceNumber())
                                .customerName(invoice.getUser().getFirstName() + " " + invoice.getUser().getLastName())
                                .status(invoice.getStatus())
                                .totalAmount(invoice.getTotalAmount())
                                .outstandingAmount(invoice.getOutstandingAmount())
                                .issueDate(invoice.getIssueDate())
                                .dueDate(invoice.getDueDate())
                                .overdue(invoice.isOverdue())
                                .build();
        }
}
