package fctreddit.impl.java.servers;

import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;

import java.util.List;
import java.util.logging.Logger;

import fctreddit.impl.api.java.ExtendedContent;
import utils.Hibernate;

import static fctreddit.impl.java.clients.Clients.ContentClients;
import static fctreddit.impl.java.clients.Clients.ImageClients;


public class JavaUsers implements Users {

    private static Logger Log = Logger.getLogger(JavaUsers.class.getName());
    private Hibernate hibernate = Hibernate.getInstance();

    private static ExtendedContent contentClients;
    private static Image imageClients;

    private static ExtendedContent getContentClients() {
        if (contentClients == null) {
            contentClients = ContentClients.get();
        }
        return contentClients;
    }

    private static Image getImageClients() {
        if (imageClients == null) {
            imageClients = ImageClients.get();
        }
        return imageClients;
    }


    @Override
    public Result<String> createUser(User user) {
        Log.info("createUser : " + user);

        // Check if user data is valid
        if (badUserInfoCreate(user)) {
            Log.info("User object invalid.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        try {
            hibernate.persist(user);
            Log.info("User " + user.getUserId() + " created.");
            return Result.ok(user.getUserId());

        } catch (Exception e) {
            e.printStackTrace(); //Most likely the exception is due to the user already existing...
            Log.info("Failed creating user: " + e.getMessage());
            return Result.error(Result.ErrorCode.CONFLICT);
        }
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        Log.info("getUser : user = " + userId + "; pwd = " + password);
        // Check if user is valid
        if (userId == null || password == null) {
            Log.info("UserId or password null.");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        User user;
        try {
            user = hibernate.get(User.class, userId);
        } catch (Exception e) {
            Log.severe("Exception while getting user: " + e.getMessage());
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        // Check if user exists
        if (user == null) {
            Log.info("User does not exist.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        // Check if the password is correct
        if (!user.getPassword().equals(password)) {
            Log.info("Password is incorrect.");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User newUser) {
        Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; userData = " + newUser);

        // Check if user data is valid
        if (badUserInfoUpdate(userId, password, newUser)) {
            Log.info("User object invalid.");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        User user;
        try {
            var res = getUser(userId,password);
            if(!res.isOK())
                return res;
            user = res.value();

            user.updateUserTo(newUser);

            hibernate.update(user);
            Log.info("User " + user.getUserId() + " updated.");
            return Result.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Failed updating user: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        Log.info("deleteUser : user = " + userId + "; pwd = " + password);

        User user;
        try {
            var userRes = getUser(userId,password);
            if(!userRes.isOK())
                return Result.error(userRes.error());

            user = userRes.value();

            getContentClients().removeAuthorsFromPost(userId);

            getContentClients().removeVotesFromPost(userId, password);
            if(user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty())
                getImageClients().deleteImage(userId, user.getAvatarUrl().substring(user.getAvatarUrl().lastIndexOf("/") + 1), password);

            hibernate.delete(user);
            Log.info("User " + user.getUserId() + " deleted.");

            return Result.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Failed deleting user: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info("searchUsers : pattern = " + pattern);

        try {
            List<User> list = hibernate.jpql("SELECT u FROM User u WHERE u.userId LIKE '%" + pattern +"%'", User.class);
            List<User> secureList = list.stream().
                    map(User::clonePwdHidden).
                    toList();
            return Result.ok(secureList);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Failed searching users : " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private boolean badUserInfoCreate(User user){
        return user.getUserId() == null || user.getUserId().isEmpty() || user.getPassword() == null || user.getPassword().isEmpty() ||
                user.getFullName() == null || user.getFullName().isEmpty() || user.getEmail() == null || user.getEmail().isEmpty();
    }

    private boolean badUserInfoUpdate(String userId, String pwd, User user){
        return userId == null || userId.isEmpty() || pwd == null || pwd.isEmpty();
    }
}
