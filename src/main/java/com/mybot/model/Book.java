package com.mybot.model;

import java.util.Collections;
import java.util.List;

public class Book {
    private boolean success;// Indicates if the book was successfully retrieved
    private String title;
    private String url;
    private String coverUrl;
    private String error;
    private List<String> artists;
    private List<String> groups;
    private List<String> languages;
    private List<String> tags;
    private List<String> parodies;
    private List<String> characters;
    private String pages;

    // constructor
    public Book(String title, String url, String coverUrl,
            List<String> artists, List<String> groups, List<String> languages,
            List<String> tags, List<String> parodies, List<String> characters, String pages) {
        this.success = true;
        this.title = title;
        this.url = url;
        this.coverUrl = coverUrl;
        this.artists = (artists != null) ? artists : Collections.emptyList();
        this.groups = (groups != null) ? groups : Collections.emptyList();
        this.languages = (languages != null) ? languages : Collections.emptyList();
        this.tags = (tags != null) ? tags : Collections.emptyList();
        this.parodies = (parodies != null) ? parodies : Collections.emptyList();
        this.characters = (characters != null) ? characters : Collections.emptyList();
        this.pages = (pages != null) ? pages : "N/A";
    }

    public Book(String error) {
        this.success = false;
        this.error = error;
        this.pages = "N/A";
        this.artists = Collections.emptyList();
        this.groups = Collections.emptyList();
        this.languages = Collections.emptyList();
        this.tags = Collections.emptyList();
        this.parodies = Collections.emptyList();
        this.characters = Collections.emptyList();
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getError() {
        return error;
    }

    public List<String> getArtists() {
        return artists;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getParodies() {
        return parodies;
    }

    public List<String> getCharacters() {
        return characters;
    }

    public String getPages() {
        return pages;
    }
}
