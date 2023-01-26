package com.github.klefstad_teaching.cs122b.movies.model.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.klefstad_teaching.cs122b.core.result.Result;

import com.github.klefstad_teaching.cs122b.movies.repo.entity.Persons;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonResponse {
    private com.github.klefstad_teaching.cs122b.core.result.Result result;
    private List<Persons> persons;

    private Persons person;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public List<Persons> getPersons() {
        return persons;
    }
    public void setPersons(List<Persons> persons) {
        this.persons = persons;
    }

    public Persons getPerson() {
        return person;
    }

    public void setPerson(Persons person) {
        this.person = person;
    }
}
