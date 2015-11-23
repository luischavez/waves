package controllers;

import controllers.securesocial.SecureSocial;

import play.i18n.Lang;
import play.mvc.Controller;

/**
 * Controlador encargado de mostrar la informacion publica de la pagina.
 *
 * @author Luis Chávez
 */
public class Public extends Controller {

    /**
     * Accion encargada de cambiar el idioma de la pagina.
     *
     * @param locale idioma
     */
    public static void lang(String locale) {
        if (null == locale) {
            locale = "es";
        }

        Lang.change(locale);

        Application.Redirects.back();
    }

    /**
     * Accion que muestra el landing page.
     */
    public static void landing() {
        if (SecureSocial.isUserLoggedIn()) {
            Application.Redirects.home();
        } else {
            render();
        }
    }
}
