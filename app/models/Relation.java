package models;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import play.data.validation.Required;
import play.modules.morphia.Model;

/**
 * Created by frost on 15/11/2015.
 */
@Entity
public class Relation extends Model {

    @Required
    @Reference
    public User user;

    @Required
    @Reference
    public User friend;

    @Required
    public boolean accepted;

    public Relation(User user, User friend, boolean accepted) {
        this.user = user;
        this.friend = friend;
        this.accepted = accepted;
    }

    public Relation(User user, User friend) {
        this(user, friend, false);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", user.email, friend.email);
    }
}
