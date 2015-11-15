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

import securesocial.provider.SocialUser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frost on 15/11/2015.
 */
public class Application extends Controller {

    @Before
    static void checkLogin() {
        if (!SecureSocial.isUserLoggedIn()) {
            redirect(Router.reverse("securesocial.SecureSocial.login").url);
        }
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
        return Relation.find("user", currentUser).asList();
    }

    static Relation relation(String email) {
        User currentUser = currentUser();
        User friend = user(email);

        return Relation.find("user, friend", currentUser, friend).first();
    }

    static List<Sound> sounds(String email) {
        return Sound.find("user", user(email)).asList();
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

    public static void friends() {
        List<Relation> relations = relations();
        renderArgs.put("friends", relations);

        render();
    }

    public static void acceptFriend(@Required String email) {
        Relation relation = relation(email);
        if (null != relation) {
            relation.accepted = true;
            relation.save();
        }

        redirect(Router.reverse("Application.friends").url);
    }

    public static void addFriend(@Required String email) {
        Relation relation = relation(email);
        if (null == relation) {
            relation = new Relation(currentUser(), user(email));
            relation.save();
        }

        redirect(Router.reverse("Application.friends").url);
    }

    public static void removeFriend(@Required String email) {
        Relation relation = relation(email);
        if (null != relation) {
            relation.delete();
        }

        redirect(Router.reverse("Application.friends").url);
    }

    public static void uploadForm() {
        render();
    }

    public static void listFiles(@Required String email) {
        Relation relation = relation(email);

        if (currentUser().email.equals(email)
                || (null != relation && relation.friend.email.equals(email))) {
            List<Sound> sounds = sounds(email);
            renderArgs.put("sounds", sounds);
        }

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
                } else {
                    sound.path = target.getPath();
                    sound.save();

                    Files.copy(file, target);
                    Files.delete(file);
                }
            }
        }

        Map<String, Object> args = new HashMap<>();
        args.put("email", currentUser.email);

        redirect(Router.reverse("Application.listFiles", args).url);
    }

    public static void deleteFile(@Required String id) {
        Sound sound = sound(id);
        if (null != sound) {
            Files.delete(new File(sound.path));
            sound.delete();
        }

        Map<String, Object> args = new HashMap<>();
        args.put("email", currentUser().email);

        redirect(Router.reverse("Application.listFiles", args).url);
    }

    public static void streamFile(@Required String id) {
        Sound sound = sound(id);
        if (null == sound) {
            Map<String, Object> args = new HashMap<>();
            args.put("email", currentUser().email);

            redirect(Router.reverse("Application.listFiles", args).url);
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
