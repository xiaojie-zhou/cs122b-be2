package com.github.klefstad_teaching.cs122b.movies.repo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;


public class Movies {
    private Long id;
    private String title;
    private Integer year;
    private String director;
    private Double rating;
    private String backdropPath;
    private String posterPath;
    private Boolean hidden;
    public Long getId() {
        return id;
    }

    public Movies setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Movies setTitle(String title) {
        this.title = title;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public Movies setYear(Integer year) {
        this.year = year;
        return this;
    }

    public String getDirector() {
        return director;
    }

    public Movies setDirector(String director) {
        this.director = director;
        return this;
    }

    public Double getRating() {
        return rating;
    }

    public Movies setRating(Double rating) {
        this.rating = rating;
        return this;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public Movies setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
        return this;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public Movies setPosterPath(String posterPath) {
        this.posterPath = posterPath;
        return this;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public Movies setHidden(Boolean hidden) {
        this.hidden = hidden;
        return this;
    }
}
