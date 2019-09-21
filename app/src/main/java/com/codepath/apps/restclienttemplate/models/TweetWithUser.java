package com.codepath.apps.restclienttemplate.models;

import androidx.room.Embedded;

import java.util.ArrayList;
import java.util.List;

public class TweetWithUser {

    // @Embedded notation flattens the properties of the User object into the object, preserving encapsulation.
    @Embedded
    User user;

    @Embedded(prefix = "tweet_")
    Tweet tweet;

    public String body;

    public static List<Tweet> getTweetList(List<TweetWithUser> tweetWithUserList) {
        List<Tweet> tweets = new ArrayList<>();
        for (int i = 0; i < tweetWithUserList.size(); i++) {
            TweetWithUser tweetWithUser = tweetWithUserList.get(i);
            Tweet tweet = tweetWithUser.tweet;
            tweet.user = tweetWithUser.user;
            tweets.add(tweet);
        }
        return tweets;
    }
}
