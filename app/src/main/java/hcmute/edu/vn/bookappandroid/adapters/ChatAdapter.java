package hcmute.edu.vn.bookappandroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.bookappandroid.R;
import hcmute.edu.vn.bookappandroid.models.MessageModel;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<MessageModel> messageList;
    private String myUid;

    public ChatAdapter(Context context, List<MessageModel> messageList, String myUid) {
        this.context = context;
        this.messageList = messageList;
        this.myUid = myUid;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel msg = messageList.get(position);
        if (msg.getSenderId() != null && msg.getSenderId().equals(myUid)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_right, parent, false);
            return new RightViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_left, parent, false);
            return new LeftViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = messageList.get(position);
        if (holder instanceof RightViewHolder) {
            ((RightViewHolder) holder).messageTv.setText(msg.getMessage());
        } else if (holder instanceof LeftViewHolder) {
            ((LeftViewHolder) holder).messageTv.setText(msg.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class LeftViewHolder extends RecyclerView.ViewHolder {
        TextView messageTv;

        LeftViewHolder(View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.messageTv);
        }
    }

    class RightViewHolder extends RecyclerView.ViewHolder {
        TextView messageTv;

        RightViewHolder(View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.messageTv);
        }
    }
}
