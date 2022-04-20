package com.example.musicsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MusicBrainzObject {
    final String MBbaseURL = "https://musicbrainz.org/ws/2/";
    final String MBurlParameters = "?&fmt=json&inc=url-rels+release-groups";
    final String wikidataBaseURL = "https://www.wikidata.org/w/api.php";
    final String wikidataParameters = "?action=wbgetentities&format=json&props=sitelinks";
    final String wikipediaBaseURL = "https://en.wikipedia.org/w/api.php";
    final String wikipediaParameters = "?action=query&format=json&prop=extracts&exintro=true&explaintext=true&redirects=true";
    final String coverArtBaseURL = "https://coverartarchive.org";

    String mbid;
    Entity entityType;
    List syncAlbums;

    public MusicBrainzObject(String mbid, Entity entityType){
        this.mbid = mbid;
        this.entityType = entityType;
        this.syncAlbums = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * This method can be regarded as the main method of the MusicBrainzObject. The method makes calls to other methods
     * and composes a JSON response of the call.
     * @return A JSON string containing the name, MBID, descriptions and albums (with cover image if present). If the
     * MBID does not belong to an artist or band an error string informing the user will be returned.
     */
    public String lookup(){
        JSONObject mbObject;
        JSONArray albums;
        JSONObject resultObject;
        try{
            URL url = new URL(MBbaseURL + entityType + "/" + mbid + MBurlParameters);
            mbObject = getJSONfromURL(url);
            if(mbObject == null){
                return "The infromation from MusicBrainz was failed to be retrieved or " +
                        "the MBID does not belong to an artist :'(";
            }
            resultObject = MBdataExtraction(mbObject);
            albums = getAlbums(mbObject);
            resultObject.put("albums", albums);

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return resultObject.toJSONString();
    }

    /**
     * A method to make a GET() request via http to a website and receive a JSON response.
     * @param url The URL to website to send the request to.
     * @return The JSON response from the website.
     */
    private JSONObject getJSONfromURL(URL url){
        JSONObject jsonObject;
        InputStream in;
        JSONParser jsonParser = new JSONParser();

        try{
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("GET");
            http.connect();

            if (http.getResponseCode() == 200) {
                try {
                    in = http.getInputStream();
                    jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
                return jsonObject;

            } else {
                return null;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method extracts information about an artist or a band and makes further calls to extract the description.
     * @param jsonObject The JSON response from the lookup with the MBID towards the MusicBrainz's database.
     * @return Returns a JSONObject containing information about the artist/band.
     */
    private JSONObject MBdataExtraction(JSONObject jsonObject){
        JSONArray relations;
        JSONObject returnObject = new JSONObject();
        String description;

        relations = (JSONArray) jsonObject.get("relations");
        description = getDescription(relations);

        returnObject.put("name", jsonObject.get("name"));
        returnObject.put("mbid", mbid);
        returnObject.put("description", description);
        return returnObject;
    }

    /**
     * This method makes a call either directly to wikipedia to extract a description for an artist or a band, or it will
     * make a call via wikidata first to extract the artist or band name to be able to then make the call to wikipedia.
     * @param relations A JSONArray containing multiple relations to the artist/band. The ones that are of interest are
     *                  the ones with either type=wikipedia or type=wikidata.
     * @return Returns a description in plain text from wikipedia (can be changed to html by removing "&explaintext=true"-
     * part in the wikipedia parameter string.
     */
    private String getDescription(JSONArray relations){
        JSONObject wikipedia = null;
        JSONObject wikidata = null;
        JSONObject wikiObject;
        JSONObject tmp;

        String description;
        String title;
        String wikiURL;

        for (Object relation : relations) {
            JSONObject obj = (JSONObject) relation;
            if(obj.get("type").equals("wikidata")){
                wikidata = obj;
            } else if(obj.get("type").equals("wikipedia")){
                wikipedia = obj;
            }
        }

        // If a wikipeda type is found, that object is used as the artis/band name can be extracted from that object.
        // Otherwise, the wikdata object will be used to extract an identifier that can be used to lookup cite links to
        // wikipedia from which the artist/band name can be extracted. With the artist/band name a lookup towards the
        // wikipedia API can be made to retrieve a description.

        // First the artist/band name will be extracted from the wikipedia json object. It contains a key called "url",
        // that contains another json object containing a key called "resource" with the value that is the url to the
        // artist's/band's wikipeda cite. The url is on the form "https://en.wikipedia.org/wiki/ARTIST_NAME"
        // and the artist or band name will be extracted from that url (index 30 is the first index of the artist name in the url).
        // The tmp object will be used to access nested data in the JSONObjects.
        if(wikipedia != null){

            tmp = (JSONObject) wikipedia.get("url");
            wikiURL = (String) tmp.get("resource");
            title = wikiURL.substring(30);

            try{
                URL url = new URL(wikipediaBaseURL + wikipediaParameters +
                        "&titles=" + URLEncoder.encode(title, StandardCharsets.UTF_8));
                wikiObject = getJSONfromURL(url);
                assert wikiObject != null;
                tmp = (JSONObject) wikiObject.get("query");
                tmp = (JSONObject) tmp.get("pages");
                tmp = (JSONObject) tmp.values().toArray()[0];
                description = (String) tmp.get("extract");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // The wikidata object contains a key called "url" that contains another json object containing a key called
        // "resource" with the value that is the url to the artist's/band's wikidata cite. The wikidata url is on the form:
        // "https://www.wikidata.org/wiki/IDENTIFIER" (the identifier starts at index 30)
        //  where the identifier can be used to lookup in wikidata's API and fetch cite links to the artist's/band's
        //  wikipedia cite. Then the artis/band name can be extracted and a lookup towards the wikipedia API can be made.
        else if (wikidata != null) {
            String wikidataID;

            tmp = (JSONObject) wikidata.get("url");
            wikiURL = (String) tmp.get("resource");
            wikidataID = wikiURL.substring(30);

            try{
                URL wikiDdataURL = new URL(wikidataBaseURL + wikidataParameters +
                        "&ids=" + wikidataID);
                wikiObject = getJSONfromURL(wikiDdataURL);
                assert wikiObject != null;
                tmp = (JSONObject) wikiObject.get("entities");
                tmp = (JSONObject) tmp.get(wikidataID);
                tmp = (JSONObject) tmp.get("sitelinks");
                tmp = (JSONObject) tmp.get("enwiki");
                title = (String) tmp.get("title");

                URL wikipediaURL = new URL(wikipediaBaseURL + wikipediaParameters +
                        "&titles=" + URLEncoder.encode(title, StandardCharsets.UTF_8));
                wikiObject = getJSONfromURL(wikipediaURL);
                assert wikiObject != null;
                tmp = (JSONObject) wikiObject.get("query");
                tmp = (JSONObject) tmp.get("pages");
                tmp = (JSONObject) tmp.values().toArray()[0];
                description = (String) tmp.get("extract");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            return null;
        }

        return description;
    }

    /**
     * Gets the albums from the artis/band. It will also try to fetch all the cover art images for each album.
     * The fetching of the images will be done in parallel execution.
     * @param mbObject The MusicBrainz JSONObject. The interest here is the "release-groups" field, that contains e.g.
     *                 all the albums.
     * @return Returns a JSONArray with all the albums with corresponding cover image if successfully fetched.
     */
    private JSONArray getAlbums(JSONObject mbObject) {
        JSONArray releaseGroups;
        JSONArray albums = new JSONArray();

        releaseGroups = (JSONArray) mbObject.get("release-groups");
        releaseGroups.parallelStream().forEach(this::parallelGet);
        albums.addAll(syncAlbums);

        return albums;
    }

    /**
     * The method that is used to run in parallel. It first checks that the element is of type "Album", after that it
     * makes a request to get the cover image from Cover Art Archives. Each album will then be added to a synchronized
     * array.
     * @param release Each element in the "release-groups"-array.
     */
    private void parallelGet(Object release){
        JSONObject obj = (JSONObject) release;
        JSONObject album = new JSONObject();

        if(obj.get("primary-type") != null && obj.get("primary-type").equals("Album")){
            String coverImg = getCoverImg(obj);
            album.put("title", obj.get("title"));
            album.put("id", obj.get("id"));
            album.put("image", coverImg);
            syncAlbums.add(album);
        }
    }

    /**
     * A method that requests the cover image for an album from Cover Art Archives. This method first looks for if there
     * is a cover image with the field "front"=true that image will be chosen as the image, otherwise the first image in
     * the array will be chosen.
     * @param album The album in JSON format containing an "id"-field that is the id used for lookup.
     * @return If successfully fetched, this method returns a string of the link the cover image. If the fetch failed,
     * null will be returned.
     */
    private String getCoverImg(JSONObject album){
        String albumID = (String) album.get("id");
        JSONObject coverArtObject;
        JSONArray coverArts;

        try{
            URL url = new URL(coverArtBaseURL + "/release-group/" + albumID);
            coverArtObject = getJSONfromURL(url);
            if(coverArtObject == null){
                return null;
            }

            coverArts = (JSONArray) coverArtObject.get("images");

            for(Object cover : coverArts){
                JSONObject obj = (JSONObject) cover;
                if((boolean) obj.get("front")){
                    return (String) obj.get("image");
                }
            }
            JSONObject firstImage = (JSONObject) coverArts.get(0);
            return (String) firstImage.get("image");

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
