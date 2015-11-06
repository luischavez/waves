
import play.*;
import play.jobs.*;
import play.test.*;

import models.*;
import securesocial.provider.ProviderType;
import securesocial.provider.UserId;

/**
 * Clase encargada de cargar la informacion inicial para las pruebas.
 */
@OnApplicationStart
public class Bootstrap extends Job {

    /**
     * Carga la informacion pre-cargada.
     */
    public void doJob() {
        // Check if the database is empty
        if(null == User.find("byEmail", "admin@waves.com").get()) {
            User admin = new User();
            UserId id = new UserId();
            id.id = "admin";
            id.provider = ProviderType.userpass;
            admin.id = id;
            admin.displayName = "admin";
            admin.email = "admin@waves.com";
            admin.password = "Xr4ilOzQ4PCOq3aQ0qbuaQ==";
            admin.isEmailVerified = true;
            admin.save();
        }
    }

}
