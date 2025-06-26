package fctreddit.impl.kafka.replication;

import fctreddit.api.Post;

public class ReplicatedPost {
    public long version;
    public String operation;
    public Post post;
    public String secret;

    public ReplicatedPost() {}

    public ReplicatedPost(Post post, long version, String secret) {
        this.version = version;
        this.operation = "createPost";
        this.post = post;
        this.secret = secret;
    }
}
