package controllers;

import controllers.securesocial.SecureSocial;

import models.User;
import play.mvc.Before;
import play.mvc.With;
import securesocial.provider.SocialUser;

/**
 * Este controlador es el encargador de manejar la administracion de la pagina.
 */
@CRUD.For(models.User.class)
@With(SecureSocial.class)
public class Users extends CRUD {

    @Before
    static void checkAuthentification() {
        SocialUser user = SecureSocial.getCurrentUser();
        if (null != user) {
            if (!"admin@waves.com".equals(user.email)) {
                SecureSocial.login();
            }
        }
    }
}
