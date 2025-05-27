package hcmute.edu.vn.bookappandroid.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new HolderUser(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderUser holder, int position) {
        ModelUser user = userList.get(position);
        holder.usernameTv.setText(user.getName());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("otherUid", user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class HolderUser extends RecyclerView.ViewHolder {
        TextView usernameTv;

        HolderUser(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.usernameTv);
        }
    }
}
