package models;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import play.data.validation.Required;
import play.modules.morphia.Model;

/**
 * Modelo de las canciones.
 *
 * @author Javier Maldonado
 */
@Entity
public class Sound extends Model {

    @Required
    @Reference
    public User owner;

    @Required
    public String name;

    @Required
    public String path;

    public Sound(User owner, String name, String path) {
        this.owner = owner;
        this.name = name;
        this.path = path;
    }

    @Override
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }

        if (other instanceof Sound) {
            return Sound.class.cast(other).getId().equals(getId());
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("%s::%s[%s]", owner.email, name, path);
    }
}
