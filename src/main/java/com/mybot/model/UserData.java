package com.mybot.model;

import java.util.HashSet;
import java.util.Set;

// data class to store user data
public class UserData {
    private String userId; // Discord User ID
    private Set<String> savedBooks; // set of saved book IDs (set no duplicates)
    private Set<String> savedTags; // set of saved tags
    private Set<String> blockedBooks; // set of blocked book IDs
    private Set<String> blockedTags; // set of blocked tags
    // empty constructor for database use

    public UserData() {
        this.savedBooks = new HashSet<>();
        this.savedTags = new HashSet<>();
        this.blockedBooks = new HashSet<>();
        this.blockedTags = new HashSet<>();
    }

    // constructor with userId
    public UserData(String userId) {
        this.userId = userId;
        this.savedBooks = new HashSet<>();
        this.savedTags = new HashSet<>();
        this.blockedBooks = new HashSet<>();
        this.blockedTags = new HashSet<>();
    }

    // --- Getters & Setters ---
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getSavedBooks() {
        return savedBooks;
    }

    public void setSavedBooks(Set<String> savedBooks) {
        this.savedBooks = savedBooks;
    }

    public Set<String> getSavedTags() {
        return savedTags;
    }

    public void setSavedTags(Set<String> savedTags) {
        this.savedTags = savedTags;
    }

    public Set<String> getBlockedBooks() {
        return blockedBooks;
    }

    public void setBlockedBooks(Set<String> blockedBooks) {
        this.blockedBooks = blockedBooks;
    }

    public Set<String> getBlockedTags() {
        return blockedTags;
    }

    public void setBlockedTags(Set<String> blockedTags) {
        this.blockedTags = blockedTags;
    }

    // --- Helper Methods ---
    // tag
    public void addSavedTag(String tag) {
        this.savedTags.add(tag);
    }

    public void removeSavedTag(String tag) {
        this.savedTags.remove(tag);
    }

    public void addBlockedTag(String tag) {
        this.blockedTags.add(tag);
    }

    public void removeBlockedTag(String tag) {
        this.blockedTags.remove(tag);
    }

    // book
    public void addSavedBook(String bookId) {
        this.savedBooks.add(bookId);
    }

    public void removeSavedBook(String bookId) {
        this.savedBooks.remove(bookId);
    }

    public void addBlockedBook(String bookId) {
        this.blockedBooks.add(bookId);
    }

    public void removeBlockedBook(String bookId) {
        this.blockedBooks.remove(bookId);
    }

    // --- Check Methods ---
    public boolean isBookSaved(String bookId) {
        return this.savedBooks.contains(bookId);
    }

    public boolean isBookBlocked(String bookId) {
        return this.blockedBooks.contains(bookId);
    }

    public boolean isTagSaved(String tag) {
        return this.savedTags.contains(tag);
    }

    public boolean isTagBlocked(String tag) {
        return this.blockedTags.contains(tag);
    }
}