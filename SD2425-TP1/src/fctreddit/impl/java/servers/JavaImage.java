package fctreddit.impl.java.servers;

import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.api.rest.RestImage;
import fctreddit.impl.java.servers.Data.ImageData;
import utils.Hibernate;

import java.util.logging.Logger;

import static fctreddit.impl.java.clients.Clients.UsersClients;

public class JavaImage implements Image {

    private static final Logger Log = Logger.getLogger(JavaImage.class.getName());
    private static final Hibernate hibernate = Hibernate.getInstance();

    private static Users usersClient;

    private static Users getUsersClients() {
        if (usersClient == null) {
            usersClient = UsersClients.get();
        }
        return usersClient;
    }
    private final String IMAGE = "image";

    // Pode ser configurado via variável de ambiente ou constante hardcoded
    public static String serverURI;

    private Result<User> authenticate(String userId, String password) {
        return getUsersClients().getUser(userId, password);
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        Log.info("createImage: " + userId);

        if (imageContents == null || imageContents.length == 0 || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        var auth = authenticate(userId, password);
        if (!auth.isOK())
            return Result.error(auth.error());

        var image = new ImageData(userId, imageContents);

        try {
            hibernate.persist(image);

            String uri = String.format("%s%s/%s/%s", serverURI, RestImage.PATH, userId, image.getImageId());
            return Result.ok(uri);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Log.info("getImage: userId = " + userId + ", imageId = " + imageId);

        if (userId == null || imageId == null) {
            Log.info("getImage: userId or imageId is null");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        ImageData image;
        try {
            image = hibernate.get(ImageData.class, imageId);
        } catch (Exception e) {
            Log.severe("getImage: " + e.getMessage());
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        if (image == null) {
            Log.info("getImage: Image " + imageId + " not found");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        if (!image.getUserId().equals(userId)) {
            Log.info("getImage: userId doesn't match: " + image.getUserId());
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        var data = image.getData();
        if (data == null) {
            Log.info("getImage: Image found but content is null.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Log.info("getImage: Image found successfully. Size (bytes): " + data.length);
        return Result.ok(data);
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        Log.severe("deleteImage: " + imageId);

        if (password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        var auth = authenticate(userId, password);
        if (!auth.isOK())
            return Result.error(auth.error());

        try {
            var image = hibernate.get(ImageData.class, imageId);
            if (image == null)
                return Result.error(Result.ErrorCode.NOT_FOUND);

            if (!image.getUserId().equals(userId))
                return Result.error(Result.ErrorCode.FORBIDDEN);

            hibernate.delete(image);
            Log.info("deleteImage: Image " + imageId + " successfully deleted");
            return Result.ok();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}
