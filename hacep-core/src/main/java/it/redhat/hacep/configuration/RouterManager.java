package it.redhat.hacep.configuration;

/**
 * Created by fabio on 15/07/16.
 */
public interface RouterManager {
    /**
     * Start camel context.
     */
    void start();

    /**
     * Stop camel context.
     */
    void stop();

    /**
     * Suspend the route responsible for the messages ingestion.
     */
    void suspend();

    /**
     * Resume the route responsible for the messages ingestion.
     */
    void resume();
}
