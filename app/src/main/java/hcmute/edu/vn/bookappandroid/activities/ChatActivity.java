package hcmute.edu.vn.bookappandroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.ChatAdapter;
import hcmute.edu.vn.bookappandroid.databinding.ActivityChatBinding;
import hcmute.edu.vn.bookappandroid.models.MessageModel;

public class ChatActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_PICK = 200;

    private ActivityChatBinding binding;
    private String myUid, otherUid, chatKey;
    private DatabaseReference chatRef;

    private List<MessageModel> messageList;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        myUid = FirebaseAuth.getInstance().getUid();
        otherUid = getIntent().getStringExtra("otherUid");

        chatKey = (myUid.compareTo(otherUid) < 0) ? myUid + "_" + otherUid : otherUid + "_" + myUid;
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatKey).child("messages");

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, myUid);
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.chatRecyclerView.setAdapter(chatAdapter);

        binding.messageEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.btnAttach.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        loadMessages();
        loadReceiverInfo(otherUid);
    }

    private void sendMessage() {
        String msg = binding.messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        String messageId = chatRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        MessageModel model = new MessageModel(myUid, otherUid, msg, timestamp);
        model.setSeen(false);
        chatRef.child(messageId).setValue(model);
        binding.messageEditText.setText("");
    }

    private void uploadImageMessage(Uri imageUri) {
        String messageId = chatRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        StorageReference ref = FirebaseStorage.getInstance().getReference("ChatImages/" + messageId + ".jpg");
        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            MessageModel msg = new MessageModel(myUid, otherUid, null, timestamp, uri.toString());
            msg.setSeen(false);
            chatRef.child(messageId).setValue(msg);
        })).addOnFailureListener(e ->
                Toast.makeText(this, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            uploadImageMessage(data.getData());
        }
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    MessageModel model = ds.getValue(MessageModel.class);
                    if (model != null) {
                        messageList.add(model);
                        if (model.getReceiverId().equals(myUid) && !model.isSeen()) {
                            ds.getRef().child("seen").setValue(true);
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
                binding.chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("ChatDebug", "Lỗi Firebase: " + error.getMessage());
            }
        });
    }

    private void loadReceiverInfo(String receiverUid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    binding.nameTv.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Không thể tải tên người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
