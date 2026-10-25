package com.leasedesk.audit;

import java.time.Instant;

import com.leasedesk.common.InvoiceEvent;
import com.leasedesk.common.Channels;
import com.leasedesk.common.DeedEvent;
import com.leasedesk.common.SignEvent;
import com.leasedesk.common.HoldEvent;
import com.leasedesk.common.QuoteEvent;
import com.leasedesk.common.VacateEvent;
import com.leasedesk.common.VetEvent;
import com.leasedesk.common.ApplicationFact;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditWorker {

    private final AuditRepository entries;

    public AuditWorker(AuditRepository entries) {
        this.entries = entries;
    }

    @KafkaListener(topics = Channels.APPLICATIONS, groupId = "audit-applications")
    @Transactional
    public void onApplication(ApplicationFact event) {
        add("ApplicationFact", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.QUOTES, groupId = "audit-quotes")
    @Transactional
    public void onQuote(QuoteEvent event) {
        add("QuoteEvent", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.VETS, groupId = "audit-vets")
    @Transactional
    public void onVet(VetEvent event) {
        add("VetEvent", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.HOLDS, groupId = "audit-holds")
    @Transactional
    public void onHold(HoldEvent event) {
        add("HoldEvent", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.DEEDS, groupId = "audit-deeds")
    @Transactional
    public void onDeed(DeedEvent event) {
        add("DeedEvent", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.INVOICES, groupId = "audit-invoices")
    @Transactional
    public void onInvoice(InvoiceEvent event) {
        add("InvoiceEvent", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.SIGNS, groupId = "audit-signs")
    @Transactional
    public void onSign(SignEvent event) {
        add("SignEvent", event.applicationId(), event.status());
    }

    @KafkaListener(topics = Channels.VACATES, groupId = "audit-vacates")
    @Transactional
    public void onVacate(VacateEvent event) {
        add("VacateEvent", event.applicationId(), event.status());
    }

    private void add(String kind, String applicationId, String status) {
        String id = kind + ":" + applicationId + ":" + status;
        if (entries.existsById(id)) {
            return;
        }
        AuditEntry row = new AuditEntry();
        row.setId(id);
        row.setApplicationId(applicationId);
        row.setKind(kind);
        row.setStatus(status);
        row.setRecordedAt(Instant.now());
        entries.save(row);
    }
}
