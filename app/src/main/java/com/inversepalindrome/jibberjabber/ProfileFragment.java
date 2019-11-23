/*
Copyright (c) 2019 Inverse Palindrome
Jibber Jabber - ProfileFragment.java
https://inversepalindrome.com/
*/


package com.inversepalindrome.jibberjabber;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
}
