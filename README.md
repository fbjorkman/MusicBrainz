# MusicSearch application
The MBID is an identifier at MusicBrainz used to indentify different kind of 
entities in their database. These are the entities that currently are supported:
area, artist, event, genre, instrument, label, place, recording, release, release-group, series, work, url.
In this application only artist search will be implemented partly because of the time limit, but also that is
what I understood the assignment to be.

The application will run on your loopback network interface "localhost" on port 8080.
A very simple index.html file is located in the application root which enables the user to search a MBID from
the localhost root: localhost:8080/ 

### Build essentials
Java and Maven are required to be installed on your system to build and run the application.
Not entirely sure what the minimum requirements are to build this application, but I have built it with 
the following software versions:
* Maven 3.8.4 
* Java 17.0.3

### How to run the application
Here is how to run the application on linux.
1. Extract the zip-file
2. In the terminal, move into the project root folder
3. In the terminal, run: mvn spring-boot:run
4. Open your browser and visit: localhost:8080
5. Enter the MBID of an artist or a band in the search box
6. Hit Search!
7. To make a new search, visit localhost:8080 again (there is no return button implemented)

### Some assumptions: 
All JSON responses from MusicBrainz always contain a "name" and "relations"-field.
The relation-field is an JSON array which will contain an object of typ "wikidata" and sometimes
also contain an object of typ "wikipedia" All cite link to wikipedia is on the form:
"https://en.wikipedia.org/wiki/ARTIST_NAME" and all cite links to wikidata is on the form:
"https://www.wikidata.org/wiki/IDENTIFIER". This assumption is made as to extract the 
name of the artist and the identifier, the index from that position in the above string are used.
Another assumption is that the description in the JSON response from Wikipedia will be found
by querying the following keys recursively: "query" -> "pages" -> PAGE_ID -> "extract".
And to extract the artist/band name that is used by english wikipedia from wikidata, the following 
keys are queried: "entities" -> WIKIDATA_ID -> "sitelinks" -> "enwiki" -> "title".
If no links to wikidata or wikipedia is found, the "description"-field will be null.
The JSON response from the Cover Art Archive is assumed to contain a JSON array under the key "images". 
All elements in that array hopefully contains a boolean "front", which tells if the cover image is regarded 
as the front cover for the album, and an "image"-key which is the link to the cover image.
The image field under albums will also be null if the coverArt image cannot be found.

### Note
In the JSON response from the application, the link to the album cover image will have escaped 
forward slashes, example: 
"image":"http:\/\/coverartarchive.org\/release\/b6d56ec3-4bb6-3c07-babf-654276b0e30c\/913607156.jpg". 
This happens (I think) in the put-method in the json-simple repository, it allows JSON to be embedded
in for example <script>-tags. If reading the JSON using java-script, this is not a problem as the 
"\/" will be red as "/" but if using Java this might need to be handled.
