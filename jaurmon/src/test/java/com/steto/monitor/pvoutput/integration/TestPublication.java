package com.steto.monitor.pvoutput.integration;

import com.google.common.eventbus.EventBus;
import com.steto.monitor.FakePVOutputServer;
import com.steto.monitor.MonitorMsgInverterStatus;
import com.steto.monitor.PeriodicInverterTelemetries;
import com.steto.monitor.RandomObjectGenerator;
import com.steto.monitor.pvoutput.PVOutputParams;
import com.steto.monitor.pvoutput.PvOutputNew;
import com.steto.monitor.pvoutput.PvOutputRecord;
import com.steto.utils.HttpUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.steto.monitor.RandomObjectGenerator.getInt;
import static com.steto.monitor.TestUtility.createPvoutputConfigFile;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

/**
 * Created by stefano on 14/02/16.
 */
public class TestPublication {



    @Test
    public void shouldSaveAndPublishData() throws IOException, ConfigurationException, InterruptedException {

        //Setup
        Integer pvOutputPort = getInt(65532);
        String pvOutServiceUrl = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + pvOutputPort + pvOutServiceUrl;

        PVOutputParams pvOutputParams = RandomObjectGenerator.getA_PvOutputParams();
        pvOutputParams.url = pvOutUrl;
        pvOutputParams.period = (float) 0.2;
        pvOutputParams.timeWindowSec = (float) 0.4;

        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(pvOutputPort, pvOutputParams.apiKey, pvOutputParams.systemId, pvOutServiceUrl);

        EventBus eventBus = new EventBus();
        File tempFile = File.createTempFile("aurora", "cfg");
        createPvoutputConfigFile(tempFile.getAbsolutePath(), pvOutputParams);
        PvOutputNew pvOutput = new PvOutputNew(tempFile.getAbsolutePath(), eventBus);
        pvOutput.start();
        Thread.sleep(300);


        MonitorMsgInverterStatus monitorMsgInverterStatus = new MonitorMsgInverterStatus(false);

        //Exercise
        PeriodicInverterTelemetries periodicInverterTelemetries1 = RandomObjectGenerator.getA_PeriodicInverterTelemetries();
        eventBus.post( periodicInverterTelemetries1 );
        Thread.sleep((long) (pvOutputParams.timeWindowSec*1000*1.1));
        PeriodicInverterTelemetries periodicInverterTelemetries2 = RandomObjectGenerator.getA_PeriodicInverterTelemetries();
        eventBus.post(periodicInverterTelemetries2);
        Thread.sleep(2000);
        pvOutput.stop();

        PvOutputNew pvOutputSecondRun = new PvOutputNew(tempFile.getAbsolutePath(), eventBus);
        pvOutputSecondRun.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> fakeServerExecutorFuture = executor.submit(fakePVOutputServer);
        eventBus.post(monitorMsgInverterStatus);

        //Verify
        String httpString = fakePVOutputServer.waitForRequest(2000);
        assertNotNull(httpString);
        String generatedRequest = fakePVOutputServer.getLastRequest();
        Map<String, String> queryMap = HttpUtils.getQueryMap(generatedRequest);

        String csvData = (queryMap.get("data"));

        String[] recordsValues = csvData.split(";");
        String[] values = recordsValues[0].split(",");

        PvOutputRecord pvOutputRecord1 = new PvOutputRecord(periodicInverterTelemetries1);
        PvOutputRecord pvOutputRecord2 = new PvOutputRecord(periodicInverterTelemetries2);

        assertEquals(values[0], PvOutputNew.convertDate(pvOutputRecord1.getDate()));
        assertEquals(values[1], PvOutputNew.convertDayTime(pvOutputRecord1.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord1.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord1.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord1.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord1.totalGridVoltage, 0.0001);

        values = recordsValues[1].split(",");

        assertEquals(values[0], PvOutputNew.convertDate(pvOutputRecord2.getDate()));
        assertEquals(values[1], PvOutputNew.convertDayTime(pvOutputRecord2.getDate()));
        assertEquals(Float.parseFloat(values[2]), pvOutputRecord2.dailyCumulatedEnergy, 0.0001);
        assertEquals(Float.parseFloat(values[3]), pvOutputRecord2.totalPowerGenerated, 0.0001);
        assertEquals(Float.parseFloat(values[4]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[5]), -1, 0.0001);
        assertEquals(Float.parseFloat(values[6]), pvOutputRecord2.temperature, 0.0001);
        assertEquals(Float.parseFloat(values[7]), pvOutputRecord2.totalGridVoltage, 0.0001);


    }
}
