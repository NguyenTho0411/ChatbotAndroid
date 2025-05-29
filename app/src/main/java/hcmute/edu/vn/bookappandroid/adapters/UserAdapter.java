package hcmute.edu.vn.bookappandroid.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.activities.ChatActivity;
import hcmute.edu.vn.bookappandroid.models.ModelUser;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.HolderUser> {

    private Context context;
    private List<ModelUser> userList;

    public UserAdapter(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public HolderUser onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new HolderUser(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderUser holder, int position) {
        ModelUser user = userList.get(position);
        holder.usernameTv.setText(user.getName());

        // Fetch last message, timestamp, and unread count
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String chatId = generateChatId(currentUserId, user.getUid());

        FirebaseDatabase.getInstance().getReference("Chats").child(chatId).child("Messages")
                .orderByChild("timestamp").limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String lastMessage = ds.child("message").getValue(String.class);
                                Long timestamp = ds.child("timestamp").getValue(Long.class);
                                Boolean isSeen = ds.child("seen").getValue(Boolean.class);

                                // Set last message
                                if (lastMessage != null && !lastMessage.isEmpty()) {
                                    holder.lastMessageTv.setText(lastMessage);
                                } else {
                                    String imageUri = ds.child("imageUri").getValue(String.class);
                                    holder.lastMessageTv.setText(imageUri != null && !imageUri.isEmpty() ? "Image" : "No messages yet");
                                }

                                // Set timestamp
                                if (timestamp != null) {
                                    holder.timeTv.setText(formatTimestamp(timestamp));
                                } else {
                                    holder.timeTv.setText("");
                                }

                                // Set unread count
                                FirebaseDatabase.getInstance().getReference("Chats").child(chatId).child("Messages")
                                        .orderByChild("seen").equalTo(false)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                long unreadCount = snapshot.getChildrenCount();
                                                if (unreadCount > 0) {
                                                    holder.unreadCountTv.setText(String.valueOf(unreadCount));
                                                    holder.unreadCountTv.setVisibility(View.VISIBLE);
                                                } else {
                                                    holder.unreadCountTv.setVisibility(View.GONE);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {}
                                        });
                            }
                        } else {
                            holder.lastMessageTv.setText("No messages yet");
                            holder.timeTv.setText("");
                            holder.unreadCountTv.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Handle click to open chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("otherUid", user.getUid());
            context.startActivity(intent);
        });

        // Handle message button click
        holder.messageButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("otherUid", user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private String generateChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    class HolderUser extends RecyclerView.ViewHolder {
        TextView usernameTv, lastMessageTv, timeTv, unreadCountTv;
        ImageView messageButton;

        HolderUser(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.usernameText);
            lastMessageTv = itemView.findViewById(R.id.lastMessageText);
            timeTv = itemView.findViewById(R.id.timeText);
            unreadCountTv = itemView.findViewById(R.id.unreadCountText);
            messageButton = itemView.findViewById(R.id.messageButton);
        }
    }
}