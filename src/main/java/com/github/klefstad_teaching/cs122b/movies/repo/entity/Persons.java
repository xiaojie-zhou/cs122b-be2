package com.github.klefstad_teaching.cs122b.movies.repo.entity;


import com.fasterxml.jackson.annotation.JsonInclude;


public class Persons {
    private Long id;
    private String name;
    private String birthday;
    private String biography;
    private String birthplace;
    private Float popularity;
    private String profilePath;

    public Long getId() {
        return id;
    }

    public Persons setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Persons setName(String name) {
        this.name = name;
        return this;
    }

    public String getBirthday() {
        return birthday;
    }

    public Persons setBirthday(String birthday) {
        this.birthday = birthday;
        return this;
    }

    public String getBiography() {
        return biography;
    }

    public Persons setBiography(String biography) {
        this.biography = biography;
        return this;
    }

    public String getBirthplace() {
        return birthplace;
    }

    public Persons setBirthplace(String birthplace) {
        this.birthplace = birthplace;
        return this;
    }

    public Float getPopularity() {
        return popularity;
    }

    public Persons setPopularity(Float popularity) {
        this.popularity = popularity;
        return this;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public Persons setProfilePath(String profilePath) {
        this.profilePath = profilePath;
        return this;
    }
}
