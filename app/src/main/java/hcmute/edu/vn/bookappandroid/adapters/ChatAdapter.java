package hcmute.edu.vn.bookappandroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.models.MessageModel;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private final Context context;
    private final List<MessageModel> messageList;
    private final String myUid;

    public ChatAdapter(Context context, List<MessageModel> messageList, String myUid) {
        this.context = context;
        this.messageList = messageList;
        this.myUid = myUid;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSenderId().equals(myUid) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_right, parent, false);
            return new RightMessageHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_left, parent, false);
            return new LeftMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        String timeText = formatTimestamp(message.getTimestamp());

        if (holder instanceof RightMessageHolder) {
            RightMessageHolder h = (RightMessageHolder) holder;

            if (message.getMessage() != null) {
                h.messageTv.setText(message.getMessage());
                h.messageTv.setVisibility(View.VISIBLE);
            } else {
                h.messageTv.setVisibility(View.GONE);
            }

            if (message.getImageUri() != null && !message.getImageUri().trim().isEmpty()) {
                h.imageMessage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUri())
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_error)
                        .into(h.imageMessage);
            } else {
                h.imageMessage.setVisibility(View.GONE);
            }


            h.timeTv.setText(timeText);

            if (position == messageList.size() - 1) {
                h.statusIcon.setImageResource(message.isSeen() ? R.drawable.ic_check_double : R.drawable.ic_check);
                h.statusIcon.setColorFilter(context.getResources().getColor(R.color.message_status_color));
                h.statusIcon.setVisibility(View.VISIBLE);
            } else {
                h.statusIcon.setVisibility(View.GONE);
            }

        } else if (holder instanceof LeftMessageHolder) {
            LeftMessageHolder h = (LeftMessageHolder) holder;

            if (message.getMessage() != null) {
                h.messageTv.setText(message.getMessage());
                h.messageTv.setVisibility(View.VISIBLE);
            } else {
                h.messageTv.setVisibility(View.GONE);
            }

            if (message.getImageUri() != null && !message.getImageUri().trim().isEmpty()) {
                h.imageMessage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUri())
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_error)
                        .into(h.imageMessage);
            } else {
                h.imageMessage.setVisibility(View.GONE);
            }

            h.timeTv.setText(timeText);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class RightMessageHolder extends RecyclerView.ViewHolder {
        TextView messageTv, timeTv;
        ImageView imageMessage, statusIcon;

        public RightMessageHolder(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            imageMessage = itemView.findViewById(R.id.imageMessage);
            statusIcon = itemView.findViewById(R.id.statusIcon);
        }
    }

    public static class LeftMessageHolder extends RecyclerView.ViewHolder {
        TextView messageTv, timeTv;
        ImageView imageMessage;

        public LeftMessageHolder(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.messageTv);
            imageMessage = itemView.findViewById(R.id.imageMessage);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}
