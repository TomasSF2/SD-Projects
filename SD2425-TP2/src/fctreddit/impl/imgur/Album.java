package fctreddit.impl.imgur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Album {
    public String id;
    public String title;      // You can consider this as the name
    public String deletehash;

    // Optional: Add a constructor and toString for easier debugging
    public Album() {}

    public Album(String id, String title, String deletehash) {
        this.id = id;
        this.title = title;
        this.deletehash = deletehash;
    }

    @Override
    public String toString() {
        return "Album{id='" + id + "', title='" + title + "', deletehash='" + deletehash + "'}";
    }
}
