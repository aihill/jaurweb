package com.steto.jaurmon.monitor.integration;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurmon.monitor.AuroraMonitorOld;
import com.steto.jaurmon.monitor.webserver.AuroraWebServer;
import com.steto.jaurmon.monitor.FakeAuroraWebClient;
import com.steto.jaurmon.monitor.FakePVOutputServer;
import jssc.SerialPortException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by stefano on 23/12/14.
 */
public class TestHttpResponseToWebClient {

    String configFile = "resources/aurora.cfg";
    AuroraMonitorOld auroraMonitorOld = null;
    AuroraWebServer auroraWebServer = null;
    int auroraServicePort = 8085;
    private String pvOutDirPath;
    EventBus theEventBus = new EventBus();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Before
    public void before() throws IOException, InterruptedException, SerialPortException {

        pvOutDirPath = tempFolder.newFolder().getAbsolutePath();
        auroraMonitorOld = new AuroraMonitorOld(mock(AuroraDriver.class), configFile, pvOutDirPath);
        Thread.sleep(500);
        auroraWebServer = new AuroraWebServer(auroraServicePort, "./html/bootjack/web", theEventBus);
        new Thread(auroraWebServer).start();
        Thread.sleep(200);

    }

    @After
    public void after() throws Exception {

        tempFolder.delete();
        auroraWebServer.stop();
        auroraMonitorOld.stop();
        Thread.sleep(200);

    }


    @Test
    public void shouldSendStatus() throws Exception {

        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        String jsonResult = fakeAuroraWebClient.sendStatusRequest();
        Gson gson = new Gson();
        Map result = gson.fromJson(jsonResult, Map.class);
        assertEquals(auroraMonitorOld.isInverterOnline() ? "online" : "offline", result.get("inverterStatus"));
        assertEquals(auroraMonitorOld.getPvOutputRunningStatus() ? "on" : "off", result.get("pvOutputStatus"));

    }


    @Test
    public void shouldLoadPvOutputParams() throws Exception {

        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        String jsonResult = fakeAuroraWebClient.sendLoadInvSettingsRequest();
        Gson gson = new Gson();
        Map result = gson.fromJson(jsonResult, Map.class);
        assertEquals(auroraMonitorOld.getPvOutputApiKey(), result.get("pvOutputApiKey"));
        assertEquals(auroraMonitorOld.getPvOutputUrl(), result.get("pvOutputUrl"));
        assertEquals(auroraMonitorOld.getPvOutputPeriod(), (double) result.get("pvOutputPeriod"), 0.00001);
        assertEquals(auroraMonitorOld.getPvOutputSystemId(), (double) result.get("pvOutputSystemId"), 0.00001);
        assertEquals(auroraMonitorOld.getSerialPortName(), result.get("serialPort"));
        assertEquals(auroraMonitorOld.getSerialPortBaudRate(), (double) result.get("baudRate"), 0.00001);
        assertEquals(auroraMonitorOld.getInverterAddress(), (double) result.get("inverterAddress"), 0.00001);

    }

    @Test
    public void shouldStorePvOutputParams() throws Exception {

        String key = "aRandomKey";
        Integer systemId = 1122;
        String url = "http://something";
        Float period = Float.valueOf(5);
        Map<String, String> mapConfig = new HashMap<>();
        mapConfig.put("pvOutputApiKey", key);
        mapConfig.put("pvOutputSystemId", systemId.toString());
        mapConfig.put("pvOutputUrl", url);
        mapConfig.put("pvOutputPeriod", period.toString());


        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendSaveConfigurationRequest(mapConfig);

        // verify
        assertEquals(key, auroraMonitorOld.getPvOutputApiKey());
        assertEquals(url, auroraMonitorOld.getPvOutputUrl());
        assertEquals(systemId.intValue(), auroraMonitorOld.getPvOutputSystemId());
        assertEquals(period, auroraMonitorOld.getPvOutputPeriod(), 0001);
    }

    @Test
    public void shouldSavePvOutputParams() throws Exception {

        String key = "aRandomKey";
        Integer systemId = 1122;
        String url = "http://something";
        Integer period = 5;
        Map<String, String> mapConfig = new HashMap<>();
        mapConfig.put("pvOutputApiKey", key);
        mapConfig.put("pvOutputSystemId", systemId.toString());
        mapConfig.put("pvOutputUrl", url);
        mapConfig.put("pvOutputPeriod", period.toString());


        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);
        Thread.sleep(500);

        // exercise
        AuroraMonitorOld auroraMonitorOldBis = new AuroraMonitorOld(mock(AuroraDriver.class), configFile, pvOutDirPath);

        assertEquals(key, auroraMonitorOldBis.getPvOutputApiKey());
        assertEquals(url, auroraMonitorOldBis.getPvOutputUrl());
        assertEquals(systemId.intValue(), auroraMonitorOldBis.getPvOutputSystemId());
        assertEquals(period.intValue(), auroraMonitorOldBis.getPvOutputPeriod());
    }

    @Test
    public void shouldPerformPVOutputConnectionTest() throws Exception {

        Integer pvOutputPort = 8082;
        String pvOutServiceUrl = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + pvOutputPort + pvOutServiceUrl;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;
        Integer period = 5;

        Map<String, String> mapConfig = new HashMap<>();
        mapConfig.put("pvOutputApiKey", pvOutKey);
        mapConfig.put("pvOutputSystemId", systemId.toString());
        mapConfig.put("pvOutputUrl", pvOutUrl);
        mapConfig.put("pvOutputPeriod", period.toString());


        FakePVOutputServer fakePVOutputServer = new FakePVOutputServer(pvOutputPort, pvOutKey, systemId, pvOutServiceUrl);
        new Thread(fakePVOutputServer).start();
        Thread.sleep(1000);

        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendTestPVOutputRequest(mapConfig);
        //verify
        Gson gson = new Gson();
        Map result = gson.fromJson(jsonResult, Map.class);
        assertEquals("OK", result.get("result"));
        assertEquals(pvOutUrl, auroraMonitorOld.getPvOutputUrl());
        assertEquals(pvOutKey, auroraMonitorOld.getPvOutputApiKey());
        assertEquals(systemId.intValue(), auroraMonitorOld.getPvOutputSystemId());


    }

    @Test
    public void shouldPerformPVOutputConnectionTestOnFailure() throws Exception {

        Integer pvOutputPort = 8082;
        String pvOutServiceUrl = "/pvoutputservice";
        String pvOutUrl = "http://localhost:" + pvOutputPort + pvOutServiceUrl;
        String pvOutKey = "a908fds653";
        Integer systemId = 1223;
        Integer period = 5;

        Map<String, String> mapConfig = new HashMap<>();
        mapConfig.put("pvOutputApiKey", pvOutKey);
        mapConfig.put("pvOutputSystemId", systemId.toString());
        mapConfig.put("pvOutputUrl", pvOutUrl);
        mapConfig.put("pvOutputPeriod", period.toString());


        FakeAuroraWebClient fakeAuroraWebClient = new FakeAuroraWebClient("http://localhost:" + auroraServicePort);

        // exercise
        String jsonResult = fakeAuroraWebClient.sendTestPVOutputRequest(mapConfig);
        //verify
        Gson gson = new Gson();
        Map result = gson.fromJson(jsonResult, Map.class);
        assertEquals("ERROR", result.get("result"));

    }


}


