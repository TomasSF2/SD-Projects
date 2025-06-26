package fctreddit.impl.rest.servers;

import fctreddit.api.java.Image;
import fctreddit.api.rest.RestImage;
import fctreddit.impl.java.servers.JavaImage;

import java.util.logging.Logger;

public class RestImageResource extends RestResource implements RestImage {

    private static Logger Log = Logger.getLogger(RestContentResource.class.getName());

    Image impl;

    public RestImageResource() {
        impl = new JavaImage();
    }

    @Override
    public String createImage(String userId, byte[] imageContents, String password) {
        return super.resultOrThrow(impl.createImage(userId, imageContents, password));
    }

    @Override
    public byte[] getImage(String userId, String imageId) {
        return super.resultOrThrow(impl.getImage(userId, imageId));
    }

    @Override
    public void deleteImage(String userId, String imageId, String password) {
        super.resultOrThrow(impl.deleteImage(userId, imageId, password));
    }
}
