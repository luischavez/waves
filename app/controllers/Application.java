package controllers;

import controllers.securesocial.SecureSocial;

import models.Relation;
import models.Sound;
import models.User;

import org.bson.types.ObjectId;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.libs.Files;
import play.libs.MimeTypes;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;

import securesocial.provider.SocialUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frost on 15/11/2015.
 */
@With(SecureSocial.class)
public class Application extends Controller {

    @Before
    static void commons() {
    }

    static User currentUser() {
        SocialUser currentUser = SecureSocial.getCurrentUser();
        return User.find("email", currentUser.email).first();
    }

    static User user(String email) {
        return User.find("email", email).first();
    }

    static List<Relation> relations() {
        User currentUser = currentUser();

        List<Relation> relations = new ArrayList<>();
        relations.addAll(Relation.find("user", currentUser).order("-_created").asList());
        relations.addAll(Relation.find("friend", currentUser).order("-_created").asList());

        return relations;
    }

    static Relation relation(String friendEmail) {
        User currentUser = currentUser();
        User friend = user(friendEmail);

        Relation relation = Relation.find("user, friend", currentUser, friend).first();
        if (null == relation) {
            relation = Relation.find("user, friend", friend, currentUser).first();
        }

        return relation;
    }

    static List<Sound> sounds(String email) {
        return Sound.find("user", user(email)).order("-_created").asList();
    }

    static Sound sound(String id) {
        User currentUser = currentUser();
        List<Relation> relations = relations();

        Sound sound = Sound.find("_id", new ObjectId(id)).first();

        boolean valid = false;
        if (!sound.user.email.equals(currentUser.email)) {
            for (Relation relation : relations) {
                if (sound.user.email.equals(relation.friend.email)) {
                    valid = true;
                    break;
                }
            }
        } else {
            valid = true;
        }

        return valid ? sound : null;
    }

    public static void home() {
        List<Relation> relations = relations();
        List<Sound> sounds = sounds(currentUser().email);

        int friendCount = 0;
        int soundCount = sounds.size();

        for (Relation relation : relations) {
            if (relation.accepted) {
                friendCount++;
            }
        }

        if (!relations.isEmpty()) {
            Relation relation = relations.get(0);
            if (relation.accepted) {
                renderArgs.put("relation", relation);
            }
        }

        if (!sounds.isEmpty()) {
            renderArgs.put("sound", sounds.get(0));
        }

        renderArgs.put("friendCount", friendCount);
        renderArgs.put("soundCount", soundCount);

        render();
    }

    public static void friends() {
        List<Relation> relations = relations();
        renderArgs.put("friends", relations);

        render();
    }

    public static void acceptFriend(@Required String email) {
        Relation relation = relation(email);
        User friend = user(email);

        if (null != relation && !currentUser().email.equals(email)) {
            relation.accepted = true;
            relation.save();
            flash.success("Se acepto al usuario [%s]", friend.displayName);
        } else {
            flash.error("El usuario [%s] no esta en tu lista de amigos", friend.displayName);
        }

        redirect(Router.reverse("Application.friends").url);
    }

    public static void addFriend(@Required String email) {
        if (!currentUser().email.equals(email)) {
            User friend = user(email);
            Relation relation = relation(email);
            if (null == friend) {
                flash.error("El usuario [%s] no existe!", email);
            } else {
                if (null == relation) {
                    relation = new Relation(currentUser(), friend);
                    relation.save();
                    flash.success("Se envio la solicitud al usuario [%s]", friend.displayName);
                } else {
                    flash.error("Ya existe una solicitud [%s] en espera", friend.displayName);
                }
            }
        }

        redirect(Router.reverse("Application.friends").url);
    }

    public static void removeFriend(@Required String email) {
        Relation relation = relation(email);
        User friend = user(email);

        if (null != relation && !currentUser().email.equals(email)) {
            relation.delete();
            flash.success("Se elimino al usuario [%s]", friend.displayName);
        } else {
            flash.error("El usuario [%s] no esta en tu lista de amigos", friend.displayName);
        }

        redirect(Router.reverse("Application.friends").url);
    }

    public static void files(@Required String email) {
        Relation relation = relation(email);

        if (currentUser().email.equals(email) || null != relation) {
            List<Sound> sounds = sounds(email);
            renderArgs.put("sounds", sounds);
        }

        renderArgs.put("filesOwner", user(email));

        render();
    }

    public static void uploadFile(@Required File file) throws IOException {
        User currentUser = currentUser();

        if (!Validation.hasErrors()) {
            String contentType = MimeTypes.getContentType(file.getName());
            System.out.println(contentType);
            if ("audio/mpeg3".equals(contentType)) {
                Sound sound = new Sound(currentUser, file.getName(), "");
                sound.save();

                File path = new File(
                        String.format("public/sounds/%s/", currentUser.email));
                path.mkdirs();
                File target = new File(
                        String.format("public/sounds/%s/%s.mp3", currentUser.email, sound.getId().toString()));
                target.createNewFile();
                if (!target.exists()) {
                    sound.delete();
                    flash.error("No se pudo subir el archivo [%s]", sound.name);
                } else {
                    sound.path = target.getPath();
                    sound.save();

                    Files.copy(file, target);
                    flash.success("Se subio correctamente el archivo [%s]", sound.name);
                }
                Files.delete(file);
            } else {
                flash.error("El archivo [%s] no es valido!");
            }
        }

        Map<String, Object> args = new HashMap<>();
        args.put("email", currentUser.email);

        redirect(Router.reverse("Application.files", args).url);
    }

    public static void deleteFile(@Required String id) {
        Sound sound = sound(id);
        if (null != sound) {
            Files.delete(new File(sound.path));
            sound.delete();
            flash.success("Se elimino correctamente el archivo [%s]", sound.name);
        }

        Map<String, Object> args = new HashMap<>();
        args.put("email", currentUser().email);

        redirect(Router.reverse("Application.files", args).url);
    }

    public static void streamFile(@Required String id) {
        Sound sound = sound(id);
        if (null == sound) {
            Map<String, Object> args = new HashMap<>();
            args.put("email", currentUser().email);

            redirect(Router.reverse("Application.files", args).url);
        }

        renderArgs.put("sound", sound);

        render();
    }

    public static void download(@Required String id) {
        Sound sound = sound(id);
        if (null != sound) {
            renderBinary(new File(sound.path));
        }
    }
}
