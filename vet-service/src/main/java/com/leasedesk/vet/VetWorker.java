package com.leasedesk.vet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.LeaseRules;
import com.leasedesk.common.VetCommand;
import com.leasedesk.common.VetEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class VetWorker {

    private final VetRepository vets;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;

    public VetWorker(VetRepository vets, OutboxRepository outbox, ObjectMapper mapper, EntityManager em,
            PlatformTransactionManager manager) {
        this.vets = vets;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @RabbitListener(queues = Channels.DECIDE)
    public void onDecide(VetCommand command) {
        Boolean dead = tx.execute(status -> {
            if (!InboxClaims.claim(em, command.applicationId() + ":decide")) {
                return false;
            }
            boolean liened = LeaseRules.lienedTenant(command.tenantCode());
            VetEntity row = new VetEntity();
            row.setId(command.applicationId());
            row.setStatus(liened ? "REJECTED" : "ACCEPTED");
            vets.save(row);
            OutboxWriter.kafka(outbox, mapper, Channels.VETS, command.applicationId(),
                    new VetEvent(command.applicationId(), row.getStatus()));
            return liened;
        });
        if (Boolean.TRUE.equals(dead)) {
            throw new AmqpRejectAndDontRequeueException("lien");
        }
    }
}
