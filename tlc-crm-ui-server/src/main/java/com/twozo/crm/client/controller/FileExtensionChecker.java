package com.twozo.crm.client.controller;

import com.twozo.crm.client.models.header.WebHeadersProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Pattern;

public class FileExtensionChecker {

    @Autowired
    private WebHeadersProperties webHeadersProperties;

    public static void main(String[] args) {
        String regex = "(?:.*\\/)?([^\\/]+\\.(txt|html|jpg|png|json|ico|js|woff2|css))$";
        Pattern pattern = Pattern.compile(regex);

        // Example file paths
        String[] fileNames = {
                "a.json",
                "/a.json",
                "/a/b.png",
                "/a/b/c.png",
                "/a/b/c/d.png",
                "/some/path/file.txt",
                "/a/b/c/file.pdf"
        };

        for (String fileName : fileNames) {
            if (pattern.matcher(fileName).find()) {
                System.out.println(fileName + " -> Valid extension");
            } else {
                System.out.println(fileName + " -> Invalid extension");
            }
        }

    }
}

