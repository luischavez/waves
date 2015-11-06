import models.User;
import securesocial.provider.SocialUser;
import securesocial.provider.UserId;
import securesocial.provider.UserServiceDelegate;

/**
 * Clase encargada de manejar la persistencia del usuario.
 */
public class UserServiceImp implements UserServiceDelegate {

    @Override
    public SocialUser find(UserId id) {
        User mongoUser = User.find("byId", id).get();
        if (null == mongoUser) {
            return null;
        }
        SocialUser user = new SocialUser();
        user.id = mongoUser.id;
        user.displayName = mongoUser.displayName;
        user.email = mongoUser.email;
        user.avatarUrl = mongoUser.avatarUrl;
        user.lastAccess = mongoUser.lastAccess;
        user.password = mongoUser.password;
        user.isEmailVerified = mongoUser.isEmailVerified;
        return user;
    }

    @Override
    public SocialUser find(String email) {
        User mongoUser = User.find("byEmail", email).get();
        if (null == mongoUser) {
            return null;
        }
        SocialUser user = new SocialUser();
        user.id = mongoUser.id;
        user.displayName = mongoUser.displayName;
        user.email = mongoUser.email;
        user.avatarUrl = mongoUser.avatarUrl;
        user.lastAccess = mongoUser.lastAccess;
        user.password = mongoUser.password;
        user.isEmailVerified = mongoUser.isEmailVerified;
        return user;
    }

    @Override
    public void save(SocialUser user) {
        User mongoUser;
        if (null != find(user.email)) {
            mongoUser = User.find("byEmail", user.email).get();
        } else {
            mongoUser = new User();
        }

        mongoUser.id = user.id;
        mongoUser.displayName = user.displayName;
        mongoUser.email = user.email;
        mongoUser.avatarUrl = user.avatarUrl;
        mongoUser.lastAccess = user.lastAccess;
        mongoUser.password = user.password;
        mongoUser.isEmailVerified = true;
        mongoUser.save();
    }

    @Override
    public String createActivation(SocialUser user) {
        return null;
    }

    @Override
    public boolean activate(String uuid) {
        return false;
    }

    @Override
    public String createPasswordReset(SocialUser user) {
        return null;
    }

    @Override
    public SocialUser fetchForPasswordReset(String username, String uuid) {
        return null;
    }

    @Override
    public void disableResetCode(String username, String uuid) {

    }

    @Override
    public void deletePendingActivations() {

    }
}
