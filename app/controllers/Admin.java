package controllers;

import controllers.securesocial.SecureSocial;

import play.mvc.Before;
import play.mvc.Router;
import play.mvc.With;

import securesocial.provider.SocialUser;

/**
 * Controlador encargado de manejar el acceso al area de administracion.
 *
 * @author Luis Chávez
 */
@With(SecureSocial.class)
public class Admin extends CRUD {

    @Before
    static void checkAuthentification() {
        SocialUser user = SecureSocial.getCurrentUser();
        if (null != user) {
            if (!"admin@waves.com".equals(user.email)) {
                redirect(Router.reverse("Info.landing").url);
            }
        }
    }
}
