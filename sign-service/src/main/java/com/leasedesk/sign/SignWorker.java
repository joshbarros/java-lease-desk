package com.leasedesk.sign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.SignCommand;
import com.leasedesk.common.SignEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SignWorker {

    private final SignRepository signs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public SignWorker(SignRepository signs, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.signs = signs;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.SIGN)
    @Transactional
    public void onSign(SignCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":sign")) {
            return;
        }
        SignEntity row = new SignEntity();
        row.setId(command.applicationId());
        row.setStatus("ISSUED");
        signs.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.SIGNS, command.applicationId(), new SignEvent(command.applicationId(), "ISSUED"));
    }
}
