package org.regele.totask2.controller;


import org.regele.totask2.service.ProjectPlanData;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * gantViewer needs dates in ISO Format during JSON request.
 * 
 * @see ProjectPlanData
 */
@Component
public class JsonDateSerializer extends JsonSerializer<Date>{

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

    @Override
    public void serialize(Date value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException {
        
        String formattedDate = dateFormat.format(value);

        jgen.writeString(formattedDate);   
    }

}
