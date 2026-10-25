package com.leasedesk.deed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.DeedCommand;
import com.leasedesk.common.DeedEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeedWorker {

    private final DeedRepository deeds;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public DeedWorker(DeedRepository deeds, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.deeds = deeds;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.DEED)
    @Transactional
    public void onDeed(DeedCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":deed")) {
            return;
        }
        DeedEntity row = new DeedEntity();
        row.setId(command.applicationId());
        row.setFeeCents(command.feeCents());
        row.setStatus("ISSUED");
        deeds.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.DEEDS, command.applicationId(),
                new DeedEvent(command.applicationId(), row.getFeeCents(), "ISSUED"));
    }
}
