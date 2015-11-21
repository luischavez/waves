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
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;

import securesocial.provider.SocialUser;

import waves.Drive;

import java.io.ByteArrayInputStream;
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

    // Indica el numero maximo de amigos a mostrar en el inicio.
    public static final int MAX_RECENT_FRIENDS = 5;

    // Indica el numero maximo de musica a mostrar en el inicio.
    public static final int MAX_RECENT_SOUNDS = 5;

    /**
     * Clase encargada de facilitar el acceso a los datos.
     */
    static class Data {

        /**
         * Obtiene al usuario al que pertenece el correo especificado.
         *
         * @param email correo
         * @return usuario o null si el correo no esta registrado.
         */
        static User user(String email) {
            SocialUser currentUser = SecureSocial.getCurrentUser();
            return User.find("email", email).first();
        }

        /**
         * Obtiene al usuario actual.
         *
         * @return usuario actual.
         */
        static User currentUser() {
            return user(SecureSocial.getCurrentUser().email);
        }

        /**
         * Obtiene la lista de relaciones del usuario especificado.
         *
         * @param user usuario
         * @return lista de relaciones.
         */
        static List<Relation> relations(User user) {
            User currentUser = currentUser();

            List<Relation> relations = new ArrayList<>();
            relations.addAll(Relation.find("user", currentUser).order("-_created").asList());
            relations.addAll(Relation.find("friend", currentUser).order("-_created").asList());

            return relations;
        }

        /**
         * Obtiene la relacion entre el usuario actual y el usuario con el correo especificado.
         *
         * @param friend amigo a buscar.
         * @return relacion o null si no existe.
         */
        static Relation relation(User friend) {
            User currentUser = currentUser();

            Relation relation = Relation.find("user, friend", currentUser, friend).first();
            if (null == relation) {
                relation = Relation.find("user, friend", friend, currentUser).first();
            }

            return relation;
        }

        /**
         * Verifica si el usuario es el usuario actual.
         *
         * @param user usuario.
         * @return true si es el usuario actual, false de otra manera.
         */
        static boolean isCurrentUser(User user) {
            return currentUser().email.equals(user.email);
        }

        /**
         * Verifica si el usuario actual tiene como amigo al usuario especificado.
         *
         * @param user amigo a buscar.
         * @return true si son amigos, false de otra manera.
         */
        static boolean isFriend(User user) {
            return null != relation(user);
        }

        /**
         * Obtiene la lista de musica del usuario con el correo especificado.
         *
         * @param user usuario
         * @return lista de musica.
         */
        static List<Sound> sounds(User user) {
            return Sound.find("owner", user).order("-_created").asList();
        }

        /**
         * Obtiene la lista con la musica del usuario actual y todos sus amigos.
         *
         * @return lista de musica.
         */
        static List<Sound> soundsAndFriendSounds() {
            List<Sound> sounds = new ArrayList<>();

            sounds.addAll(sounds(currentUser()));

            List<Relation> relations = relations(currentUser());
            for (Relation relation : relations) {
                if (relation.accepted) {
                    if (isCurrentUser(relation.user)) {
                        sounds.addAll(sounds(relation.friend));
                    } else {
                        sounds.addAll(sounds(relation.user));
                    }
                }
            }

            return sounds;
        }

        /**
         * Obtiene la cancion con el identificador especificado.
         *
         * @param soundId identificador
         * @return cancion o null si no existe o no se tiene permiso para reproducirla.
         */
        static Sound sound(String soundId) {
            Sound sound = Sound.find("_id", new ObjectId(soundId)).first();

            return sound;
        }

        /**
         * Obtine las listas de reproduccion de un usuario especifico.
         *
         * @param user usuario
         * @return listas de reproduccion.
         */
        static List<Playlist> lists(User user) {
            return Playlist.find("owner", user).order("-_created").asList();
        }

        /**
         * Obtiene la lista de reproduccion con el identificador especificado.
         *
         * @param playlistId identificador
         * @return lista de reproduccion o null si no existe o no se tiene permiso para acceder a ella.
         */
        static Playlist list(String playlistId) {
            Playlist playlist = Playlist.find("_id", new ObjectId(playlistId)).first();

            return playlist;
        }

        /**
         * Total de amigos del usuario especificado.
         *
         * @param user usuario
         * @return total de amigos.
         */
        static int friendCount(User user) {
            List<Relation> relations = Data.relations(user);

            int friendCount = 0;

            for (Relation relation : relations) {
                if (relation.accepted) {
                    friendCount++;
                }
            }

            return friendCount;
        }

        /**
         * Total de canciones del usuario especificado.
         *
         * @param user usuario
         * @return total de canciones.
         */
        static int soundCount(User user) {
            List<Sound> sounds = Data.sounds(user);

            return sounds.size();
        }

        /**
         * Verifica si el usuario actual es el propietario de la cancion
         * epecificada.
         *
         * @param sound cancion
         * @return true si es el propietario, false de otra manera.
         */
        static boolean isOwner(Sound sound) {
            return sound.owner.email.equals(currentUser().email);
        }

        /**
         * Verifica si la cancion especificada pertenece a algun amigo del
         * usuario actual.
         *
         * @param sound cancion
         * @return true si le pertenece a algun amigo, false de otra manera.
         */
        static boolean belongsToFriend(Sound sound) {
            boolean valid = false;

            for (Relation relation : relations(currentUser())) {
                if (relation.friend.email.equals(sound.owner.email)
                        || relation.user.email.equals(sound.owner.email)) {
                    valid = true;
                    break;
                }
            }

            return valid;
        }

        /**
         * Verifica si el usuario actual es el propietario de la lista
         * de reproduccion especificada.
         *
         * @param playlist lista de reproduccion
         * @return true si es el propietario, false de otra manera.
         */
        static boolean isOwner(Playlist playlist) {
            return playlist.owner.email.equals(currentUser().email);
        }

        /**
         * Verifica si la lista de reproduccion especificada pertenece
         * a algun amigo del usuario actual.
         *
         * @param playlist cancion
         * @return true si le pertenece a algun amigo, false de otra manera.
         */
        static boolean belongsToFriend(Playlist playlist) {
            boolean valid = false;

            for (Relation relation : relations(currentUser())) {
                if (relation.friend.email.equals(playlist.owner.email)
                        || relation.user.email.equals(playlist.owner.email)) {
                    valid = true;
                    break;
                }
            }

            return valid;
        }
    }

    /**
     * Clase encargada de manejar los mensajes de la sesion.
     */
    static class Out {

        /**
         * Agrega un mensaje de error en la sesion.
         *
         * @param value mensaje
         * @param args parametros
         */
        static void error(String value, Object... args) {
            flash.error(value, args);
        }

        /**
         * Agrega un mensaje en la sesion.
         *
         * @param value mensaje
         * @param args parametros
         */
        static void success(String value, Object... args) {
            flash.success(value, args);
        }
    }

    /**
     * Clase encargada de manejar las redirecciones.
     */
    static class Redirects {

        /**
         * Redirecciona a la ruta anterior.
         */
        static void back() {
            Http.Header referer = request.headers.get("referer");

            if (null == referer) {
                home();
            } else {
                redirect(referer.value());
            }
        }

        /**
         * Redirecciona al inicio.
         */
        static void home() {
            redirect(Router.reverse("Application.home").url);
        }

        /**
         * Redirecciona al listado de amigos.
         */
        static void friends() {
            redirect(Router.reverse("Application.friends").url);
        }

        /**
         * Redirecciona a las canciones del usuario especificado.
         *
         * @param user usuairo
         */
        static void files(User user) {
            Map<String, Object> args = new HashMap<>();
            args.put("email", user.email);

            redirect(Router.reverse("Application.files", args).url);
        }

        /**
         * Redirecciona a las listas de reproduccion del usuario especificado.
         *
         * @param user usuairo
         */
        static void playlists(User user) {
            Map<String, Object> args = new HashMap<>();
            args.put("email", user.email);

            redirect(Router.reverse("Application.playlists", args).url);
        }

        /**
         * Redirecciona a la lista de reproduccion especifidada.
         *
         * @param playlist lista de reproduccion.
         */
        static void playlist(Playlist playlist) {
            Map<String, Object> args = new HashMap<>();
            args.put("playlistId", playlist.getId());

            redirect(Router.reverse("Application.playlist", args).url);
        }
    }

    /**
     * Accion que muestra la pantalla de bienvenida.
     *
     * Carga las siguientes variables:
     * - friends: relaciones del usuario actual.
     * - sounds: canciones del usuario actual.
     * - friendCount: total de amigos del usuario actual.
     * - soundCount: total de canciones del usuario actual.
     */
    public static void home() {
        User currentUser = Data.currentUser();

        List<Relation> relations = Data.relations(currentUser);
        List<Sound> sounds = Data.sounds(currentUser);

        int friendCount = Data.friendCount(currentUser);
        int soundCount = Data.soundCount(currentUser);

        renderArgs.put("friends", relations);
        renderArgs.put("sounds", sounds);
        renderArgs.put("friendCount", friendCount);
        renderArgs.put("soundCount", soundCount);
        renderArgs.put("MAX_RECENT_FRIENDS", MAX_RECENT_FRIENDS);
        renderArgs.put("MAX_RECENT_SOUNDS", MAX_RECENT_SOUNDS);

        render();
    }

    /**
     * Accion que muestra la lista de amigos del usuario.
     *
     * Carga las siguientes variables:
     * - friends: relaciones del usuario actual.
     */
    public static void friends() {
        User currentUser = Data.currentUser();

        List<Relation> relations = Data.relations(currentUser);

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
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            User friend = Data.user(email);

            if (null == friend || Data.isCurrentUser(friend)) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                Relation relation = Data.relation(friend);

                if (null == relation) {
                    Out.error(play.i18n.Messages.get("waves.error.acceptFriend"));
                    Redirects.back();
                } else {
                    relation.accepted = true;
                    relation.save();

                    Out.success(play.i18n.Messages.get("waves.success.acceptFriend", friend.displayName));
                    Redirects.friends();
                }
            }
        }
    }

    /**
     * Accion para enviar una peticion de amistad.
     *
     * Solo se envia si el correo es valido y no existe una peticion anterior para el mismo usuario.
     *
     * @param email correo de la persona a la que se le enviara la peticion.
     */
    public static void addFriend(@Required String email) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            User currentUser = Data.currentUser();
            User friend = Data.user(email);

            if (null == friend || Data.isCurrentUser(friend)) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                Relation relation = Data.relation(friend);

                if (null != relation) {
                    Out.error(play.i18n.Messages.get("waves.error.addFriend", friend.displayName));
                    Redirects.back();
                } else {
                    relation = new Relation(currentUser, friend);
                    relation.save();

                    Out.success(play.i18n.Messages.get("waves.success.addFriend", friend.displayName));
                    Redirects.friends();
                }
            }
        }
    }

    /**
     * Accion para eliminar a un amigo.
     *
     * Solo se eliminara si el correo es valido y existe la relacion.
     *
     * @param email correo de la persona a eliminar.
     */
    public static void removeFriend(@Required String email) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            User friend = Data.user(email);

            if (null == friend || Data.isCurrentUser(friend)) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                Relation relation = Data.relation(friend);

                if (null == relation) {
                    Out.error(play.i18n.Messages.get("waves.error.removeFriend", friend.displayName));
                    Redirects.back();
                } else {
                    relation.delete();

                    Out.success(play.i18n.Messages.get("waves.success.removeFriend", friend.displayName));
                    Redirects.friends();
                }
            }
        }
    }

    /**
     * Accion para mostrar la lista de canciones del usuario.
     *
     * Solo se mostrara si el correo es valido.
     *
     * @param email correo del usuario.
     */
    public static void files(@Required String email) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            User user = Data.user(email);

            if (null == user) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isCurrentUser(user) && !Data.isFriend(user)) {
                    Out.error(play.i18n.Messages.get("waves.error.files"));
                    Redirects.back();
                } else {
                    renderArgs.put("sounds", Data.sounds(user));
                    renderArgs.put("filesOwner", user);

                    render();
                }
            }
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
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            String contentType = MimeTypes.getContentType(file.getName());

            if (!"audio/mpeg3".equals(contentType)) {
                Files.delete(file);

                Out.error(play.i18n.Messages.get("waves.error.upload", file.getName()));
                Redirects.back();
            } else {
                User currentUser = Data.currentUser();

                Sound sound = new Sound(currentUser, file.getName(), "");
                sound.save();

                String out = String.format("%s/%s", SOUND_PATH, currentUser.getId());

                boolean write = Drive.write(file, out, sound.getId().toString(), "mp3");

                if (!write) {
                    sound.delete();

                    Out.error(play.i18n.Messages.get("waves.error.upload", sound.name));
                    Redirects.back();
                } else {
                    sound.path = out.concat("/").concat(sound.getId().toString()).concat(".mp3");
                    sound.save();

                    Out.success(play.i18n.Messages.get("waves.success.upload", sound.name));
                    Redirects.files(currentUser);
                }
            }
        }
    }

    /**
     * Accion para eliminar una cancion con el identificador especificado.
     *
     * Solo se eliminara si la cancion existe.
     *
     * @param soundId identificador
     */
    public static void deleteFile(@Required String soundId) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            Sound sound = Data.sound(soundId);

            if (null == sound) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isOwner(sound)) {
                    Out.error(play.i18n.Messages.get("waves.error.deleteFile"));
                    Redirects.back();
                } else {
                    User currentUser = Data.currentUser();

                    if (Drive.delete(sound.path)) {
                        List<Playlist> playlists = Playlist.q().filter("sounds in", sound).asList();
                        for (Playlist playlist : playlists) {
                            playlist.sounds.remove(sound);
                            playlist.save();
                        }

                        sound.delete();
                        Out.success(play.i18n.Messages.get("waves.success.deleteFile", sound.name));
                    }

                    Redirects.files(currentUser);
                }
            }
        }
    }

    /**
     * Accion que muestra la pagina de la cancion con el identificador especificado.
     *
     * Solo se mostrara si la cancion existe.
     *
     * @param soundId identificador
     */
    public static void streamFile(@Required String soundId) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            Sound sound = Data.sound(soundId);

            if (null == sound) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isOwner(sound) && !Data.belongsToFriend(sound)) {
                    Out.error(play.i18n.Messages.get("waves.error.streamFile"));
                    Redirects.back();
                } else {
                    renderArgs.put("sound", sound);

                    render();
                }
            }
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
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            Sound sound = Data.sound(soundId);

            if (null == sound) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isOwner(sound) && !Data.belongsToFriend(sound)) {
                    Out.error(play.i18n.Messages.get("waves.error.download"));
                    Redirects.back();
                } else {
                    renderBinary(new ByteArrayInputStream(Drive.read(sound.path)), sound.name);
                }
            }
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
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            User user = Data.user(email);

            if (null == user) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                List<Playlist> playlists = Data.lists(user);

                if (!Data.isCurrentUser(user) && !Data.isFriend(user)) {
                    Out.error(play.i18n.Messages.get("waves.error.playlists"));
                    Redirects.back();
                } else {
                    renderArgs.put("playlists", playlists);
                    renderArgs.put("listsOwner", user);

                    render();
                }
            }
        }
    }

    /**
     * Accion que muestra la lista de reproduccion con el identificador especificado.
     *
     * @param playlistId identificador.
     */
    public static void playlist(@Required String playlistId) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            Playlist playlist = Data.list(playlistId);

            if (null == playlist) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isOwner(playlist) && !Data.belongsToFriend(playlist)) {
                    Out.error(play.i18n.Messages.get("waves.error.playlist"));
                    Redirects.back();
                } else {
                    renderArgs.put("playlist", playlist);

                    render();
                }
            }
        }
    }

    /**
     * Accion que crea una nueva lista de reproduccion con el nombre especificado.
     *
     * @param name nombre de la lista de reproduccion.
     */
    public static void createPlaylist(@Required @MinSize(2) @MaxSize(20) String name) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.createPlaylist", name));
            Redirects.back();
        } else {
            Playlist playlist = new Playlist(name, Data.currentUser());
            playlist.save();

            Out.success(play.i18n.Messages.get("waves.success.createPlaylist", name));
            Redirects.playlist(playlist);
        }
    }

    /**
     * Accion que crea elimina una lista de reproduccion con el identificador especificado.
     *
     * @param playlistId identificador de la lista de reproduccion.
     */
    public static void deletePlaylist(@Required String playlistId) {
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            User currentUser = Data.currentUser();
            Playlist playlist = Data.list(playlistId);

            if (!Data.isOwner(playlist)) {
                Out.error(play.i18n.Messages.get("waves.error.deletePlaylist"));
                Redirects.back();
            } else {
                playlist.delete();

                Out.success(play.i18n.Messages.get("waves.success.deletePlaylist", playlist.name));
                Redirects.playlists(currentUser);
            }
        }
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
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            Playlist playlist = Data.list(playlistId);
            Sound sound = Data.sound(soundId);

            if (null == playlist || null == sound) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isOwner(playlist) || (!Data.isOwner(sound) && !Data.belongsToFriend(sound))) {
                    Out.error(play.i18n.Messages.get("waves.error.addToPlaylist"));
                    Redirects.back();
                } else {
                    playlist.sounds.add(sound);
                    playlist.save();

                    Out.success(play.i18n.Messages.get("waves.success.addToPlaylist", sound.name, playlist.name));
                    Redirects.playlist(playlist);
                }
            }
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
        if (Validation.hasErrors()) {
            Out.error(play.i18n.Messages.get("waves.error.invalid"));
            Redirects.back();
        } else {
            Playlist playlist = Data.list(playlistId);
            Sound sound = Data.sound(soundId);

            if (null == playlist || null == sound) {
                Out.error(play.i18n.Messages.get("waves.error.invalid"));
                Redirects.back();
            } else {
                if (!Data.isOwner(playlist)) {
                    Out.error(play.i18n.Messages.get("waves.error.removeFromPlaylist"));
                    Redirects.back();
                } else {
                    playlist.sounds.remove(sound);
                    playlist.save();

                    Out.success(play.i18n.Messages.get("waves.success.removeFromPlaylist", sound.name, playlist.name));
                    Redirects.playlist(playlist);
                }
            }
        }
    }

    /**
     * Accion ajax que realiza una busqueda de archivos.
     *
     * @param name nombre del archivo a buscar.
     */
    public static void searchFile(String playlistId, String name) {
        List<Search> searches = new ArrayList<>();

        if (null != playlistId && !playlistId.isEmpty()) {
            Playlist playlist = Data.list(playlistId);

            if (Data.isOwner(playlist)) {
                List<Sound> sounds = Data.soundsAndFriendSounds();

                if (null != playlist) {
                    for (Sound sound : sounds) {
                        if (!playlist.sounds.contains(sound) && sound.name.toLowerCase().contains(name.toLowerCase())) {
                            searches.add(new Search(sound.getId().toString(), sound.name));
                        }
                    }
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
