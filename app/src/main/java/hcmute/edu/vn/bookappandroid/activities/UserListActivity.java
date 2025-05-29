package hcmute.edu.vn.bookappandroid.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.adapters.UserAdapter;
import hcmute.edu.vn.bookappandroid.models.ModelUser;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ModelUser> userList;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList);
        recyclerView.setAdapter(userAdapter);

        loadUsersWithChats();
    }

    private void loadUsersWithChats() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        Set<String> userIds = new HashSet<>();

        // Fetch all chat rooms involving the current user
        database.getReference("Chats")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userIds.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String chatId = ds.getKey();
                            String[] uids = chatId.split("_");
                            String otherUid = uids[0].equals(currentUserId) ? uids[1] : uids[0];
                            userIds.add(otherUid);
                        }

                        // Fetch user details for the identified users
                        if (!userIds.isEmpty()) {
                            database.getReference("Users")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            userList.clear();
                                            for (DataSnapshot ds : snapshot.getChildren()) {
                                                ModelUser user = ds.getValue(ModelUser.class);
                                                if (user != null && userIds.contains(user.getUid()) && "user".equals(user.getUserType())) {
                                                    userList.add(user);
                                                }
                                            }
                                            userAdapter.notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        } else {
                            userList.clear();
                            userAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}