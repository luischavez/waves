package models;

import org.mongodb.morphia.annotations.Entity;

import play.modules.morphia.Model;

import securesocial.provider.UserId;

import java.util.Date;

/**
 * Modelo para el usuario.
 */
@Entity
public class User extends Model {

    public UserId id;

    public String displayName;

    public String email;

    public String avatarUrl;

    public Date lastAccess;

    public String password;

    public boolean isEmailVerified;

    @Override
    public String toString() {
        return email;
    }
}
