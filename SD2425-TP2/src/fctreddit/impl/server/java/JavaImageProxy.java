package fctreddit.impl.server.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.ImgurApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.imgur.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class JavaImageProxy extends JavaServer implements Image {
    private static final String apiKey = "971923550b1b36d";
    private static final String apiSecret = "79d86f1a1f92d9b3b433c3cbeca46246d5107141";
    private static final String accessTokenStr = "110990ffa0d103a6c366e177744202db34176a0d";

    private static final String CREATE_ALBUM_URL = "https://api.imgur.com/3/album";
    private static final String GET_ALBUMS_URL = "https://api.imgur.com/3/account/TomasSF2/albums";
    private static final String GET_ALBUM_URL = "https://api.imgur.com/3/account/TomasSF2/album/{{albumHash}}";
    private static final String DELETE_ALBUM_URL = "https://api.imgur.com/3/account/TomasSF2/album/{{albumHash}}";
    private static final String DELETE_IMAGE_FROM_ALBUM_URL = "https://api.imgur.com/3/image/{{imageHash}}";
    private static final String UPLOAD_IMAGE_URL = "https://api.imgur.com/3/image";
    private static final String GET_IMAGE_URL = "https://api.imgur.com/3/image/{{imageHash}}";
    private static final String ADD_IMAGE_TO_ALBUM_URL = "https://api.imgur.com/3/album/{{albumHash}}/add";
    private static final String GET_IMAGE_FROM_ALBUM = "https://api.imgur.com/3/album/{{albumHash}}/image/{{imageHash}}";
    private static final String GET_IMAGES_FROM_ALBUM = "https://api.imgur.com/3/album/{{albumHash}}/images";


    private static final int HTTP_SUCCESS = 200;
    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    public static String ALBUM_NAME = "";
    private static String ALBUM_ID = "";

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;

    public static boolean resetState;
    public static boolean init = false;
    private static Logger Log = Logger.getLogger(JavaImageProxy.class.getName());


    public void initAlbum(){
        if(!init){
            Album album = albumExists();
            if(album == null){
                ALBUM_ID = createAlbum();
            } else if(!resetState) {
                ALBUM_ID = album.id;
            } else{
                deleteAlbum(album.id);
                ALBUM_ID = createAlbum();
                resetState = false;
            }
            init = true;
        }
    }


    public JavaImageProxy(){
        json = new Gson();
        accessToken = new OAuth2AccessToken(accessTokenStr);
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(ImgurApi.instance());
        initAlbum();
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) throws Exception {
        Result<User> owner = getUsersClient().getUser(userId, password);
        if (!owner.isOK())
            return Result.error(owner.error());

        Log.info("\n\n ALBUM ID: " + ALBUM_ID + "\n\n");
        String imageId = UUID.randomUUID().toString();
        String imageHash = uploadImage(imageContents, imageId);
        if(imageHash == null){
            return Result.error(Result.ErrorCode.CONFLICT);
        }
        if(!addImageToAlbum(ALBUM_ID, imageHash))
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);

        return Result.ok(imageHash);
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {

        String imageFromAlbum = getImageId(imageId);
        if(imageFromAlbum == null){
            Log.info("Failed to fetch image");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        byte[] image = getImageFromAlbum(imageId);
        if(image == null){
            Log.info("Failed to fetch image");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        return Result.ok(image);
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) throws Exception {
        Result<User> owner = getUsersClient().getUser(userId, password);
        if (!owner.isOK())
            return Result.error(owner.error());

        if(!getImage(userId, imageId).isOK())
            return Result.error(Result.ErrorCode.NOT_FOUND);
        else {
            if (deleteImageFromAlbum(imageId))
                return Result.ok();
        }

        return Result.error(Result.ErrorCode.CONFLICT);
    }

    private Album albumExists() {
        OAuthRequest request = new OAuthRequest(Verb.GET, GET_ALBUMS_URL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, request);

        try{
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                Log.info("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
            } else {
                Log.info("Albums fetched successfully! Contents of Body: " + r.getBody() + "\n");

                ObjectMapper mapper = new ObjectMapper();
                AlbumListResponse response = mapper.readValue(r.getBody(), AlbumListResponse.class);

                for (Album album : response.data) {
                    if(ALBUM_NAME.equals(album.title))
                        return album;
                }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void deleteAlbum(String albumId) {
        String requestUrl = DELETE_ALBUM_URL.replace("{{albumHash}}", albumId);
        OAuthRequest request = new OAuthRequest(Verb.DELETE, requestUrl);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                Log.info("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
            } else {
                Log.info("Deleted successfully! Contents of Body: " + r.getBody() + "\n");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String createAlbum(){
        OAuthRequest request = new OAuthRequest(Verb.POST, CREATE_ALBUM_URL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new CreateAlbumArguments(ALBUM_NAME, ALBUM_NAME)));

        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                Log.info("\nOperation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return null;
            } else {
                BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
                Log.info("\nContents of Body: " + r.getBody());
                Log.info("\nAlbum created successfully\nAlbum name: " + ALBUM_NAME + "\nAlbum ID: " + body.getData().get("id") + "\n");
                return body.getData().get("id").toString();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private String uploadImage(byte[] imageContents, String imageId) throws Exception {
        OAuthRequest request = new OAuthRequest(Verb.POST, UPLOAD_IMAGE_URL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new ImageUploadArguments(imageContents, imageId)));

        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                Log.info("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return null;
            } else {
                BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
                Log.info("Image uploaded successfully!\nImage name: " + imageId + "\nImage ID: " + body.getData().get("id") + "\n");
                return body.getData().get("id").toString();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private boolean addImageToAlbum(String albumId, String imageId){
        String requestURL = ADD_IMAGE_TO_ALBUM_URL.replaceAll("\\{\\{albumHash\\}\\}", albumId);

        OAuthRequest request = new OAuthRequest(Verb.POST, requestURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new AddImagesToAlbumArguments(imageId)));

        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                Log.info("\nOperation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return false;
            } else {
                Log.info("\nImage successfully added to album! Contents of Body: " + r.getBody() + "\n");
                return true;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String getImageId(String imageId){
        String requestURL = GET_IMAGES_FROM_ALBUM.replaceAll("\\{\\{albumHash\\}\\}", ALBUM_ID);
        OAuthRequest request = new OAuthRequest(Verb.GET, requestURL);

        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                System.err.println("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return null;
            } else {
                System.err.println("Image fetched successfully! Contents of Body: " + r.getBody());

                JsonObject jsonBody = JsonParser.parseString(r.getBody()).getAsJsonObject();
                if(jsonBody.has("data") && jsonBody.get("data").isJsonNull()){
                    return null;
                }

                BasicResponseList body = json.fromJson(r.getBody(), BasicResponseList.class);

                if(body.getData() != null){
                    ObjectMapper mapper = new ObjectMapper();
                    ImgurImageResponse response = mapper.readValue(r.getBody(), ImgurImageResponse.class);

                    for (ImgurImage image : response.data) {
                        if(image.id.equals(imageId)){
                            return image.id;
                        }
                    }
                }else
                    return null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return requestURL;
    }

    private byte[] getImageFromAlbum(String imageId){
        String requestURL = GET_IMAGE_FROM_ALBUM.replaceAll("\\{\\{albumHash\\}\\}", ALBUM_ID).replaceAll("\\{\\{imageHash\\}\\}", imageId);
        OAuthRequest request = new OAuthRequest(Verb.GET, requestURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, request);

        try{
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                Log.info("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
            } else {
                Log.info("Image fetched successfully! Contents of Body: " + r.getBody() + "\n");

                JsonObject jsonBody = JsonParser.parseString(r.getBody()).getAsJsonObject();
                if(jsonBody.has("data") && jsonBody.get("data").isJsonNull()){
                    return null;
                }

                BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
                for(Object key: body.getData().keySet()) {
                    System.err.println(key + " -> " + body.getData().get(key));
                }

                return this.downloadImageBytes(body.getData().get("link").toString());
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private boolean deleteImageFromAlbum(String imageId){
        String requestUrl = DELETE_IMAGE_FROM_ALBUM_URL.replaceAll("\\{\\{imageHash\\}\\}", imageId);
        OAuthRequest request = new OAuthRequest(Verb.DELETE, requestUrl);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                Log.info("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return false;
            } else {
                Log.info("Image deleted successfully! Contents of Body: " + r.getBody() + "\n");
                return true;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private byte[] downloadImageBytes(String imageURL) {
        OAuthRequest request = new OAuthRequest(Verb.GET, imageURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if(r.getCode() == HTTP_SUCCESS) {
                byte[] imageContent = r.getStream().readAllBytes();
                System.err.println("Successfully downloaded " + imageContent.length + " bytes from the image.");
                return imageContent;
            } else {
                System.err.println("Operation to download image bytes Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to download image bytes");
            throw new RuntimeException(e);
        }
    }
}
