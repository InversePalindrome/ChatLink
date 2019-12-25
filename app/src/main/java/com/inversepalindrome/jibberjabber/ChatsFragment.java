/*
Copyright (c) 2019 Inverse Palindrome
Jibber Jabber - ChatsFragment.java
https://inversepalindrome.com/
*/


package com.inversepalindrome.jibberjabber;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;


public class ChatsFragment extends Fragment {
    private String senderID;
    private FirebaseDatabase database;
    private ArrayList<UserModel> userModelItems;
    private UserViewAdapter userViewAdapter;
    private EmptyRecyclerView userView;
    private TextView emptyStartChatText;
    private FloatingActionButton startChatButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chats, container, false);

        senderID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        database = FirebaseDatabase.getInstance();

        userModelItems = new ArrayList<>();

        userViewAdapter = new UserViewAdapter(R.layout.item_user, userModelItems, new OnClickListener() {
            @Override
            public void onClick(final View view) {
                int itemPosition = userView.getChildLayoutPosition(view);
                UserModel userModelItem = userModelItems.get(itemPosition);

                openChatActivity(userModelItem);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        userView = view.findViewById(R.id.chats_user_view);
        startChatButton = view.findViewById(R.id.chats_start_chat_button);
        emptyStartChatText = view.findViewById(R.id.chats_empty_start_chat_text);

        userView.setLayoutManager(linearLayoutManager);
        userView.setItemAnimator(new DefaultItemAnimator());
        userView.setAdapter(userViewAdapter);
        userView.setEmptyView(emptyStartChatText);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(userView.getContext(),
                linearLayoutManager.getOrientation());

        userView.addItemDecoration(dividerItemDecoration);

        UserItemCallback userItemCallback = new UserItemCallback(getContext(), new UserItemActions() {
            @Override
            public void onDeleteClicked(int position) {
                removeChatFromDatabase(userModelItems.get(position).uID);

                userModelItems.remove(position);
                userViewAdapter.notifyItemRemoved(position);
                userViewAdapter.notifyItemRangeChanged(position, userViewAdapter.getItemCount());
            }
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(userItemCallback);
        itemTouchHelper.attachToRecyclerView(userView);

        startChatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openStartChatDialog();
            }
        });
        emptyStartChatText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openStartChatDialog();
            }
        });

        loadChatFromDatabase();

        return view;
    }

    public void openStartChatDialog() {
        AlertDialog.Builder startChatDialogBuilder = new AlertDialog.Builder(getActivity(), R.style.CustomDialogTheme);
        startChatDialogBuilder.setTitle("Start Chat");

        final View startChatView = getLayoutInflater().inflate(R.layout.dialog_start_chat, null);
        startChatDialogBuilder.setView(startChatView);

        startChatDialogBuilder.setPositiveButton("Open Chat", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final EditText emailEntry = startChatView.findViewById(R.id.start_chat_email_entry);
                final String email = emailEntry.getText().toString().toLowerCase();

                DatabaseReference usersReference = database.getReference().child(Constants.DATABASE_USERS_NODE);

                usersReference.orderByChild(Constants.DATABASE_EMAIL_NODE).equalTo(email).
                        addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                                        UserModel receiverUserModel = childDataSnapshot.getValue(UserModel.class);

                                        addChatToDatabase(receiverUserModel.uID);
                                        openChatActivity(receiverUserModel);
                                    }
                                } else {
                                    Toast.makeText(getActivity(), "Email address not registered to Jibber Jabber!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
            }
        });

        startChatDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog startChatDialog = startChatDialogBuilder.create();
        startChatDialog.show();
    }

    private void openChatActivity(UserModel receiverUserModel) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("receiver", receiverUserModel);
        startActivity(intent);
    }

    private void addChatToDatabase(String receiverID) {
        String chatID = ChatIDCreator.getChatID(senderID, receiverID);

        DatabaseReference chatsReference = database.getReference().child(Constants.DATABASE_CHATS_NODE);
        DatabaseReference chatReference = chatsReference.child(chatID);

        DatabaseReference membersReference = chatReference.child(Constants.DATABASE_MEMBERS_NODE);
        membersReference.child(senderID).setValue(true);
        membersReference.child(receiverID).setValue(true);

        DatabaseReference usersChatsReference = database.getReference().child(Constants.DATABASE_USERS_CHATS_NODE);

        DatabaseReference senderChatsReference = usersChatsReference.child(senderID);
        senderChatsReference.child(chatID).setValue(true);

        DatabaseReference receiverChatsReference = usersChatsReference.child(receiverID);
        receiverChatsReference.child(chatID).setValue(true);

        DatabaseReference usersReference = database.getReference().child(Constants.DATABASE_USERS_NODE);
        DatabaseReference receiverReference = usersReference.child(receiverID);
        receiverReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                addUserModelToView(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void removeChatFromDatabase(String receiverID) {
        String chatID = ChatIDCreator.getChatID(senderID, receiverID);

        DatabaseReference usersChatsReference = database.getReference().child(Constants.DATABASE_USERS_CHATS_NODE);

        DatabaseReference senderChatsReference = usersChatsReference.child(senderID);
        senderChatsReference.child(chatID).removeValue();
    }

    private void addUserModelToView(@NonNull DataSnapshot userDataSnapshot) {
        final String receiverUsername = userDataSnapshot.child(
                Constants.DATABASE_USERNAME_NODE).getValue().toString();
        final String receiverEmail = userDataSnapshot.child(Constants.DATABASE_EMAIL_NODE)
                .getValue().toString();
        final String receiverProfileURI = userDataSnapshot.child(
                Constants.DATABASE_PROFILE_URI_NODE).getValue().toString();
        final String receiverStatus = userDataSnapshot.child(Constants.DATABASE_STATUS_NODE)
                .getValue().toString();

        userModelItems.add(new UserModel(userDataSnapshot.getKey(), receiverUsername,
                receiverEmail, receiverProfileURI, receiverStatus));

        userViewAdapter.notifyDataSetChanged();
    }

    private void loadChatFromDatabase() {
        DatabaseReference usersChatsReference = database.getReference().child(Constants.DATABASE_USERS_CHATS_NODE);

        DatabaseReference senderChatsReference = usersChatsReference.child(senderID);
        senderChatsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot chatDataSnapshot : dataSnapshot.getChildren()) {
                    DatabaseReference chatsReference = database.getReference().child(Constants.DATABASE_CHATS_NODE);
                    DatabaseReference chatReference = chatsReference.child(chatDataSnapshot.getKey());

                    DatabaseReference membersReference = chatReference.child(Constants.DATABASE_MEMBERS_NODE);
                    membersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                                String memberID = memberSnapshot.getKey();

                                if (!memberID.equals(senderID)) {
                                    DatabaseReference usersReference = database.getReference().child(Constants.DATABASE_USERS_NODE);
                                    DatabaseReference receiverReference = usersReference.child(memberID);

                                    receiverReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            addUserModelToView(dataSnapshot);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}