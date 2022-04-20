package com.example.musicsearch;

import org.json.simple.parser.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;

@SpringBootApplication
@RestController
public class MusicSearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(MusicSearchApplication.class, args);
	}

	@GetMapping("/search")
	public String search(@RequestParam(value = "mbid") String mbid) throws IOException, ParseException {
    	MusicBrainzObject searchObject = new MusicBrainzObject(mbid, Entity.ARTIST);
		return searchObject.lookup();
	}

}
