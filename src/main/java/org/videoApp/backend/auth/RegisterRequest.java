package org.videoApp.backend.auth;


public class RegisterRequest {
    private String email, firstName, lastName, password;


    public RegisterRequest(final String email, final String firstName, final String lastName, final String password) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }
}
