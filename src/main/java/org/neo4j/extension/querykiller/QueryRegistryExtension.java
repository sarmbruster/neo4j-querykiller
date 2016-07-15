package org.neo4j.extension.querykiller;

import java.util.*;
import java.util.concurrent.*;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.neo4j.extension.querykiller.events.bind.BindTransactionEvent;
import org.neo4j.extension.querykiller.events.bind.UnbindTransactionEvent;
import org.neo4j.extension.querykiller.events.cypher.CypherContext;
import org.neo4j.extension.querykiller.events.cypher.ResetCypherContext;
import org.neo4j.extension.querykiller.events.query.QueryAbortedEvent;
import org.neo4j.extension.querykiller.events.query.QueryRegisteredEvent;
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent;
import org.neo4j.extension.querykiller.events.transport.EmbeddedTransportContext;
import org.neo4j.extension.querykiller.events.transport.ResetTransportContext;
import org.neo4j.extension.querykiller.events.transport.TransportContext;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.configuration.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryRegistryExtension implements DefaultLifecycle
{
    public final Logger log = LoggerFactory.getLogger(QueryRegistryExtension.class);

    public final static Setting<Long> QUERY_TIMEOUT_SETTING = Settings.setting("querykiller.timeout", Settings.DURATION, "0");

    // maps threadId to Entry
    protected final Map<Long, TransactionEntry> transactionEntryMap = new ConcurrentHashMap<>();

    // maps threadId to Stack of Cypher statements (we might have multiple cypher statements for tx)
    protected final Map<Long, CypherContext> cypherContextForThread = new ConcurrentHashMap<>();

    // map threadId to transportContext, either embedded, http or bolt
    protected final Map<Long, TransportContext> transportContextMap = new ConcurrentHashMap<>();

    private EventBus eventBus;
    private final QueryRegistryExtensionFactory.Dependencies dependencies;
    private long queryTimeout;
    private ScheduledExecutorService queryTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    public QueryRegistryExtension(QueryRegistryExtensionFactory.Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Subscribe
    public void handleBindTransaction(BindTransactionEvent event) {
        TransactionEntry transactionEntry = new TransactionEntry(event.getKernelTransaction(), queryTimeout, this);
        transactionEntryMap.put( Thread.currentThread().getId(), transactionEntry);
        if (log.isDebugEnabled()) {
            log.debug("registered query for key " + transactionEntry);
        }
        eventBus.post(new QueryRegisteredEvent(transactionEntry));
    }

    @Subscribe
    public void handleUnbindTransaction(UnbindTransactionEvent event) {
        long currentThreadId = Thread.currentThread().getId();
        TransactionEntry transactionEntry = transactionEntryMap.get(currentThreadId);
//        new Exception("HURZ").printStackTrace();
        if ((transactionEntry != null) && transactionEntry.getKernelTransaction().equals(event.getKernelTransaction())) {
            transactionEntryMap.remove(currentThreadId);
            if (log.isDebugEnabled()) {
                log.debug("unregistered query for key " + transactionEntry);
            }
            eventBus.post(new QueryUnregisteredEvent(transactionEntry, cypherContextForThread(currentThreadId) ));
        } else {
            if (log.isDebugEnabled()) {
                log.info(event + " is not registered here.");
            }
        }
    }

    @Subscribe
    public void handleCypherContext(CypherContext context) {
        long threadId = Thread.currentThread().getId();
        cypherContextForThread.put(threadId, context);
        if (log.isDebugEnabled()) {
            log.debug("set context to " + context + ", " + transactionEntryMap.get(threadId).getKey());
        }
    }

    @Subscribe
    public void handleResetCypherContext(ResetCypherContext event) {
        // do nothing intentionally
        log.debug("resetted context");
    }

    @Subscribe
    public void handleTransportContext(TransportContext event) {
        long currentThreadId = Thread.currentThread().getId();
        transportContextMap.put(currentThreadId, event);
    }

    @Subscribe
    public void handleResetTransportContext(ResetTransportContext event) {
        long currentThreadId = Thread.currentThread().getId();
        transportContextMap.remove(currentThreadId);
    }

    public TransactionEntry abortQuery(String key) {
        TransactionEntry entry = findQueryRegistryEntryForKey( key );
        entry.kill(eventBus);
        if (log.isDebugEnabled()) {
            log.info("aborted query for key " + key);
        }
        transactionEntryMap.remove(entry);
        eventBus.post(new QueryAbortedEvent(entry));
        return entry;
    }

    // TODO: consider maintaining map structure for speeding up
    private TransactionEntry findQueryRegistryEntryForKey(String key )
    {
        for (TransactionEntry entry: transactionEntryMap.values()) {
            if (key.equals( entry.getKey() )) {
                return entry;
            }
        }
        throw new NoSuchQueryException(key);
    }

    // TODO: consider maintaining map structure for speeding up
    private TransactionEntry findQueryRegistryEntryForTransaction(KernelTransaction transaction) {
        for (TransactionEntry entry : transactionEntryMap.values()) {
            if (transaction.equals(entry.getKernelTransaction())) {
                return entry;
            }
        }
        throw new NoSuchQueryException(transaction);
    }

    public SortedSet<TransactionEntry> getTransactionEntries() {
        return new TreeSet<>(transactionEntryMap.values());
    }

    @Override
    public void start() throws Throwable
    {
        eventBus = dependencies.getEventBusLifecycle();
        eventBus.register(this);
        startTerminationByTimeout(dependencies.getConfig().get(QUERY_TIMEOUT_SETTING));
    }

    @Override
    public void stop() throws Throwable
    {
        if (queryTimeoutExecutor!=null) {
            queryTimeoutExecutor.shutdown();
        }
        eventBus.unregister(this);
    }

    public String formatAsTable()
    {
        StringBuilder sb = new StringBuilder(  );
        sb.append(      "+---------+----------+--------------------------------------------------------------+-----------------+-----------------+\n")
                .append("| time ms | key      | query                                                        | source          | endPoint        |\n");
        for (TransactionEntry transactionEntry : getTransactionEntries()) {
            sb.append(formatEntry(transactionEntry)).append("\n");
        }

        sb.append("+---------+----------+--------------------------------------------------------------+-----------------+-----------------+\n");
        return sb.toString();
    }

    private String formatEntry(TransactionEntry entry) {
        long duration = System.currentTimeMillis() - entry.getStarted().getTime();
        long threadId = entry.getThreadId();
        TransportContext transportContext = transportContextForThread(threadId);
        return String.format("| %7d | %8s | %-60.60s | %-15.15s | %-15.15s |",
                duration,
                entry.getKey().substring(0, 8),
                cypherContextForThread(threadId),
                transportContext.getRemoteHost(),
                transportContext.getEndPoint()
        );
    }

    public String cypherContextForThread(long threadId) {
        CypherContext context = cypherContextForThread.get(threadId);
        return context == null ? null : context.toString();
    }

    public TransportContext transportContextForThread(long threadId) {
        TransportContext transportContext = transportContextMap.get(threadId);
        return transportContext == null ? EmbeddedTransportContext.getInstance() : transportContext;
    }

    public void startTerminationByTimeout(long queryTimeout) {
        stopTerminationByTimeout();  // just to be sure
        this.queryTimeout = queryTimeout;
        if (this.queryTimeout > 0) {
            scheduledFuture = queryTimeoutExecutor.scheduleAtFixedRate(() -> {
                log.warn("check for old queries");
                long now = System.currentTimeMillis();
                for (TransactionEntry entry : transactionEntryMap.values()) {
                    if ((!entry.isKilled()) && (entry.isDueForTermination(now))) {
                        entry.kill(eventBus);
                    }
                }
            }, this.queryTimeout, this.queryTimeout, TimeUnit.MILLISECONDS);
        }
    }

    public boolean stopTerminationByTimeout() {
        if (scheduledFuture==null) {
            return false;
        } else {
            return scheduledFuture.cancel(false);
        }
    }


}
