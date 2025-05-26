package hcmute.edu.vn.bookappandroid.models;

public class ModelUser {
    private String uid, name, email, userType;

    public ModelUser() {}

    public ModelUser(String uid, String name, String email, String userType) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.userType = userType;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUserType() { return userType; }

    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setUserType(String userType) { this.userType = userType; }
}
