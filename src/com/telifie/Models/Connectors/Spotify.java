package com.telifie.Models.Connectors;

import com.telifie.Models.Article;
import com.telifie.Models.Articles.Attribute;
import com.telifie.Models.Articles.DataSet;
import com.telifie.Models.Articles.Source;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.CollectionsClient;
import com.telifie.Models.Collection;
import com.telifie.Models.Connector;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import org.apache.hc.core5.http.ParseException;
import org.bson.Document;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Spotify extends Connector {

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

    public Spotify(Connector con) throws IOException, ParseException, SpotifyWebApiException {
        this(Document.parse(con.toString()));
    }

    public void parse(Configuration config, Session session) throws IOException, ParseException, SpotifyWebApiException {

        Configuration personalConfiguration = new Configuration();
        Domain pd = new Domain();
        pd.setId(session.getUser());
        personalConfiguration.setDomain(pd);
        ArticlesClient articles = new ArticlesClient(config, session); //Put found articles in personal domain
        CollectionsClient collections = new CollectionsClient(config, session); //Put generated collections in personal domain
        Source primarySource = new Source(
                "com.telifie.connectors.spotify",
                "https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/connectors/spotify.png",
                "Connector / Spotify",
                "https://telifie.com/documentation/connectors/spotify"
        );

        //Prepare collections for organizing Spotify content
        Collection playlistsCollection = new Collection("Spotify Playlists").setDomain(session.getUser());
        playlistsCollection.setIcon("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/images/connectors/spotify.png");
        collections.create(playlistsCollection);

        GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi.getListOfUsersPlaylists(super.getUserId()).build();
        Paging<PlaylistSimplified> plsts = getListOfUsersPlaylistsRequest.execute();

        for (int i = 0; i < plsts.getItems().length; i++) {
            String playlist = plsts.getItems()[i].getId();

            //Make article for playlist
            Article playlistArticle = new Article();
            playlistArticle.setTitle(plsts.getItems()[i].getName());
            playlistArticle.setLink(plsts.getItems()[i].getExternalUrls().get("spotify"));
            playlistArticle.setIcon(plsts.getItems()[i].getImages()[0].getUrl());
            playlistArticle.setDescription("Spotify Playlist");
            DataSet tracks = new DataSet("Tracks");
            tracks.add(new String[]{"Cover", "Track Name", "Artists", "Album", "Duration"});
            playlistArticle.setSource(primarySource);

            //Create playlist collection
            Collection playlistCollection = new Collection(plsts.getItems()[i].getName());
            playlistCollection.setIcon(plsts.getItems()[i].getImages()[0].getUrl());

            Paging<PlaylistTrack> playlistTracks = spotifyApi.getPlaylistsItems(playlist).build().execute();
            for (PlaylistTrack playlistTrack : playlistTracks.getItems()) {
                Track track = (Track) playlistTrack.getTrack();

                //Prepare playlist tracks as data set and add to playlist article
                String trackName = Telifie.tools.strings.htmlEscape(track.getName());
                String trackLink = track.getExternalUrls().get("spotify");
                String trackIcon = Telifie.tools.strings.htmlEscape(track.getAlbum().getImages()[0].getUrl());
                String trackAlbumName = track.getAlbum().getName();
                String trackDuration = formatDuration(track.getDurationMs());
                Date addedAt = playlistTrack.getAddedAt();
                String formattedAddedAt = new SimpleDateFormat("MMM d, yyyy h:mm a").format(addedAt);

                List<String> artistNames = new ArrayList<>();
                for (ArtistSimplified artist : track.getArtists()) {
                    artistNames.add(artist.getName());
                }
                String artists = String.join(", ", artistNames);
                tracks.add(new String[]{"@" + trackIcon, trackName, artists, trackAlbumName, trackDuration});

                //No create an article for each track
                Article trackArticle = new Article();
                trackArticle.setTitle(trackName);
                trackArticle.setLink(trackLink);
                trackArticle.setIcon(trackIcon);
                trackArticle.setDescription("Spotify Track");
                trackArticle.addAttribute(new Attribute("Artist", artists));
                trackArticle.addAttribute(new Attribute("Album", trackAlbumName));
                trackArticle.addAttribute(new Attribute("Duration", trackDuration));
                trackArticle.addAttribute(new Attribute("Added", formattedAddedAt));
                trackArticle.setContent(trackName + " is a track by " + artists + " from " + trackAlbumName + ". It's " + trackDuration + " in length. It was added on " + formattedAddedAt + ".");
                articles.create(trackArticle); //Create article for track
                collections.save(playlistCollection, trackArticle); //Add article to this playlist collection
                trackArticle.setSource(primarySource);
            }
            playlistArticle.addDataSet(tracks);
            articles.create(playlistArticle); //Create article for playlist
            collections.save(playlistsCollection, playlistArticle); //Add article to playlists collection
        }
    }

    public Connector getConnector() {
        return super.getConnector();
    }

    private String formatDuration(int durationMs) {
        int seconds = (durationMs / 1000) % 60;
        int minutes = (durationMs / 1000) / 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}