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
 * Controlador encargado de manejar toda la funcionalidad de la aplicacion.
 *
 * @author Luis Chávez
 */
@With(SecureSocial.class)
public class Application extends Controller {

    // Carpeta donde se almacenara la musica.
    public static final String SOUND_PATH = "sounds/";

    @Before
    static void commons() {
    }

    /**
     * Obtiene al usuario actual.
     *
     * @return usuario actual.
     */
    static User currentUser() {
        SocialUser currentUser = SecureSocial.getCurrentUser();
        return User.find("email", currentUser.email).first();
    }

    /**
     * Obtiene al usuario al que pertenece el correo especificado.
     *
     * @param email correo
     * @return usuario o null si el correo no esta registrado.
     */
    static User user(String email) {
        return User.find("email", email).first();
    }

    /**
     * Obtiene la lista de relaciones del usuario actual.
     *
     * @return lista de relaciones.
     */
    static List<Relation> relations() {
        User currentUser = currentUser();

        List<Relation> relations = new ArrayList<>();
        relations.addAll(Relation.find("user", currentUser).order("-_created").asList());
        relations.addAll(Relation.find("friend", currentUser).order("-_created").asList());

        return relations;
    }

    /**
     * Obtiene la relacion entre el usuario actual y el usuario con el correo especificado.
     *
     * @param friendEmail correo del usuario a buscar.
     * @return relacion o null si no existe.
     */
    static Relation relation(String friendEmail) {
        User currentUser = currentUser();
        User friend = user(friendEmail);

        Relation relation = Relation.find("user, friend", currentUser, friend).first();
        if (null == relation) {
            relation = Relation.find("user, friend", friend, currentUser).first();
        }

        return relation;
    }

    /**
     * Obtiene la lista de musica del usuario con el correo especificado.
     *
     * @param email correo
     * @return lista de musica.
     */
    static List<Sound> sounds(String email) {
        return Sound.find("user", user(email)).order("-_created").asList();
    }

    /**
     * Obtiene la cancion con el identificador especificado.
     *
     * @param id identificador
     * @return cancion o null si no existe.
     */
    static Sound sound(String id) {
        User currentUser = currentUser();
        List<Relation> relations = relations();

        Sound sound = Sound.find("_id", new ObjectId(id)).first();

        boolean valid = false;
        if (null != sound && !sound.user.email.equals(currentUser.email)) {
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

    /**
     * Accion que muestra la pantalla de bienvenida.
     *
     * Carga las siguientes variables:
     * - relation: ultima relacion del usuario actual.
     * - sound: ultima cancion del usuario actual.
     * - friendCount: total de amigos del usuario actual.
     * - soundCount: total de canciones del usuario actual.
     */
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

    /**
     * Accion que muestra la lista de amigos del usuario.
     *
     * Carga las siguientes variables:
     * - friends: lista de amigos del usuario actual.
     */
    public static void friends() {
        List<Relation> relations = relations();
        renderArgs.put("friends", relations);

        render();
    }

    /**
     * Accion para aceptar una peticion de amistad.
     *
     * Solo se acepta si la relacion existe.
     *
     * @param email correo de la persona a aceptar.
     */
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

    /**
     * Accion para enviar una peticion de amistad.
     *
     * Solo se envia si el correo es valido y no existe una peticion anterior para el mismo usuario.
     *
     * @param email correo de la persona a la que se le enviara la peticion.
     */
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

    /**
     * Accion para eliminar a un amigo.
     *
     * Solo se eliminara si el correo es valido y existe la relacion.
     *
     * @param email correo de la persona a eliminar.
     */
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

    /**
     * Accion para mostrar la lista de canciones del usuario.
     *
     * Solo se mostrara si el correo es valido.
     *
     * @param email correo del usuario.
     */
    public static void files(@Required String email) {
        Relation relation = relation(email);

        if (null == relation && !currentUser().email.equals(email)) {
            redirect(Router.reverse("Application.home").url);
        } else {
            if (currentUser().email.equals(email)) {
                List<Sound> sounds = sounds(email);
                renderArgs.put("sounds", sounds);
            }

            renderArgs.put("filesOwner", user(email));

            render();
        }
    }

    /**
     * Accion para subir una cancion a la cuenta del usuario.
     *
     * Solo se aceptan canciones en formato mp3.
     *
     * @param file cancion a subir.
     * @throws IOException si ocurre una error.
     */
    public static void uploadFile(@Required File file) throws IOException {
        User currentUser = currentUser();

        if (!Validation.hasErrors()) {
            String contentType = MimeTypes.getContentType(file.getName());
            if ("audio/mpeg3".equals(contentType)) {
                Sound sound = new Sound(currentUser, file.getName(), "");
                sound.save();

                File path = new File(
                        String.format("%s/%s/", SOUND_PATH, currentUser.getId()));
                path.mkdirs();
                File target = new File(
                        String.format("%s/%s/%s.mp3", SOUND_PATH, currentUser.getId(), sound.getId().toString()));
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

    /**
     * Accion para eliminar una cancion con el identificador especificado.
     *
     * Solo se eliminara si la cancion existe.
     *
     * @param id identificador
     */
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

    /**
     * Accion que muestra la pagina de la cancion con el identificador especificado.
     *
     * Solo se mostrara si la cancion existe.
     *
     * @param id identificador
     */
    public static void streamFile(@Required String id) {
        Sound sound = sound(id);
        if (null == sound) {
            Map<String, Object> args = new HashMap<>();
            args.put("email", currentUser().email);

            redirect(Router.reverse("Application.files", args).url);
        } else {
            renderArgs.put("sound", sound);

            render();
        }
    }

    /**
     * Accion para descargar la cancion con el identificador especificado.
     *
     * Solo se decargara si la cancion existe.
     *
     * @param id identificador.
     */
    public static void download(@Required String id) {
        Sound sound = sound(id);
        if (null != sound) {
            renderBinary(new File(sound.path));
        }
    }
}
