package hcmute.edu.vn.bookappandroid.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.ChatAdapter;
import hcmute.edu.vn.bookappandroid.databinding.ActivityChatBinding;
import hcmute.edu.vn.bookappandroid.models.MessageModel;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private String myUid, otherUid, chatKey;
    private DatabaseReference chatRef;

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageView btnSend, btnEmoji, btnAttach;

    private List<MessageModel> messageList;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        myUid = FirebaseAuth.getInstance().getUid();
        otherUid = getIntent().getStringExtra("otherUid");

        // Tạo key chung cho cuộc trò chuyện
        chatKey = myUid.compareTo(otherUid) < 0 ? myUid + "_" + otherUid : otherUid + "_" + myUid;
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatKey).child("messages");

        // Ánh xạ View
        chatRecyclerView = binding.chatRecyclerView;
        messageEditText = binding.messageEditText;
        btnSend = binding.btnSend;
        btnEmoji = binding.btnEmoji;
        btnAttach = binding.btnAttach;

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, myUid);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Mặc định nút gửi bị disable, chỉ bật khi có nội dung
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
        loadReceiverInfo(otherUid); // Load tên đối phương

        // Tạm thời chưa xử lý emoji và đính kèm
        btnEmoji.setOnClickListener(v -> {
            // Mở trình chọn emoji nếu bạn dùng thư viện
        });

        btnAttach.setOnClickListener(v -> {
            // Mở trình chọn ảnh/tệp nếu cần
        });
    }

    private void sendMessage() {
        String msg = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        String messageId = chatRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        MessageModel model = new MessageModel(myUid, otherUid, msg, timestamp);
        chatRef.child(messageId).setValue(model);

        messageEditText.setText("");
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    MessageModel model = ds.getValue(MessageModel.class);
                    messageList.add(model);
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void loadReceiverInfo(String receiverUid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    binding.nameTv.setText(name); // Đặt tên người nhận lên toolbar
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Không thể tải tên người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
