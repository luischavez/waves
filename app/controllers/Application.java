package controllers;

import controllers.securesocial.SecureSocial;

import models.Playlist;
import models.Relation;
import models.Sound;
import models.User;

import org.bson.types.ObjectId;

import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.libs.Files;
import play.libs.MimeTypes;
import play.modules.morphia.Model;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;

import securesocial.provider.SocialUser;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
        return Sound.find("owner", user(email)).order("-_created").asList();
    }

    /**
     * Obtiene la cancion con el identificador especificado.
     *
     * @param soundId identificador
     * @return cancion o null si no existe.
     */
    static Sound sound(String soundId) {
        User currentUser = currentUser();
        List<Relation> relations = relations();

        Sound sound = Sound.find("_id", new ObjectId(soundId)).first();

        if (null != sound && currentUser.email.equals(sound.owner.email)) {
            return sound;
        }

        boolean valid = false;
        if (null != sound) {
            for (Relation relation : relations) {
                if (relation.friend.email.equals(sound.owner.email)
                        || relation.user.email.equals(sound.owner.email)) {
                    valid = true;
                    break;
                }
            }
        }

        return valid ? sound : null;
    }

    /**
     * Obtine las listas de reproduccion de un usuario especifico.
     *
     * @param email correo del ususario.
     * @return listas de reproduccion.
     */
    static List<Playlist> lists(String email) {
        return Playlist.find("owner", user(email)).order("-_created").asList();
    }

    /**
     * Obtiene la lista de reproduccion con el identificador especificado.
     *
     * @param playlistId identificador
     * @return lista de reproduccion o null si no existe.
     */
    static Playlist list(String playlistId) {
        User currentUser = currentUser();
        List<Relation> relations = relations();

        Playlist playlist = Playlist.find("_id", new ObjectId(playlistId)).first();

        if (null != playlist && currentUser.email.equals(playlist.owner.email)) {
            return playlist;
        }

        boolean valid = false;
        if (null != playlist) {
            for (Relation relation : relations) {
                if (relation.friend.email.equals(playlist.owner.email)
                        || relation.user.email.equals(playlist.owner.email)) {
                    valid = true;
                    break;
                }
            }
        }

        return valid ? playlist : null;
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
            List<Sound> sounds = sounds(email);
            renderArgs.put("sounds", sounds);
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
                flash.error("El archivo [%s] no es valido!", file.getName());
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
     * @param soundId identificador
     */
    public static void deleteFile(@Required String soundId) {
        Sound sound = sound(soundId);
        if (null != sound) {
            List<Playlist> playlists = Playlist.q().filter("sounds in", sound).asList();
            for (Playlist playlist : playlists) {
                playlist.sounds.remove(sound);
                playlist.save();
            }

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
     * @param soundId identificador
     */
    public static void streamFile(@Required String soundId) {
        Sound sound = sound(soundId);
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
     * @param soundId identificador.
     */
    public static void download(@Required String soundId) {
        Sound sound = sound(soundId);
        if (null != sound) {
            renderBinary(new File(sound.path), sound.name);
        }
    }

    /**
     * Accion que muestra las listas de reproduccion del usuario especificado.
     *
     * Solo se mostraran si el usuario es valido.
     *
     * @param email correo del usuario.
     */
    public static void playlists(@Required String email) {
        Relation relation = relation(email);

        if (null == relation && !currentUser().email.equals(email)) {
            redirect(Router.reverse("Application.home").url);
        } else {
            List<Playlist> playlists = lists(email);
            renderArgs.put("playlists", playlists);
            renderArgs.put("listsOwner", user(email));

            render();
        }
    }

    /**
     * Accion que muestra la lista de reproduccion con el identificador especificado.
     *
     * @param playlistId identificador.
     */
    public static void playlist(@Required String playlistId) {
        Playlist playlist = list(playlistId);
        if (null == playlist) {
            Map<String, Object> args = new HashMap<>();
            args.put("email", currentUser().email);

            redirect(Router.reverse("Application.playlists", args).url);
        } else {
            renderArgs.put("playlist", playlist);

            render();
        }
    }

    /**
     * Accion que crea una nueva lista de reproduccion con el nombre especificado.
     *
     * @param name nombre de la lista de reproduccion.
     */
    public static void createPlaylist(@Required @MinSize(5) @MaxSize(20) String name) {
        if (Validation.hasErrors()) {
            flash.error("No se pudo crear la lista de reproduccion [%s], el nombre debe de contener entre 5 y 20 caracteres", name);
        } else {
            Playlist playlist = new Playlist(name, currentUser());
            playlist.save();
            flash.success("Se creo correctamente la lista de reproduccion [%s]", name);
        }

        Map<String, Object> args = new HashMap<>();
        args.put("email", currentUser().email);

        redirect(Router.reverse("Application.playlists", args).url);
    }

    /**
     * Accion que crea elimina una lista de reproduccion con el identificador especificado.
     *
     * @param playlistId identificador de la lista de reproduccion.
     */
    public static void deletePlaylist(@Required String playlistId) {
        Playlist playlist = list(playlistId);

        if (null != playlist && playlist.owner.email.equals(currentUser().email)) {
            playlist.delete();
            flash.success("Se elimino correctamente  la lista de reproduccion [%s]", playlist.name);
        }

        Map<String, Object> args = new HashMap<>();
        args.put("email", currentUser().email);

        redirect(Router.reverse("Application.playlists", args).url);
    }

    /**
     * Accion que agrega una cancion a una lista de reproduccion.
     *
     * Solo se agregara si la lista y la cancion existen y son validas.
     *
     * @param playlistId identificador de la lista de reproduccion.
     * @param soundId identificador de la cancion.
     */
    public static void addToPlaylist(@Required String playlistId, @Required String soundId) {
        Playlist playlist = list(playlistId);
        Sound sound = sound(soundId);

        if (null == playlist || null == sound) {
            redirect(Router.reverse("Application.home").url);
        } else {
            if (playlist.owner.email.equals(currentUser().email)
                    && sound.owner.email.equals(currentUser().email)) {
                playlist.sounds.add(sound);
                playlist.save();
                flash.success("Se agrego correctamente la cancion [%s] a la lista de reproduccion [%s]", sound.name, playlist.name);
            }
            Map<String, Object> args = new HashMap<>();
            args.put("playlistId", playlist.getId());

            redirect(Router.reverse("Application.playlist", args).url);
        }
    }

    /**
     * Accion que elimina una cancion de una lista de reproduccion.
     *
     * Solo se eliminara si la lista y la cancion existen y son validas.
     *
     * @param playlistId identificador de la lista de reproduccion.
     * @param soundId identificador de la cancion.
     */
    public static void removeFromPlaylist(@Required String playlistId, @Required String soundId) {
        Playlist playlist = list(playlistId);
        Sound sound = sound(soundId);

        if (null == playlist || null == sound) {
            redirect(Router.reverse("Application.home").url);
        } else {
            if (playlist.owner.email.equals(currentUser().email)
                    && sound.owner.email.equals(currentUser().email)) {
                playlist.sounds.remove(sound);
                playlist.save();
                flash.success("Se elimino correctamente la cancion [%s] de la lista de reproduccion [%s]", sound.name, playlist.name);
            }
            Map<String, Object> args = new HashMap<>();
            args.put("playlistId", playlist.getId());

            redirect(Router.reverse("Application.playlist", args).url);
        }
    }

    /**
     * Accion ajax que realiza una busqueda de archivos.
     *
     * @param name nombre del archivo a buscar.
     */
    public static void searchFile(String playlistId, String name) {
        Playlist playlist = list(playlistId);
        List<Sound> sounds = sounds(currentUser().email);

        List<Search> searches = new ArrayList<>();
        if (null != playlist) {
            for (Sound sound : sounds) {
                if (!playlist.sounds.contains(sound)) {
                    searches.add(new Search(sound.getId().toString(), sound.name));
                }
            }
        }

        renderJSON(searches);
    }

    public static class Search implements Serializable {

        public String id;

        public String name;

        public Search(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
