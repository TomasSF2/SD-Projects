package fctreddit.impl.kafka.replication;

import com.google.gson.Gson;
import fctreddit.api.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.kafka.utils.SyncPoint;
import fctreddit.impl.server.java.JavaContentRep;
import fctreddit.impl.server.java.JavaServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import fctreddit.api.kafka.RecordProcessor;

import static fctreddit.impl.server.java.JavaContentRep.serverURI;
import static fctreddit.impl.server.rest.ContentRepResource.Log;

public class ContentReplicationProcessor implements RecordProcessor {

    private final Content impl = new JavaContentRep();
    private final Gson gson = new Gson();

    @Override
    public void onReceive(ConsumerRecord<String, String> record) {

        Result result;
        try {
            ReplicationEvent event = ReplicationEvent.fromString(record.value());

            switch (event.getOperation()) {
                case "CREATE": {
                    String[] parts = event.getValue().split("#", 2);
                    Post post = gson.fromJson(parts[0], Post.class);
                    String password = parts[1];
                    /*
                    if(post.getParentUrl() != null && !post.getParentUrl().isBlank()){
                        String parentPostID = JavaServer.extractResourceID(post.getParentUrl());
                        String newParentUrl = serverURI + RestContent.PATH + "/" + parentPostID;
                        post.setParentUrl(newParentUrl);
                    }
                     */
                    result = impl.createPost(post, password);
                    break;
                }
                case "UPDATE": {
                    String[] parts = event.getValue().split("#", 3);
                    result = impl.updatePost(parts[0], parts[1], gson.fromJson(parts[2], Post.class));
                    break;
                }
                case "DELETE": {
                    String[] parts = event.getValue().split("#", 2);
                    result = impl.deletePost(parts[0], parts[1]);
                    break;
                }
                case "UPVOTE":
                    result = impl.upVotePost(splitArg(event.getValue(), 0), splitArg(event.getValue(), 1), splitArg(event.getValue(), 2));
                    break;
                case "REMOVE_UPVOTE":
                    result = impl.removeUpVotePost(splitArg(event.getValue(), 0), splitArg(event.getValue(), 1), splitArg(event.getValue(), 2));
                    break;
                case "DOWNVOTE":
                    result = impl.downVotePost(splitArg(event.getValue(), 0), splitArg(event.getValue(), 1), splitArg(event.getValue(), 2));
                    break;
                case "REMOVE_DOWNVOTE":
                    result = impl.removeDownVotePost(splitArg(event.getValue(), 0), splitArg(event.getValue(), 1), splitArg(event.getValue(), 2));
                    break;
                default:
                    throw new RuntimeException();
            }

            SyncPoint.getSyncPoint().setResult(record.offset(), result);

        } catch (Exception e) {
            System.err.println("Error processing record with offset " + record.offset());
            e.printStackTrace();
        }
    }

    private String splitArg(String value, int index) {
        return value.split("#", -1)[index];
    }
}
