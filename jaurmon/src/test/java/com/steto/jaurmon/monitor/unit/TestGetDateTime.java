package com.steto.jaurmon.monitor.unit;

import com.steto.jaurmon.monitor.AuroraMonitor;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by stefano on 20/12/14.
 */
public class TestGetDateTime {

    @Test
    public void shouldConvertDate() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy-HH:mm");
        Date d = sdf.parse("01/01/2000-23:15");
        String date = AuroraMonitor.convertDate(d) ;

        assertEquals("20000101",date);

    }

    @Test
    public void shouldConvertTimeOfDay() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy-HH:mm");
        Date d = sdf.parse("19/12/2014-23:15");
        String time = AuroraMonitor.convertDayTime(d) ;

        assertEquals("23:15",time);

    }

}
