package com.leasedesk.quote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.LeaseRules;
import com.leasedesk.common.QuoteEvent;
import com.leasedesk.common.RateCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class QuoteWorker {

    private final QuoteRepository quotes;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public QuoteWorker(QuoteRepository quotes, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.quotes = quotes;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RATE)
    @Transactional
    public void onRate(RateCommand command) {
        if (!InboxClaims.claim(em, command.applicationId() + ":rate")) {
            return;
        }
        if (LeaseRules.emptyMonths(command.months())) {
            reject(command.applicationId(), "EMPTY");
            return;
        }
        if (LeaseRules.overMonths(command.months())) {
            reject(command.applicationId(), "OVER");
            return;
        }
        try {
            int fee = LeaseRules.fee(command.months(), command.rentCents());
            QuoteEntity row = new QuoteEntity();
            row.setId(command.applicationId());
            row.setFeeCents(fee);
            row.setStatus("RATED");
            quotes.save(row);
            OutboxWriter.kafka(outbox, mapper, Channels.QUOTES, command.applicationId(),
                    new QuoteEvent(command.applicationId(), fee, "RATED", ""));
        } catch (ArithmeticException ex) {
            reject(command.applicationId(), "GROSS");
        }
    }

    private void reject(String applicationId, String reason) {
        OutboxWriter.kafka(outbox, mapper, Channels.QUOTES, applicationId, new QuoteEvent(applicationId, 0, "REJECTED", reason));
    }
}
