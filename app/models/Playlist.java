package models;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import play.data.validation.Required;
import play.modules.morphia.Model;

import java.util.List;

/**
 * Modelo de las listas de reproduccion.
 *
 * @author Javier Maldonado
 */
@Entity
public class Playlist extends Model {

    @Required
    public String name;

    @Required
    @Reference
    public User owner;

    @Reference
    public List<Sound> sounds;

    public Playlist(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return String.format("Lista de reproduccion [%s] de %s", name, owner.toString());
    }
}
