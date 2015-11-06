package controllers;

public class Security extends Secure.Security {

    static boolean authentify(String username, String password) {
        return models.User.connect(username, password) != null;
    }
    
    static boolean check(String profile) {
        if("admin".equals(profile)) {
            return models.User.find("byEmail", connected()).<models.User>first().isAdmin;
        }
        return false;
    }
    
    static void onDisconnected() {
        Waves.index();
    }
    
    static void onAuthenticated() {
        Users.index();
    }
    
}

