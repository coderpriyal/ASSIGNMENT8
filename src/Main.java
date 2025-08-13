import com.insurance.claims.config.AppConfig;
import com.insurance.claims.model.Claim;
import com.insurance.claims.queue.ClaimQueueManager;
import com.insurance.claims.queue.PolicyDispatcher;
import com.insurance.claims.service.*;
import com.insurance.claims.utils.CSVReader;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
//https://docs.google.com/spreadsheets/d/12T1ZtTs_l-vZzaJpnyNLNMnsyhtGfljiC27369u4M28/edit?gid=424873467#gid=424873467
public class Main {

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);

        System.out.println("[BOOT] Starting Claims Processor with config: " + config);

        // Core shared services
        LoggingService loggingService = new LoggingService(new File("audit.log"));
        ExternalClaimChecker externalChecker = new ExternalClaimChecker(config);
        FraudDetector fraudDetector = new FraudDetector(
                config.getSuspiciousWindowSeconds(),
                config.getSuspiciousThreshold(),
                loggingService
        );
        SummaryReportService summary = new SummaryReportService();

        // Global priority backlog (bounded)
        final BlockingQueue<Claim> globalBacklog = new PriorityBlockingQueue<>(config.getBacklogCapacity());

        // Ingestion: read CSV -> policy queues -> dispatcher exposes 1 head/ policy onto globalBacklog
        ClaimQueueManager queueManager = new ClaimQueueManager(config, fraudDetector, loggingService);
        PolicyDispatcher dispatcher = new PolicyDispatcher(queueManager, globalBacklog, loggingService);

        // Workers
        ExecutorService workerPool = Executors.newFixedThreadPool(config.getWorkerCount());

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Initiating graceful shutdown...");
            dispatcher.stop();
            workerPool.shutdownNow();
            try { workerPool.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            try { loggingService.close(); } catch (Exception ignored) {}
        }));

        long startWall = System.nanoTime();

        // Start dispatcher thread
        Thread dispatcherThread = new Thread(dispatcher, "policy-dispatcher");
        dispatcherThread.start();

        // Start worker threads
        for (int i = 0; i < config.getWorkerCount(); i++) {
            workerPool.submit(new ClaimProcessor(
                    globalBacklog,
                    queueManager,
                    dispatcher,
                    externalChecker,
                    loggingService,
                    summary,
                    config
            ));
        }

        // Ingest CSV (pauses when dispatcher throttles)
        //List<Claim> claims = CSVReader.read(new File("Claims.csv"));
        List<Claim> claims = CSVReader.read(new File("Claims.csv"));

        queueManager.ingest(claims);

        // Signal no more input
        queueManager.finishIntake();

        // Wait for all queues to drain
        dispatcher.waitUntilEmpty();

        // Stop workers
        dispatcher.stop();
        workerPool.shutdown();
        workerPool.awaitTermination(1, TimeUnit.MINUTES);

        long totalWallMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startWall);

        // Final summary
        summary.writeSummary(new File("summary.txt"), totalWallMs);

        loggingService.close();

        System.out.println("[DONE] Completed at " + Instant.now() + ". See audit.log and summary.txt");
    }
}