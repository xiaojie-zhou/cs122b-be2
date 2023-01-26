package com.github.klefstad_teaching.cs122b.movies.model.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.klefstad_teaching.cs122b.core.result.Result;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.*;


import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private com.github.klefstad_teaching.cs122b.core.result.Result result;
    private List<Movies> movies;

    private List<Person> persons;

    private MovieDetail movie;

    private List<Genre> genres;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public List<Movies> getMovies() {
        return movies;
    }

    public void setMovies(List<Movies> movies) {
        this.movies = movies;
    }

    public MovieDetail getMovie() {
        return movie;
    }

    public void setMovie(MovieDetail movieDetail) {
        this.movie = movieDetail;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }
}
