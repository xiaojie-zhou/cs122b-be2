package com.github.klefstad_teaching.cs122b.movies.rest;

import com.github.klefstad_teaching.cs122b.core.result.MoviesResults;
import com.github.klefstad_teaching.cs122b.core.security.JWTManager;
import com.github.klefstad_teaching.cs122b.movies.model.Response.PersonResponse;
import com.github.klefstad_teaching.cs122b.movies.model.Response.SearchResponse;
import com.github.klefstad_teaching.cs122b.movies.repo.MovieRepo;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.Movies;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.Persons;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Types;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class PersonController
{
    private final MovieRepo repo;

    @Autowired
    public PersonController(MovieRepo repo)
    {
        this.repo = repo;
    }


    @GetMapping("/person/search")
    public ResponseEntity<PersonResponse> personSearch(@AuthenticationPrincipal SignedJWT User,
                                                       @RequestParam("name") Optional<String> name,
                                                       @RequestParam("birthday") Optional<String> birthday,
                                                       @RequestParam("movieTitle") Optional<String> movieTitle,
                                                       @RequestParam("limit") Optional<Integer> limit,
                                                       @RequestParam("page") Optional<Integer> page,
                                                       @RequestParam("orderBy") Optional<String> OrderBy,
                                                       @RequestParam("direction") Optional<String> direction

    ){
        PersonResponse personResponse = new PersonResponse();

        boolean showAll = false;
        try {
            List<String> role = User.getJWTClaimsSet().getStringListClaim((JWTManager.CLAIM_ROLES));
            showAll = (role.get(0).equalsIgnoreCase("Admin") || role.get(0).equalsIgnoreCase("Employee"));
        } catch (ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(personResponse);
        }

        boolean query = name.isPresent() || birthday.isPresent() || movieTitle.isPresent();
        String where = "WHERE";
        if (movieTitle.isPresent()){
            where = "JOIN movies.movie_person AS mp ON p.id = mp.person_id JOIN movies.movie AS m ON m.id = mp.movie_id " + where;
            where += " m.title LIKE '%" + movieTitle.get() + "%' AND";
        }

        if (name.isPresent()){
            where += " p.name LIKE '%" + name.get() + "%' AND";
        }
        if (birthday.isPresent()){
            where += " p.birthday = '" + birthday.get() + "' AND";
        }


        where = where.substring(0, where.length()-3);

        String orderby = "ORDER BY";

        if (OrderBy.isPresent()){
            if (OrderBy.get().equalsIgnoreCase("name")){
                orderby += " p.name";
            }
            else if (OrderBy.get().equalsIgnoreCase("popularity")){
                orderby += " p.popularity";
            }
            else if (OrderBy.get().equalsIgnoreCase("birthday")){
                orderby += " p.birthday";
            }
            else {
                personResponse.setResult(MoviesResults.INVALID_ORDER_BY);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(personResponse);
            }
        }
        else {
            orderby += " p.`name`";
        }

        if(direction.isPresent()){
            if (direction.get().equalsIgnoreCase("asc")){
                orderby += " ASC, p.id ASC";
            }
            else if (direction.get().equalsIgnoreCase("desc")){
                orderby += " DESC, p.id ASC";
            }
            else {
                personResponse.setResult(MoviesResults.INVALID_DIRECTION);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(personResponse);
            }

        }
        else {
            orderby += " ASC, p.id ASC";
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
                personResponse.setResult(MoviesResults.INVALID_LIMIT);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(personResponse);
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
                personResponse.setResult(MoviesResults.INVALID_PAGE);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(personResponse);
            }
        }
        else {
            orderby += " OFFSET 0;";
        }

        String sql = "SELECT distinct p.id, p.name, p.birthday, p.biography, p.birthplace, p.popularity, p.profile_path " +
                "FROM movies.person AS p ";

        if (query){
            sql += where + orderby;
        }
        else {
            sql += orderby;
        }

        System.out.println(sql);


        List<Persons> persons = this.repo.getTemplate().query(
                sql,
                (rs, rowNum) ->
                        new Persons()
                                .setId(rs.getLong("id"))
                                .setName(rs.getString("name"))
                                .setBirthday(rs.getString("birthday"))
                                .setBirthplace(rs.getString("birthplace"))
                                .setBiography(rs.getString("biography"))
                                .setPopularity(rs.getFloat("popularity"))
                                .setProfilePath(rs.getString("profile_path"))
        );
        if (persons.isEmpty()){
            personResponse.setResult(MoviesResults.NO_PERSONS_FOUND_WITHIN_SEARCH);
        }
        else {
            personResponse.setPersons(persons);
            personResponse.setResult(MoviesResults.PERSONS_FOUND_WITHIN_SEARCH);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(personResponse);
    }

    @GetMapping("/person/{personId}")
    public ResponseEntity<PersonResponse> personIdSearch(@AuthenticationPrincipal SignedJWT User,
                                                         @PathVariable Long personId){
        PersonResponse personResponse = new PersonResponse();
        boolean showAll = false;
        try {
            List<String> role = User.getJWTClaimsSet().getStringListClaim((JWTManager.CLAIM_ROLES));
            showAll = (role.get(0).equalsIgnoreCase("Admin") || role.get(0).equalsIgnoreCase("Employee"));
        } catch (ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(personResponse);
        }

        String sql = "SELECT distinct p.id, p.name, p.birthday, p.biography, p.birthplace, p.popularity, p.profile_path " +
                "FROM movies.person AS p " +
                "WHERE p.id = :personId";

        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("personId", personId.toString(), Types.INTEGER);

        List<Persons> persons = this.repo.getTemplate().query(
                sql, source,
                (rs, rowNum) ->
                        new Persons()
                                .setId(rs.getLong("id"))
                                .setName(rs.getString("name"))
                                .setBirthday(rs.getString("birthday"))
                                .setBirthplace(rs.getString("birthplace"))
                                .setBiography(rs.getString("biography"))
                                .setPopularity(rs.getFloat("popularity"))
                                .setProfilePath(rs.getString("profile_path"))
        );

        if (persons.isEmpty()){
            personResponse.setResult(MoviesResults.NO_PERSON_WITH_ID_FOUND);
        }
        else{
            personResponse.setResult(MoviesResults.PERSON_WITH_ID_FOUND);
            personResponse.setPerson(persons.get(0));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(personResponse);

    }


}
