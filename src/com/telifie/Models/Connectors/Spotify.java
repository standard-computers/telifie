package com.telifie.Models.Connectors;

import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Configuration;
import org.apache.hc.core5.http.ParseException;
import org.bson.Document;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;
import java.io.IOException;
import java.util.ArrayList;

public class Spotify extends Connector{

    private SpotifyApi spotifyApi;

    public Spotify(Document doc){
        super(doc);
        //TODO make me request and get user id
        this.spotifyApi = new SpotifyApi.Builder().setAccessToken(super.getAccessToken()).build();
    }

    public ArrayList<Article> parse(Configuration config){
        return this.getPlaylists();
    }

    public ArrayList<Article> getPlaylists(){
        GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi.getListOfUsersPlaylists(super.getUserId()).build();
        try {
            ArrayList<Article> articles = new ArrayList<>();
            Paging<PlaylistSimplified> playlists = getListOfUsersPlaylistsRequest.execute();
            for(int i = 0; i < playlists.getItems().length; i++){
                Article art = new Article();
                art.setTitle(playlists.getItems()[i].getName());
                art.setLink(playlists.getItems()[i].getExternalUrls().get("spotify"));
                art.setIcon(playlists.getItems()[i].getImages()[0].getUrl());
            }
            return articles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SpotifyWebApiException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
