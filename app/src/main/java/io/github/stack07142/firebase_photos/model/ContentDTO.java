package io.github.stack07142.firebase_photos.model;

import java.util.HashMap;
import java.util.Map;

public class ContentDTO {

    public String explain;
    public String imageUrl;
    public String uid;
    public String userId;
    public String timestamp;
    public int favoriteCount = 0;
    public Map<String, Boolean> favorites = new HashMap<>();
    public Map<String, Comment> comments;

    public static class Comment {

        public String uid;
        public String userId;
        public String comment;
    }

    @Override
    public String toString() {
        return "uid = " + uid + " , userid = " + userId;
    }
}
