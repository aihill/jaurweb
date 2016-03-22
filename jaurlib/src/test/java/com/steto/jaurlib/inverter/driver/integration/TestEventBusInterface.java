package com.steto.jaurlib.inverter.driver.integration;

import com.google.common.eventbus.EventBus;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.eventbus.EBResponseNOK;
import com.steto.jaurlib.eventbus.EBResponseOK;
import com.steto.jaurlib.eventbus.EventBusInverterAdapter;
import com.steto.jaurlib.eventbus.EBInverterRequest;
import com.steto.jaurlib.request.AuroraCumEnergyEnum;
import com.steto.jaurlib.request.AuroraDspRequestEnum;
import com.steto.jaurlib.response.*;
import jssc.SerialPortException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by stefano on 23/12/14.
 */
public class TestEventBusInterface {

    String pvOutDirPath;
    EventBus theEventBus = new EventBus();
    AuroraDriver auroraDriver = mock(AuroraDriver.class);
    EventBusInverterAdapter eventBusInverterAdapter = new EventBusInverterAdapter(theEventBus,auroraDriver, new InverterCommandFactory());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Before
    public void before() throws IOException, InterruptedException, SerialPortException {

        pvOutDirPath = tempFolder.newFolder().getAbsolutePath();
        Thread.sleep(200);

    }

    @After
    public void after() throws Exception {

        tempFolder.delete();
        Thread.sleep(200);

    }




    @Test
    public void shouldExecuteInverterCommandEnergyCumulated() throws Exception {

        // Setup Command
        int inverterAddress = 5;
        String opcde = "cumEnergy";
        String subcode = "daily";

        // Setup
        final float expectedCumulateEnergyValue = (float) 1001.0;

        AResp_CumulatedEnergy expectedCumulateEnergyResponse = new AResp_CumulatedEnergy();
        expectedCumulateEnergyResponse.setLongParam((long) expectedCumulateEnergyValue);
        when(auroraDriver.acquireCumulatedEnergy(eq(inverterAddress), eq(AuroraCumEnergyEnum.DAILY))).thenReturn(expectedCumulateEnergyResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        float energyReadout = Float.parseFloat((String) result.data);

        assertEquals(expectedCumulateEnergyValue, energyReadout, 0.00001);


    }


    @Test
    public void shouldExecuteInverterCommandDspData() throws Exception {

        // Setup Command
        int inverterAddress = 5;
        String opcde = "dspData";
        String subcode = "gridVoltageAll";

        // Setup
        final float expectedVoltageAll = (float) 253.2;

        AResp_DspData expectedResponse = new AResp_DspData();
        expectedResponse.setFloatParam(expectedVoltageAll);
        when(auroraDriver.acquireDspValue(eq(inverterAddress), eq(AuroraDspRequestEnum.GRID_VOLTAGE_ALL))).thenReturn(expectedResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        float voltageReadout = Float.parseFloat((String) result.data);

        assertEquals(expectedVoltageAll, voltageReadout, 0.00001);


    }



    @Test
    public void shouldExecuteInverterCommandProductNumber() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "productNumber";
        String subcode = "";


        String productNumber = "012345";

        AResp_ProductNumber expectedProductNumberResponse = new AResp_ProductNumber();
        expectedProductNumberResponse.set(productNumber);
        when(auroraDriver.acquireProductNumber(eq(inverterAddress))).thenReturn(expectedProductNumberResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);


        assertEquals(productNumber, result.data);


    }

    @Test
    public void shouldExecuteInverterCommandSerialNumber() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "serialNumber";
        String subcode = "";


        String serialNumber = "654321";

        AResp_SerialNumber expectedSerialNumberResponse = new AResp_SerialNumber();
        expectedSerialNumberResponse.set(serialNumber);
        when(auroraDriver.acquireSerialNumber(eq(inverterAddress))).thenReturn(expectedSerialNumberResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);


        assertEquals(serialNumber, result.data);

    }

