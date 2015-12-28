package com.steto.jaurlib.eventbus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.steto.jaurlib.AuroraDriver;
import com.steto.jaurlib.cmd.InverterCommand;
import com.steto.jaurlib.cmd.InverterCommandFactory;
import com.steto.jaurlib.response.AResp_CumulatedEnergy;
import com.steto.jaurlib.response.AuroraResponse;
import com.steto.jaurlib.response.ResponseErrorEnum;

/**
 * Created by stefano on 28/12/15.
 */
public class EventBusAdapter {
    private final EventBus eventBus;
    private final AuroraDriver auroraDriver;
    private final InverterCommandFactory inverterCommandFactory;

    public EventBusAdapter(EventBus aEventBus, AuroraDriver aAuroraDriver,InverterCommandFactory aInverterCommandFactory) {
        eventBus = aEventBus;
        auroraDriver= aAuroraDriver;
        eventBus.register(this);
        inverterCommandFactory=aInverterCommandFactory;
    }


    @Subscribe
    @AllowConcurrentEvents
    public void handleInverterCommand(EventBusInverterRequest cmd) {
        EBResponse ebResponse;

        InverterCommand inverterCommand = inverterCommandFactory.create(cmd.opcode(), cmd.subcode(), cmd.address());

        try {
            AuroraResponse auroraResponse = inverterCommand.execute(auroraDriver);

            ebResponse = (auroraResponse.getErrorCode() == ResponseErrorEnum.NONE) ? new EBResponseOK(auroraResponse.getValue()) : new EBResponseNOK(auroraResponse.getErrorCode().get(), auroraResponse.getErrorCode().toString());

        } catch (Exception e) {
            String errorString = e.getMessage();
            ebResponse = new EBResponseNOK(-1, errorString);
        }


        cmd.response = ebResponse;

    }

}
