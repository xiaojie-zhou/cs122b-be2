package com.github.klefstad_teaching.cs122b.movies.rest;

import com.github.klefstad_teaching.cs122b.core.result.MoviesResults;
import com.github.klefstad_teaching.cs122b.core.security.JWTManager;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.*;

import com.github.klefstad_teaching.cs122b.movies.model.Response.SearchResponse;
import com.github.klefstad_teaching.cs122b.movies.repo.MovieRepo;
import com.github.klefstad_teaching.cs122b.movies.util.Validate;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.Types;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class MovieController
{
    private final MovieRepo repo;
    private final Validate validate;

    @Autowired
    public MovieController(MovieRepo repo, Validate validate)
    {
        this.repo = repo;
        this.validate = validate;
    }

    @GetMapping("/movie/search")
    public ResponseEntity<SearchResponse> search(@AuthenticationPrincipal SignedJWT User,
                                                 @RequestParam("title") Optional<String> title,
                                                 @RequestParam("year") Optional<Integer> year,
                                                 @RequestParam("director") Optional<String> director,
                                                 @RequestParam("genre") Optional<String> genre,
                                                 @RequestParam("limit") Optional<Integer> limit,
                                                 @RequestParam("page") Optional<Integer> page,
                                                 @RequestParam("orderBy") Optional<String> OrderBy,
                                                 @RequestParam("direction") Optional<String> direction
                                                 ){
        SearchResponse searchResponse = new SearchResponse();

        boolean showAll = false;
        try {
            List<String> role = User.getJWTClaimsSet().getStringListClaim((JWTManager.CLAIM_ROLES));
            if(!role.isEmpty()){
                showAll = (role.get(0).equalsIgnoreCase("Admin") || role.get(0).equalsIgnoreCase("Employee"));
            }
        } catch (ParseException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(searchResponse);
        }
        boolean query = false;
        if(title.isPresent() || year.isPresent() || director.isPresent() || genre.isPresent()){
            query = true;
        }
        String where = "";
        if (query) {
            where += "WHERE";
            if (title.isPresent()){
                where += " m.title LIKE \"%" + title.get() + "%\" AND";
            }
            if (year.isPresent()){
                where += " m.`year` = " + year.get() + " AND";
            }
            if (director.isPresent()){
                where += " p.`name` LIKE \"%" + director.get() + "%\" AND";
            }
            if (genre.isPresent()){
                where += " g.`name` LIKE \"%" + genre.get() + "%\" AND";
            }
            where = where.substring(0, where.length()-3);

        }

        String orderby = "ORDER BY";

        if (OrderBy.isPresent()){
            if (OrderBy.get().equalsIgnoreCase("title")){
                orderby += " m.title";
            }
            else if (OrderBy.get().equalsIgnoreCase("rating")){
                orderby += " m.rating";
            }
            else if (OrderBy.get().equalsIgnoreCase("year")){
                orderby += " m.year";
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_ORDER_BY);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }
        }
        else {
            orderby += " m.title";
        }

        if(direction.isPresent()){
            if (direction.get().equalsIgnoreCase("asc")){
                orderby += " ASC, m.id ASC";
            }
            else if (direction.get().equalsIgnoreCase("desc")){
                orderby += " DESC, m.id ASC";
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_DIRECTION);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }

        }
        else {
            orderby += " ASC, m.id ASC";
        }

        if (limit.isPresent()){
            if (limit.get().equals(10)){
                orderby += " LIMIT 10";
            }
            else if (limit.get().equals(25)) {
                orderby += " LIMIT 25";
            }
            else if (limit.get().equals(50)){
                orderby += " LIMIT 50";
            }
            else if (limit.get().equals(100)){
                orderby += " LIMIT 100";
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_LIMIT);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }
        }
        else {
            orderby += " LIMIT 10";
        }

        if (page.isPresent()) {
            if (page.get() > 0) {
                if (limit.isPresent()) {
                    orderby += " OFFSET " + (limit.get() * (page.get() - 1)) + ";";
                }
                else {
                    orderby += " OFFSET " + (10 * (page.get() - 1)) + ";";
                }
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_PAGE);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }
        }
        else {
            orderby += " OFFSET 0;";
        }

        String sql = "SELECT distinct m.id, m.title, m.`year`, p.`name`, m.rating, m.backdrop_path, m.poster_path, m.hidden " +
                "FROM movies.movie AS m " +
                "JOIN movies.person AS p ON p.id = m.director_id " +
                "JOIN movies.movie_genre AS mg ON m.id = mg.movie_id " +
                "JOIN movies.genre AS g on g.id= mg.genre_id ";

        if (query){
            sql += where + orderby;
        }
        else {
            sql += orderby;
        }



        List<Movies> movies = this.repo.getTemplate().query(
                sql,
                (rs, rowNum) ->
                        new Movies()
                                .setId(rs.getLong("id"))
                                .setTitle(rs.getString("title"))
                                .setYear(rs.getInt("year"))
                                .setDirector(rs.getString("name"))
                                .setRating(rs.getDouble("rating"))
                                .setBackdropPath(rs.getString("backdrop_path"))
                                .setPosterPath(rs.getString("poster_path"))
                                .setHidden(rs.getBoolean("hidden"))
        );

        if (!showAll){
            movies = movies.stream().filter(movie -> movie.getHidden().equals(false))
                    .collect(Collectors.toList());
        }

        if (movies.isEmpty()){
            searchResponse.setResult(MoviesResults.NO_MOVIES_FOUND_WITHIN_SEARCH);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(searchResponse);
        }
        else {
            searchResponse.setMovies(movies);
            searchResponse.setResult(MoviesResults.MOVIES_FOUND_WITHIN_SEARCH);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(searchResponse);
        }
    }

    @GetMapping("/movie/search/person/{personId}")
    public ResponseEntity<SearchResponse> searchByPersonId(@AuthenticationPrincipal SignedJWT User,
                                                           @PathVariable Long personId,
                                                           @RequestParam("limit") Optional<Integer> limit,
                                                           @RequestParam("page") Optional<Integer> page,
                                                           @RequestParam("orderBy") Optional<String> OrderBy,
                                                           @RequestParam("direction") Optional<String> direction){
        SearchResponse searchResponse = new SearchResponse();

        boolean showAll = false;
        try {
            List<String> role = User.getJWTClaimsSet().getStringListClaim((JWTManager.CLAIM_ROLES));
            if(!role.isEmpty()){
                showAll = (role.get(0).equalsIgnoreCase("Admin") || role.get(0).equalsIgnoreCase("Employee"));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(searchResponse);
        }



        String orderby = " ORDER BY";

        if (OrderBy.isPresent()){
            if (OrderBy.get().equalsIgnoreCase("title")){
                orderby += " m.title";
            }
            else if (OrderBy.get().equalsIgnoreCase("rating")){
                orderby += " m.rating";
            }
            else if (OrderBy.get().equalsIgnoreCase("year")){
                orderby += " m.year";
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_ORDER_BY);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }
        }
        else {
            orderby += " m.title";
        }

        if(direction.isPresent()){
            if (direction.get().equalsIgnoreCase("asc")){
                orderby += " ASC, m.id ASC";
            }
            else if (direction.get().equalsIgnoreCase("desc")){
                orderby += " DESC, m.id ASC";
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_DIRECTION);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }

        }
        else {
            orderby += " ASC, m.id ASC";
        }

        if (limit.isPresent()){
            if (limit.get().equals(10)){
                orderby += " LIMIT 10";
            }
            else if (limit.get().equals(25)) {
                orderby += " LIMIT 25";
            }
            else if (limit.get().equals(50)){
                orderby += " LIMIT 50";
            }
            else if (limit.get().equals(100)){
                orderby += " LIMIT 100";
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_LIMIT);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }
        }
        else {
            orderby += " LIMIT 10";
        }

        if (page.isPresent()) {
            if (page.get() > 0) {
                if (limit.isPresent()) {
                    orderby += " OFFSET " + (limit.get() * (page.get() - 1)) + ";";
                }
                else {
                    orderby += " OFFSET " + (10 * (page.get() - 1)) + ";";
                }
            }
            else {
                searchResponse.setResult(MoviesResults.INVALID_PAGE);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(searchResponse);
            }
        }
        else {
            orderby += " OFFSET 0;";
        }


        String sql = "SELECT distinct m.id, m.title, m.`year`, p.`name`, m.rating, m.backdrop_path, m.poster_path, m.hidden " +
                "FROM movies.movie AS m " +
                "JOIN movies.person AS p ON p.id = m.director_id " +
                "JOIN movies.movie_person AS mp ON mp.movie_id = m.id ";


        sql += "WHERE mp.person_id = " + personId.toString() + orderby;

        List<Movies> movies = this.repo.getTemplate().query(
                sql,
                (rs, rowNum) ->
                        new Movies()
                                .setId(rs.getLong("id"))
                                .setTitle(rs.getString("title"))
                                .setYear(rs.getInt("year"))
                                .setDirector(rs.getString("name"))
                                .setRating(rs.getDouble("rating"))
                                .setBackdropPath(rs.getString("backdrop_path"))
                                .setPosterPath(rs.getString("poster_path"))
                                .setHidden(rs.getBoolean("hidden"))
        );


        if (!showAll){
            movies = movies.stream().filter(movie -> movie.getHidden().equals(false))
                    .collect(Collectors.toList());
        }

        if (movies.isEmpty()){
            searchResponse.setResult(MoviesResults.NO_MOVIES_WITH_PERSON_ID_FOUND);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(searchResponse);
        }
        else {
            searchResponse.setMovies(movies);
            searchResponse.setResult(MoviesResults.MOVIES_WITH_PERSON_ID_FOUND);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(searchResponse);
        }
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<SearchResponse> searchByMovieId(@AuthenticationPrincipal SignedJWT User,
                                                          @PathVariable Long movieId){

        SearchResponse searchResponse = new SearchResponse();

        boolean showAll = false;
        try {
            List<String> role = User.getJWTClaimsSet().getStringListClaim((JWTManager.CLAIM_ROLES));
            if(!role.isEmpty()){
                showAll = (role.get(0).equalsIgnoreCase("Admin") || role.get(0).equalsIgnoreCase("Employee"));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(searchResponse);
        }

        String sql = "SELECT distinct m.id, m.title, m.`year`, p.`name`, m.rating, m.num_votes, m.budget, m.revenue, m.overview, m.backdrop_path, m.poster_path, m.hidden " +
                "FROM movies.movie AS m " +
                "JOIN movies.person AS p ON p.id = m.director_id " +
                "JOIN movies.movie_person AS mp ON mp.movie_id = m.id ";


        sql += "WHERE m.id = :movieId ;";

        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("movieId", movieId.toString(), Types.INTEGER);

        List<MovieDetail> movieDetail = this.repo.getTemplate().query(
                sql, source,
                (rs, rowNum) ->
                        new MovieDetail()
                                .setId(rs.getLong("id"))
                                .setTitle(rs.getString("title"))
                                .setYear(rs.getInt("year"))
                                .setDirector(rs.getString("name"))
                                .setRating(rs.getDouble("rating"))
                                .setNumVotes(rs.getLong("num_votes"))
                                .setBudget(rs.getLong("budget"))
                                .setRevenue(rs.getLong("revenue"))
                                .setOverview(rs.getString("overview"))
                                .setBackdropPath(rs.getString("backdrop_path"))
                                .setPosterPath(rs.getString("poster_path"))
                                .setHidden(rs.getBoolean("hidden"))
        );


        if (!showAll){
            movieDetail = movieDetail.stream().filter(movie -> movie.getHidden().equals(false))
                    .collect(Collectors.toList());
        }

        if (movieDetail.isEmpty()){
            searchResponse.setResult(MoviesResults.NO_MOVIE_WITH_ID_FOUND);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(searchResponse);
        }
        else {
            searchResponse.setMovie(movieDetail.get(0));

            String sql_genre = "SELECT g.id, g.name " +
                    "FROM movies.movie_genre AS mg " +
                    "JOIN movies.genre AS g ON g.id = mg.genre_id " +
                    "WHERE mg.movie_id = " + movieId +
                    " ORDER BY g.name;";
            List<Genre> genres = this.repo.getTemplate().query(
                    sql_genre,
                    (rs, rowNum)-> new Genre()
                            .setId(rs.getLong("id"))
                            .setName(rs.getString("name")));
            searchResponse.setGenres(genres);


            String sql_person = "SELECT p.id, p.name " +
                    "FROM movies.movie_person AS mp " +
                    "JOIN movies.person AS p ON p.id = mp.person_id " +
                    "WHERE mp.movie_id = " + movieId +
                    " ORDER BY p.popularity DESC, p.id ASC;";
            List<Person> persons = this.repo.getTemplate().query(
                    sql_person,
                    (rs, rowNum) ->
                            new Person()
                                    .setId(rs.getLong("id"))
                                    .setName(rs.getString("name")));

            searchResponse.setPersons(persons);
            searchResponse.setResult(MoviesResults.MOVIE_WITH_ID_FOUND);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(searchResponse);
        }
    }
}
