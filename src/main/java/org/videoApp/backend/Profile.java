package org.videoApp.backend;

public class Profile {
    private String id, email, name, picture;


    public Profile(final String id, final String email, final String name, final String picture) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public String getId() {
        return id;
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
