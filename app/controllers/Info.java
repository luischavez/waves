package controllers;

import play.mvc.Controller;

/**
 * Controlador encargado de mostrar la informacion basica de la pagina.
 */
public class Info extends Controller {


    /**
     * Accion que muestra el landing page.
     */
    public static void landing() {
        render();
    }
}
