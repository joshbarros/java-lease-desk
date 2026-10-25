package com.leasedesk.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.HoldEvent;
import com.leasedesk.common.KeepCommand;
import com.leasedesk.common.VacateUnitCommand;
import com.leasedesk.common.RestoreUnitCommand;
import com.leasedesk.common.ReserveCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnitWorker {

    private final UnitRepository units;
    private final HoldRepository holds;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public UnitWorker(UnitRepository units, HoldRepository holds, OutboxRepository outbox, ObjectMapper mapper,
            EntityManager em) {
        this.units = units;
        this.holds = holds;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RESERVE, concurrency = "4")
    @Transactional
    public void onReserve(ReserveCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":reserve")) {
            return;
        }
        if (units.reserve(command.unitId()) != 1) {
            publish(command.applicationId(), command.unitId(), command.feeCents(), "SHORT");
            return;
        }
        if (holds.insertHold(command.applicationId(), command.unitId(), command.feeCents()) != 1) {
            throw new IllegalStateException("hold");
        }
        publish(command.applicationId(), command.unitId(), command.feeCents(), "RESERVED");
    }

    @RabbitListener(queues = Channels.RELEASE_UNIT)
    @Transactional
    public void onVacate(VacateUnitCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":vacate-unit")) {
            return;
        }
        if (holds.vacateHold(command.applicationId(), command.unitId(), command.feeCents()) != 1) {
            return;
        }
        if (units.credit(command.unitId()) != 1) {
            throw new IllegalStateException("slot");
        }
        publish(command.applicationId(), command.unitId(), command.feeCents(), "RELEASED");
    }

    @RabbitListener(queues = Channels.KEEP)
    @Transactional
    public void onKeep(KeepCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":keep")) {
            return;
        }
        if (holds.keepHold(command.applicationId(), command.unitId(), command.feeCents()) == 1) {
            publish(command.applicationId(), command.unitId(), command.feeCents(), "KEPT");
        }
    }

    @RabbitListener(queues = Channels.RESTORE)
    @Transactional
    public void onRestore(RestoreUnitCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":restore")) {
            return;
        }
        if (holds.restoreHold(command.applicationId(), command.unitId(), command.feeCents()) != 1) {
            return;
        }
        if (units.credit(command.unitId()) != 1) {
            throw new IllegalStateException("slot");
        }
        publish(command.applicationId(), command.unitId(), command.feeCents(), "RETURNED");
    }

    private void publish(String applicationId, String unitId, int feeCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.HOLDS, applicationId,
                new HoldEvent(applicationId, unitId, feeCents, status));
    }
}
