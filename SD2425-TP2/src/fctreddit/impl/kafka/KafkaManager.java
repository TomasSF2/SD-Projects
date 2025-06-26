package fctreddit.impl.kafka;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import fctreddit.api.Post;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;

public class KafkaManager {

    private List<URI> replicas;
    private URI self;

    public KafkaManager(List<URI> allReplicas, URI self) {
        this.replicas = allReplicas.stream()
                .filter(r -> !r.equals(self))
                .collect(Collectors.toList());
        this.self = self;
    }

    public static URI getSelfURI() {
        try {
            String uri = System.getenv("MY_URI"); // Ou alguma outra env/config passada
            return URI.create(uri);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<URI> getReplicaURIs() {
        try {
            String replicasEnv = System.getenv("CONTENT_REPLICAS"); // "https://content-1:8080/rest,https://content-2:8080/rest,..."
            return List.of(replicasEnv.split(",")).stream().map(URI::create).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public void replicatePost(Post post, String secret) {
        Client client = ClientBuilder.newClient();

        for (URI replica : replicas) {
            try {
                client.target(replica.toString() + "/posts")
                        .request()
                        .header("X-Secret", secret)
                        .post(Entity.json(post));
            } catch (Exception e) {
                System.err.println("Falha ao replicar para " + replica + ": " + e.getMessage());
            }
        }
    }
}
