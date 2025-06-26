package fctreddit.impl.imgur;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImgurImage {
    public String id;
    public String title;
    public String type;
    public String link;

    @Override
    public String toString() {
        return "Image{id='" + id + "', title='" + title + "', type='" + type + "', link='" + link + "'}";
    }
}

