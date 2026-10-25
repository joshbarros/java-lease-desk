package com.leasedesk.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.InvoiceEvent;
import com.leasedesk.common.Channels;
import com.leasedesk.common.ChargeCommand;
import com.leasedesk.common.DeedCommand;
import com.leasedesk.common.DeedEvent;
import com.leasedesk.common.SignEvent;
import com.leasedesk.common.HoldEvent;
import com.leasedesk.common.QuoteEvent;
import com.leasedesk.common.KeepCommand;
import com.leasedesk.common.VacateCommand;
import com.leasedesk.common.VetCommand;
import com.leasedesk.common.VetEvent;
import com.leasedesk.common.ReserveCommand;
import com.leasedesk.common.ApplicationFact;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApplicationSaga {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ApplicationSaga(OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @KafkaListener(topics = Channels.QUOTES, groupId = "application-quotes")
    @Transactional
    public void onQuote(QuoteEvent event) {
        if (!InboxClaims.claim(em, event.applicationId() + ":quote")) {
            return;
        }
        ApplicationEntity row = lock(event.applicationId());
        if (row == null || !"QUOTING".equals(row.getStatus())) {
            return;
        }
        if (!"RATED".equals(event.status())) {
            cancel(row, event.reason());
            return;
        }
        row.setFeeCents(event.feeCents());
        row.setStatus("RESERVING");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.RESERVE, row.getId(),
                new ReserveCommand(row.getId(), row.getUnitId(), row.getFeeCents()));
    }

    @KafkaListener(topics = Channels.HOLDS, groupId = "application-holds")
    @Transactional
    public void onHold(HoldEvent event) {
        if (!InboxClaims.claim(em, event.applicationId() + ":hold:" + event.status())) {
            return;
        }
        ApplicationEntity row = lock(event.applicationId());
        if (row == null) {
            return;
        }
        if ("RESERVED".equals(event.status()) && "RESERVING".equals(row.getStatus())) {
            row.setStatus("SCREENING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.DECIDE, row.getId(),
                    new VetCommand(row.getId(), row.getTenantCode(), row.getFeeCents()));
            return;
        }
        if ("SHORT".equals(event.status()) && "RESERVING".equals(row.getStatus())) {
            cancel(row, "SHORT");
            return;
        }
        if ("RELEASED".equals(event.status()) && "VACATING".equals(row.getStatus())) {
            cancel(row, "LIEN");
            return;
        }
        if ("KEPT".equals(event.status()) && "KEEPING".equals(row.getStatus())) {
            row.setStatus("INVOICING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.CHARGE, row.getId(),
                    new ChargeCommand(row.getId(), row.getFeeCents()));
            return;
        }
        if ("RETURNED".equals(event.status()) && ("READY".equals(row.getStatus()) || "SIGNED".equals(row.getStatus()))) {
            row.setStatus("RETURNED");
            fact(row);
        }
    }

    @KafkaListener(topics = Channels.VETS, groupId = "application-vets")
    @Transactional
    public void onVet(VetEvent event) {
        if (!InboxClaims.claim(em, event.applicationId() + ":vet")) {
            return;
        }
        ApplicationEntity row = lock(event.applicationId());
        if (row == null || !"SCREENING".equals(row.getStatus())) {
            return;
        }
        if ("REJECTED".equals(event.status())) {
            row.setStatus("VACATING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.VACATE, row.getId(),
                    new VacateCommand(row.getId(), row.getUnitId(), row.getFeeCents()));
            return;
        }
        if ("ACCEPTED".equals(event.status())) {
            row.setStatus("DRAFTING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.DEED, row.getId(),
                    new DeedCommand(row.getId(), row.getFeeCents()));
        }
    }

    @KafkaListener(topics = Channels.DEEDS, groupId = "application-deeds")
    @Transactional
    public void onDeed(DeedEvent event) {
        if (!InboxClaims.claim(em, event.applicationId() + ":deed-fact")) {
            return;
        }
        ApplicationEntity row = lock(event.applicationId());
        if (row == null || !"DRAFTING".equals(row.getStatus()) || !"ISSUED".equals(event.status())) {
            return;
        }
        row.setStatus("KEEPING");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.KEEP, row.getId(),
                new KeepCommand(row.getId(), row.getUnitId(), row.getFeeCents()));
    }

    @KafkaListener(topics = Channels.INVOICES, groupId = "application-invoices")
    @Transactional
    public void onInvoice(InvoiceEvent event) {
        if (!InboxClaims.claim(em, event.applicationId() + ":invoice")) {
            return;
        }
        ApplicationEntity row = lock(event.applicationId());
        if (row == null || !"INVOICING".equals(row.getStatus()) || !"POSTED".equals(event.status())) {
            return;
        }
        row.setStatus("READY");
        fact(row);
    }

    @KafkaListener(topics = Channels.SIGNS, groupId = "application-signs")
    @Transactional
    public void onSign(SignEvent event) {
        if (!InboxClaims.claim(em, event.applicationId() + ":signature")) {
            return;
        }
        ApplicationEntity row = lock(event.applicationId());
        if (row == null || !"READY".equals(row.getStatus()) || !"ISSUED".equals(event.status())) {
            return;
        }
        row.setStatus("SIGNED");
        fact(row);
    }

    private void cancel(ApplicationEntity row, String reason) {
        row.setStatus("CANCELLED");
        row.setReason(reason == null ? "" : reason);
        fact(row);
    }

    private void fact(ApplicationEntity row) {
        OutboxWriter.kafka(outbox, mapper, Channels.APPLICATIONS, row.getId(),
                new ApplicationFact(row.getId(), row.getStatus(), row.getReason()));
    }

    private ApplicationEntity lock(String id) {
        return em.find(ApplicationEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}
