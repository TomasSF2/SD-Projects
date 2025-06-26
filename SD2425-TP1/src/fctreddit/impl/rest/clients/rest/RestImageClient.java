package fctreddit.impl.rest.clients.rest;

import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestImage;
import jakarta.ws.rs.client.Entity;

import java.net.URI;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;

public class RestImageClient extends RestClient implements Image {

    private static Logger Log = Logger.getLogger(RestImageClient.class.getName());

    public RestImageClient(URI serverURI) {
        super(serverURI, RestImage.PATH);
    }

    private Result<String> _createImage(String userId, byte[] imageContents, String password) {
        return super.getResult(target.path(userId)
                .queryParam(RestImage.PASSWORD, password)
                .request()
                .post(Entity.entity(imageContents, MediaType.APPLICATION_OCTET_STREAM)),
                String.class);
    }

    private Result<byte[]> _getImage(String userId, String imageId) {
        return super.getResult(target.path(userId).path(imageId)
                .request(MediaType.APPLICATION_OCTET_STREAM)
                .get(), byte[].class);
    }

    private Result<Void> _deleteImage(String userId, String imageId, String password) {
        return super.getResult(target.path(userId).path(imageId)
                .queryParam(RestImage.PASSWORD, password)
                .request()
                .delete(), Void.class);
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        return super.reTry( () -> _createImage(userId, imageContents, password));
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        return super.reTry( () -> _getImage(userId, imageId));
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        return super.reTry( () -> _deleteImage(userId, imageId, password));
    }
}
