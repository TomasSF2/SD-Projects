package fctreddit.impl.api.java;

import fctreddit.api.java.Content;
import fctreddit.api.java.Result;

public interface ExtendedContent extends Content {

    Result<Void> removeAuthorsFromPost(String userId);

    Result<Void> removeVotesFromPost(String userId, String userPassword);

}
