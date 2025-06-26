package fctreddit.impl.api.rest;

import fctreddit.api.rest.RestContent;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path(RestContent.PATH)
public interface RestExtendedContent extends RestContent {

    public static final String ALL = "/all";
    public static final String VOTES = "/votes";


    @DELETE
    @Path("/{" + USERID + "}" + ALL)
    void removeAuthorsFromPost(@PathParam(RestContent.USERID) String userId);

    @DELETE
    @Path("/{" + USERID + "}" + VOTES + ALL)
    void removeVotesFromPost(@PathParam(RestContent.USERID) String userId, @QueryParam(PASSWORD) String userPassword);

}