    @Test
    public void shouldExecuteInverterVersionNumber() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "versionNumber";
        String subcode = "";


        int versionNumber = 0xAABBCCDD;

        AResp_VersionId expectedVersionId = new AResp_VersionId();
        expectedVersionId.setLongParam(versionNumber);
        String versionDescription = expectedVersionId.getValue();
        when(auroraDriver.acquireVersionId(eq(inverterAddress))).thenReturn(expectedVersionId);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);


        assertEquals(versionDescription, result.data);

    }

    @Test
    public void shouldExecuteInverterFirmwareVersion() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "firmwareNumber";
        String subcode = "";

        AResp_FwVersion expectedFirmwareVersionResponse = new AResp_FwVersion();
        expectedFirmwareVersionResponse.set("1234");
        String firmareVersion = expectedFirmwareVersionResponse.get();
        when(auroraDriver.acquireFirmwareVersion(eq(inverterAddress))).thenReturn(expectedFirmwareVersionResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        assertEquals(firmareVersion, result.data);

    }

    @Test
    public void shouldExecuteInverterManufacturingDate() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "manufacturingDate";
        String subcode = "";

        AResp_MFGdate expectedResponse = new AResp_MFGdate();
        expectedResponse.setDate(new Date());
        String maufactoringDate = expectedResponse.getValue();
        when(auroraDriver.acquireMFGdate(eq(inverterAddress))).thenReturn(expectedResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        assertEquals(maufactoringDate, result.data);

    }



    @Test
    public void shouldExecuteSystemConfig() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "sysConfig";
        String subcode = "";

        AResp_SysConfig expectedResponse = new AResp_SysConfig();
        expectedResponse.setConfigCode(125);
        String systemConfig = expectedResponse.getValue();
        when(auroraDriver.acquireSystemConfig(eq(inverterAddress))).thenReturn(expectedResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        assertEquals(systemConfig, result.data);

    }


    @Test
    public void shouldExecuteInverterTimeCounter() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "timeCounter";
        String subcode = "";

        AResp_TimeCounter expectedResponse = new AResp_TimeCounter();
        expectedResponse.set(new Date().getTime()/1000);
        String timeCounter = expectedResponse.getValue().toString();
        when(auroraDriver.acquireTimeCounter(eq(inverterAddress))).thenReturn(expectedResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        assertEquals(timeCounter, result.data);

    }


    @Test
    public void shouldExecuteInverterActualTime() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "actualTime";
        String subcode = "";

        AResp_ActualTime expectedResponse = new AResp_ActualTime();
        expectedResponse.set(new Date().getTime()/1000);
        String timeCounter = expectedResponse.get().toString();
        when(auroraDriver.acquireActualTime(eq(inverterAddress))).thenReturn(expectedResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        assertEquals(timeCounter, result.data);

    }

    @Test
    public void should() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "lastAlarms";
        String subcode = "";

        AResp_LastAlarms expectedResponse = new AResp_LastAlarms();
        expectedResponse.setAlarm1(0);
        expectedResponse.setAlarm2(1);
        expectedResponse.setAlarm3(2);
        expectedResponse.setAlarm4(3);
        when(auroraDriver.acquireLastAlarms(eq(inverterAddress))).thenReturn(expectedResponse);

        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseOK result = (EBResponseOK) EBInverterRequest.getResponse();
        System.out.println(result);

        assertEquals(expectedResponse.toString(), result.data);

    }

    @Test
    public void shouldHandleBadCommand() throws Exception {

        // Setup Command
        int inverterAddress = 2;
        String opcde = "badcommand ";
        String subcode = "wrong";


        EBInverterRequest EBInverterRequest = new EBInverterRequest(opcde,subcode,inverterAddress);

        // exercise
        theEventBus.post(EBInverterRequest);

        //verify
        EBResponseNOK result = (EBResponseNOK) EBInverterRequest.getResponse();
        System.out.println(result);


    }


}