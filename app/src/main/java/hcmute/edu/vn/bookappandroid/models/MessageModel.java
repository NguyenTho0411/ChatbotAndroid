package hcmute.edu.vn.bookappandroid.models;

public class MessageModel {
    private String receiverId;
    private String senderId;
    private String message;
    private Long timestamp;

    // Constructor rỗng bắt buộc cho Firebase
    public MessageModel() {
    }

    public MessageModel(String senderId, String receiverId, String message, Long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
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
    }
}
