package com.cookify.recipe.filter;

import com.cookify.recipe.domain.Course;

import java.util.List;

/** Food type, e.g. appetizer, bread, dessert. */
public class CourseFilter extends AnyOfFilter<Course> {

    public CourseFilter(List<Course> courses) {
        super("course", courses);
    }
}
