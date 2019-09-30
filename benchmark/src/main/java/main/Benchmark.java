package main;

import com.gigaspaces.common.model.CrewMember;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static com.gigaspaces.common.Constants.NUM_OF_CREW_MEMBERS;

public class Benchmark implements InitializingBean, DisposableBean {

    @GigaSpaceContext
    private GigaSpace gigaSpace;
    private static final Logger logger = Logger.getLogger(Benchmark.class.getName());;
    private ScheduledExecutorService queriesService;
    //private ScheduledExecutorService printSummeryService;
    //private OperationsTimeTaker opTimeRecorder;
    //private Summery summery;

    public void run() {
        //opTimeRecorder = new OperationsTimeTaker();
        //summery = new Summery();
        setupLogger();
        log("Start Benchmark");
        try {
            waitForSpaceToFillWithCrewMembers(NUM_OF_CREW_MEMBERS);
            //initPrintSummeryExecuter();
            initDoQueriesExecutor();
        } catch (InterruptedException e) {
            log("--------------------------------------------------------------------");
            log("Benchmark failed: ", e);
            log("--------------------------------------------------------------------");
            queriesService.shutdown();
        }
    }

    private void initDoQueriesExecutor() throws InterruptedException {
        queriesService = Executors.newScheduledThreadPool(1);
        queriesService.scheduleAtFixedRate(this::doRegularQuery, 0, 1, TimeUnit.SECONDS);
        queriesService.scheduleAtFixedRate(this::doBurstQuery, 0, 1, TimeUnit.MINUTES);
        queriesService.awaitTermination(30, TimeUnit.DAYS);
    }

    /*private void initPrintSummeryExecuter() throws InterruptedException {
        printSummeryService = Executors.newScheduledThreadPool(1);
        Runnable task = () -> log(summery.intermediateSummery());
        printSummeryService.scheduleAtFixedRate(task, 0, 30, TimeUnit.SECONDS);
    }*/

    private void waitForSpaceToFillWithCrewMembers(int expectedNumOfCrewMembers) throws InterruptedException {
        boolean finish = false;

        log(MessageFormat.format("Wait for space to fill with {0} crew members", expectedNumOfCrewMembers));

        while (!finish) {
            int numOfCrewMembers = gigaSpace.count(new CrewMember());
            log("++++++++++++++Current num of crew members: ++++++++++++++++++++++++" + numOfCrewMembers);
            if (numOfCrewMembers >= expectedNumOfCrewMembers) {
                finish = true;
            } else {
                log("Waiting for space to fill up with CrewMembers");
                Thread.sleep(20000);
            }
        }
    }

    private void doRegularQuery() {
        try {
            CrewMember[] members = gigaSpace.readMultiple(new SQLQuery<>(CrewMember.class, null), 100);
        } catch (Exception e) {
            log("Got exception: ", e);
            //summery.reportException(e);
        }
    }

    private void doBurstQuery() {
        long startTime = System.currentTimeMillis();

        log("starts burst query");

        while ((System.currentTimeMillis() - startTime) < (1000 * 30)) {
            CrewMember[] manyMembers = gigaSpace.readMultiple(new SQLQuery<>(CrewMember.class, null), 200);
            log("Expected read 200 crew members. Actual is:   " + manyMembers.length);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log("Got exception: ", e);
                //summery.reportException(e);
            }
        }

        log("end of burst query");
    }

/*
    private <T> T[] queryAll(Class <T> type) throws TimeoutException {
        //summery.incTotalQueries();
        //opTimeRecorder.reportOperationStart();
        T[] res = gigaSpace.readMultiple(new SQLQuery<>(type, null), 200);

        if (res == null) {
            throw new TimeoutException();
        } else {
            //long queryDurationNano = opTimeRecorder.reportOperationEnd();
        //    summery.reportQueryDuration(queryDurationNano);
        }

        return res;
    }
*/


    private static void log(String msg, Exception e) {
        logger.log(Level.SEVERE, msg);
        logger.log(Level.SEVERE, "Got Exception: ", e);
    }

    private static void log(String msg) {
        logger.log(Level.INFO, msg);
    }

    private static void setupLogger() {
        logger.setLevel(Level.ALL);
        try {
            String logDirPath = System.getProperty("user.home") + "/logs";
            createLogDirectoryInUserHomeDir(logDirPath);
            String logFilePath = MessageFormat.format("{0}/{1}", logDirPath, generateLogFileNameByTimeStamp());
            FileHandler fhandler = new FileHandler(logFilePath);
            log("Log file created at: " + logFilePath);
            SimpleFormatter sformatter = new SimpleFormatter();
            fhandler.setFormatter(sformatter);
            logger.addHandler(fhandler);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static String generateLogFileNameByTimeStamp() throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss");
        Date date = new Date();

        String filePath = dateFormat.format(date) + ".log";

        return filePath;
    }

    private static boolean createLogDirectoryInUserHomeDir(String path) {
        boolean created = new File(path).mkdirs();

        if (created) {
            log("log directory created at: " + path);
        }

        return created;
    }

    @Override
    public void destroy() throws Exception {
        log("Benchmark End");
        //printSummeryService.shutdown();
        //printSummeryService.awaitTermination(10, TimeUnit.SECONDS);
        queriesService.shutdown();
        queriesService.awaitTermination(10, TimeUnit.SECONDS);
        log("Printing final summery ...");
        //log(summery.finalSummery());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this::run).start();
    }

}