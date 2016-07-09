package it.redhat.hacep.playground.rules.reward.catalog;

import org.kie.api.event.rule.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RulesFiredAgendaEventListener implements AgendaEventListener {

    private List<AfterMatchFiredEvent> afterMatchFiredEvents = new ArrayList<>();

    public void clear() {
        afterMatchFiredEvents.clear();
    }

    public List<AfterMatchFiredEvent> getAfterMatchFiredEvents() {
        return Collections.unmodifiableList(afterMatchFiredEvents);
    }

    @Override
    public void matchCreated(MatchCreatedEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void matchCancelled(MatchCancelledEvent event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void beforeMatchFired(BeforeMatchFiredEvent event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void afterMatchFired(AfterMatchFiredEvent event) {
        afterMatchFiredEvents.add(event);
    }

    @Override
    public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void agendaGroupPushed(AgendaGroupPushedEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
        // TODO Auto-generated method stub

    }

}
