package com.codepath.apps.restclienttemplate.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TweetDao {

    @Query("SELECT Tweet.body AS tweet_body, Tweet.createdAt as tweet_createdAt, " +
            "User.* FROM Tweet INNER JOIN User ON Tweet.userId = User.uid " +
            "ORDER BY Tweet.uid DESC LIMIT 5")
    List<TweetWithUser> recentItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertModel(Tweet... tweet);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertModel(User... user);
}
