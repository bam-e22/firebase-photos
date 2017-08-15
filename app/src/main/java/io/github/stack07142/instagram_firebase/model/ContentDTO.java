package io.github.stack07142.instagram_firebase.model;

public class ContentDTO {

    public String explain;
    public String imageUrl;
    public String uid;
    public String userId;
    public String timestamp;

    @Override
    public String toString() {
        return "uid = " + uid + " , userid = " + userId;
    }
}
