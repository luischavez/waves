package controllers;

import controllers.securesocial.SecureSocial;

import play.mvc.Controller;
import play.mvc.Router;

/**
 * Controlador encargado de mostrar la informacion basica de la pagina.
 *
 * @author Luis Chávez
 */
public class Info extends Controller {

    /**
     * Accion que muestra el landing page.
     */
    public static void landing() {
        if (SecureSocial.isUserLoggedIn()) {
            redirect(Router.reverse("Application.home").url);
        } else {
            render();
        }
    }
}
