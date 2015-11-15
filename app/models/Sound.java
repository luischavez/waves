package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Reference;

import play.data.validation.Required;

import play.modules.morphia.Model;

/**
 * Created by frost on 15/11/2015.
 */
@Entity
public class Sound extends Model {

    @Required
    @Reference
    public User user;

    @Required
    public String name;

    @Required
    public String path;

    public Sound(User user, String name, String path) {
        this.user = user;
        this.name = name;
        this.path = path;
    }

    @Override
    public String toString() {
        return String.format("%s::%s[%s]", user.email, name, path);
    }
}
