package fctreddit.impl.java.servers.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.util.UUID;

@Entity
public class ImageData {
    @Id
    private String imageId;

    private String userId;

    @Lob
    private byte[] data;

    public ImageData() {
    }

    public ImageData(String userId, byte[] data) {
        this.imageId = UUID.randomUUID().toString();
        this.userId = userId;
        this.data = data;
    }

    public String getImageId() {
        return imageId;
    }

    public String getUserId() {
        return userId;
    }

    public byte[] getData() {
        return data;
    }
}
