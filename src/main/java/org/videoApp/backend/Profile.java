package org.videoApp.backend;

public class Profile {
    private String email, name, picture;


    public Profile(final String email, final String name, final String picture) {
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public String getEmail() {
        return email;
    }

    public String getPicture() {
        return picture;
    }

    public String getName() {
        return name;
    }
}
