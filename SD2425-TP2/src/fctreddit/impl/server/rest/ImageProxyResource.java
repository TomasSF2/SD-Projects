package fctreddit.impl.server.rest;

import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestImage;
import fctreddit.impl.server.java.JavaImageProxy;
import jakarta.ws.rs.WebApplicationException;

import java.util.logging.Logger;

public class ImageProxyResource extends RestResource implements RestImage {

    private static Logger Log = Logger.getLogger(ImageProxyResource.class.getName());

    Image impl;

    public static String baseURI = null;


    public ImageProxyResource() {
        impl = new JavaImageProxy();
    }

    public static void setServerBaseURI(String s) {
        if(ImageProxyResource.baseURI == null)
            ImageProxyResource.baseURI = s;
    }

    @Override
    public String createImage(String userId, byte[] imageContents, String password) throws Exception {
        Result<String> res = impl.createImage(userId, imageContents, password);

        if(res.isOK())
            return ImageProxyResource.baseURI + RestImage.PATH + "/" + userId + "/" + res.value();

        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public byte[] getImage(String userId, String imageId) {
        Result<byte[]> res = impl.getImage(userId, imageId);

        if(res.isOK())
            return res.value();

        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public void deleteImage(String userId, String imageId, String password) throws Exception {
        Result<Void> res = impl.deleteImage(userId, imageId, password);

        if(res.isOK())
            return;

        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }
}
