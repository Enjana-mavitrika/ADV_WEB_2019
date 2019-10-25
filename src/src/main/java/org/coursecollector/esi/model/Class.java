package org.coursecollector.esi.model;

import javax.persistence.OneToMany;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author Solofo RABONARIJAONA
 */
@Data
@Entity
public class Class {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;

    String name;

    int level;

    @OneToMany
    List<Option> options = new ArrayList<>();

    @OneToMany
    List<Subject> subjects = new ArrayList<>();

    public Class(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public Class(String name, int level, List<Option> options) {
        this(name, level);
        this.options = options;
    }

    public Class(String name, int level, List<Option> options, List<Subject> subjects) {
        this(name, level, options);
        this.subjects = subjects;
    }

    public Class() {

    }

}
