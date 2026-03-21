package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
public class EventDTO {
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private String location;
    private String address;
    private String eventUrl;
    private String image;
    private List<String> keywords;


  
    public EventDTO() {}


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }

    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEventUrl() { return eventUrl; }
    public void setEventUrl(String eventUrl) { this.eventUrl = eventUrl; }


    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public List<String> getKeywords() { 
        return keywords; 
    }
    public void setKeywords(List<String> keywords) { this.keywords =this.keywords; }
}