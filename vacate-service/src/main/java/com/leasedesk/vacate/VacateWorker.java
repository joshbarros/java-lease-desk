package com.leasedesk.vacate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.CancelCommand;
import com.leasedesk.common.Channels;
import com.leasedesk.common.VacateCommand;
import com.leasedesk.common.VacateEvent;
import com.leasedesk.common.VacateUnitCommand;
import com.leasedesk.common.RestoreUnitCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VacateWorker {

    private final VacateRepository vacates;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public VacateWorker(VacateRepository vacates, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.vacates = vacates;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.VACATE)
    @Transactional
    public void onVacate(VacateCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":vacate")) {
            return;
        }
        save(command.applicationId(), command.unitId(), command.feeCents(), "FREED");
        OutboxWriter.rabbit(outbox, mapper, Channels.RELEASE_UNIT, command.applicationId(),
                new VacateUnitCommand(command.applicationId(), command.unitId(), command.feeCents()));
    }

    @RabbitListener(queues = Channels.BREAK)
    @Transactional
    public void onCancel(CancelCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":break")) {
            return;
        }
        save(command.applicationId(), command.unitId(), command.feeCents(), "RETURNED");
        OutboxWriter.rabbit(outbox, mapper, Channels.RESTORE, command.applicationId(),
                new RestoreUnitCommand(command.applicationId(), command.unitId(), command.feeCents()));
    }

    private void save(String applicationId, String unitId, int feeCents, String status) {
        VacateEntity row = new VacateEntity();
        row.setId(applicationId);
        row.setUnitId(unitId);
        row.setFeeCents(feeCents);
        row.setStatus(status);
        vacates.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.VACATES, applicationId, new VacateEvent(applicationId, status));
    }
}
