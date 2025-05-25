package hcmute.edu.vn.bookappandroid.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

import hcmute.edu.vn.bookappandroid.MyApplication;
import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.databinding.RowCommentBinding;
import hcmute.edu.vn.bookappandroid.models.ModelComment;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.HolderComment> {

    private Context context;
    private ArrayList<ModelComment> commentArrayList;
    private FirebaseAuth firebaseAuth;

    public AdapterComment(Context context, ArrayList<ModelComment> commentArrayList) {
        this.context = context;
        this.commentArrayList = commentArrayList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderComment onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowCommentBinding binding = RowCommentBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderComment(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderComment holder, int position) {
        ModelComment modelComment = commentArrayList.get(position);
        String comment = modelComment.getComment();
        String timestamp = modelComment.getTimestamp();
        String uid = modelComment.getUid();

        holder.binding.commentTv.setText(comment);
        holder.binding.dateTv.setText(MyApplication.formatTimestamp(Long.parseLong(timestamp)));

        loadUserDetails(modelComment, holder);

        holder.itemView.setOnClickListener(view -> {
            if (firebaseAuth.getCurrentUser() != null && uid.equals(firebaseAuth.getUid())) {
                deleteComment(modelComment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentArrayList.size();
    }

    class HolderComment extends RecyclerView.ViewHolder {
        RowCommentBinding binding;

        public HolderComment(@NonNull RowCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private void loadUserDetails(ModelComment modelComment, HolderComment holder) {
        String uid = modelComment.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = "" + snapshot.child("name").getValue();
                String profileImage = "" + snapshot.child("profileImage").getValue();

                holder.binding.nameTv.setText(name);
                try {
                    Glide.with(context)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_personal_gray)
                            .into(holder.binding.profileIv);
                } catch (Exception e) {
                    holder.binding.profileIv.setImageResource(R.drawable.ic_personal_gray);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteComment(ModelComment modelComment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa bình luận này không?")
                .setPositiveButton("XÓA", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
                        ref.child(modelComment.getBookId())
                                .child("Comments")
                                .child(modelComment.getId())
                                .removeValue()
                                .addOnSuccessListener(unused -> Toast.makeText(context, "Đã xóa bình luận", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(context, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("HỦY", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }
}
