package com.example.backend.service.parser;

import com.example.backend.model.Event;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ICalendarParser {
    List<Event> parse(InputStream inputStream) throws IOException;
}