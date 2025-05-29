package hcmute.edu.vn.bookappandroid.models;

public class MessageModel {
    private String receiverId;
    private String senderId;
    private String message;
    private Long timestamp;
    private String imageUri; // ✅ ảnh (nếu có)
    // Constructor rỗng bắt buộc cho Firebase

    private boolean isSeen = false;

    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }

    public MessageModel() {
    }
    public MessageModel(String senderId, String receiverId, String message, long timestamp, String imageUri, boolean isSeen) {
        this(senderId, receiverId, message, timestamp, imageUri);
        this.isSeen = isSeen;
    }

    public MessageModel(String senderId, String receiverId, String message, Long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }
    public MessageModel(String senderId, String receiverId, String message, long timestamp, String imageUri) {
        this(senderId, receiverId, message, timestamp);
        this.imageUri = imageUri;
    }
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

}
