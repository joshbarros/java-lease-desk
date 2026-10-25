package com.leasedesk.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.InvoiceEvent;
import com.leasedesk.common.Channels;
import com.leasedesk.common.ChargeCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InvoiceWorker {

    private final InvoiceRepository invoices;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public InvoiceWorker(InvoiceRepository invoices, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.invoices = invoices;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.CHARGE)
    @Transactional
    public void onCharge(ChargeCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":charge")) {
            return;
        }
        InvoiceEntity row = new InvoiceEntity();
        row.setId(command.applicationId());
        row.setFeeCents(command.feeCents());
        row.setStatus("POSTED");
        invoices.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.INVOICES, command.applicationId(),
                new InvoiceEvent(command.applicationId(), row.getFeeCents(), "POSTED"));
    }
}
