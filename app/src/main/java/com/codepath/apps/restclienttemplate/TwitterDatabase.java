package com.codepath.apps.restclienttemplate;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.TweetDao;
import com.codepath.apps.restclienttemplate.models.User;

@Database(entities={Tweet.class, User.class}, version=3)
public abstract class TwitterDatabase extends RoomDatabase {
    public abstract TweetDao tweetDao();

    // Database name to be used
    public static final String NAME = "TwitterDataBase";
}