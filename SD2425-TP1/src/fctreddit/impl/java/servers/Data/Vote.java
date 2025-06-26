package fctreddit.impl.java.servers.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Vote {

    private boolean upVoted;
    @Id
    private String voterId;
    @Id
    private String postId;

    public Vote(){}

    public Vote(boolean upVoted, String postId, String voterId) {
        this.upVoted = upVoted;
        this.postId = postId;
        this.voterId = voterId;
    }

    public boolean isUpVoted() {return upVoted;}

    public void setUpVoted(boolean upVoted) {this.upVoted = upVoted;}

    public String getPostId() {return postId;}

    public void setPostId(String postId) {this.postId = postId;}

    public String getVoterId() {return voterId;}

    public void setVoterId(String voterId) {this.voterId = voterId;}

}
