package com.steto.jaurlib.eventbus;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by stefano on 23/12/14.
 */
public abstract class EventBusRequest {
    public final Map<String, String> paramsMap;
    public EBResponse response=null;
    public String jsonParams;

    protected EventBusRequest(Map<String, String> params) {
        this.paramsMap = params;
        jsonParams = new Gson().toJson(params);


    }

    public EventBusRequest() {
        paramsMap=null;
    }

    public EBResponse getResponse()
    {
        return response;
    }
}
