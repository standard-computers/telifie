package com.telifie.Models.Connectors;

import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.CollectionsClient;
import com.telifie.Models.Collection;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Configuration;
import org.apache.hc.core5.http.ParseException;
import org.bson.Document;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;
import java.io.IOException;
import java.util.ArrayList;

public class Spotify extends Connector{

    private SpotifyApi spotifyApi;

    public Spotify(Document doc) throws IOException, ParseException, SpotifyWebApiException {
        super(doc);
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(this.getClientId())
                .setClientSecret(this.getSecret())
                .setRedirectUri(SpotifyHttpManager.makeUri(super.getRedirectUri()))
                .build();
        spotifyApi.setAccessToken(this.getAccessToken());
        User user = spotifyApi.getCurrentUsersProfile().build().execute();
        String userId = user.getId();
        super.setUserId(userId);
    }

    public ArrayList<Article> parse(Configuration config) throws IOException, ParseException, SpotifyWebApiException {
        String telifieUser = config.getUser().getId();
        config.setDomain(new Domain(telifieUser)); //Upload to personal domain
        ArticlesClient articles = new ArticlesClient(config); //Create folder of found articles
        CollectionsClient collections = new CollectionsClient(config);
        Collection collection = new Collection(telifieUser, "Spotify Playlists");
        ArrayList<Article> playlists = this.getPlaylists();
        Domain d = new Domain(config.getDomain().getUri());
        d.setName(telifieUser);
        playlists.forEach(art -> {

        });
        return playlists;
    }

    public ArrayList<Article> getPlaylists() throws IOException, ParseException, SpotifyWebApiException {
        GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi.getListOfUsersPlaylists(super.getUserId()).build();
        ArrayList<Article> articles = new ArrayList<>();
        Paging<PlaylistSimplified> playlists = getListOfUsersPlaylistsRequest.execute();
        for(int i = 0; i < playlists.getItems().length; i++){
            Article art = new Article();
            art.setTitle(playlists.getItems()[i].getName());
            art.setLink(playlists.getItems()[i].getExternalUrls().get("spotify"));
            art.setIcon(playlists.getItems()[i].getImages()[0].getUrl());
        }
        return articles;
    }

    public Connector getConnector(){
        return super.getConnector();
    }
}
