package io.thundra.merloc.todo.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

/**
 * @author serkan
 */
@Getter
@Builder
@ToString
public class Todo {

    private final String id;
    private final String title;
    private final Date time;
    private final Boolean completed;

}
