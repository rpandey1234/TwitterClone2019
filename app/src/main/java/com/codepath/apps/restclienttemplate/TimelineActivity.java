package com.codepath.apps.restclienttemplate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.TweetDao;
import com.codepath.apps.restclienttemplate.models.TweetWithUser;
import com.codepath.apps.restclienttemplate.models.User;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class TimelineActivity extends AppCompatActivity {

    private static final String TAG = "TimelineActivity";
    // REQUEST_CODE can be any value we like, used to determine the result type later
    private final int REQUEST_CODE = 20;

    TwitterClient twitterClient;
    RecyclerView rvTweets;
    TweetsAdapter adapter;
    List<Tweet> tweets;
    TweetDao tweetDao;
    SwipeRefreshLayout swipeContainer;
    EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        twitterClient = new TwitterClient(this);

        // Find the recycler view
        rvTweets = findViewById(R.id.rvTweets);
        // Init the adapter and list of tweets
        tweets = new ArrayList<>();
        adapter = new TweetsAdapter(this, tweets);
        // Recycler view setup: layout manager and setup manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(layoutManager);
        rvTweets.setAdapter(adapter);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadMoreData();
            }
        };
        rvTweets.addOnScrollListener(scrollListener);
        swipeContainer = findViewById(R.id.swipeContainer);
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "fetching new data!");
                populateHomeTimeline();
            }
        });

        tweetDao = ((TwitterApp) getApplicationContext()).getTwitterDatabase().tweetDao();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<TweetWithUser> tweetsFromDatabase = tweetDao.recentItems();
                adapter.clear();
                Log.i(TAG, "Showing data from database");
                List<Tweet> tweetList = TweetWithUser.getTweetList(tweetsFromDatabase);
                adapter.addAll(tweetList);
            }
        });
        populateHomeTimeline();
    }

    private void loadMoreData() {
        Log.i(TAG, "loadMoreData");
        // 1. Send an API request to retrieve appropriate paginated data
        twitterClient.getNextPageOfTweets(tweets.get(tweets.size() - 1).uid, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "Success for loadMoreData: " + tweets.size());
                // 2. Deserialize and construct new model objects from the API response
                // 3. Append the new data objects to the existing set of items inside the array of items
                // 4. Notify the adapter of the new items made with `notifyItemRangeInserted()`
                try {
                    final List<Tweet> freshTweets = Tweet.fromJsonArray(json.jsonArray);
                    Log.i(TAG, "Going to add this many tweets: " + freshTweets.size());
                    adapter.addAll(freshTweets);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException with load more data", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure for loading more tweets", throwable);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.compose) {
            // Tapped on compose icon
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // Pull data out of the data intent (tweet)
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra("tweet"));
            tweets.add(0, tweet);
            // Update the recycler view to show this tweet.
            adapter.notifyItemInserted(0);
            rvTweets.smoothScrollToPosition(0);
        }
    }

    private void populateHomeTimeline() {
        twitterClient.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.d(TAG, "onSuccess");
                try {
                    adapter.clear();
                    final List<Tweet> freshTweets = Tweet.fromJsonArray(json.jsonArray);
                    final List<User> freshUsers = User.fromJsonTweetArray(json.jsonArray);
                    adapter.addAll(freshTweets);
                    // TODO: there should be some mechanism to delete content from the SQL lite DB periodically
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            ((TwitterApp) getApplicationContext()).getTwitterDatabase().runInTransaction(new Runnable() {
                                @Override
                                public void run() {
                                    tweetDao.insertModel(freshUsers.toArray(new User[0]));
                                    tweetDao.insertModel(freshTweets.toArray(new Tweet[0]));
                                }
                            });
                        }
                    });

                    // Now we call setRefreshing(false) to signal refresh has finished
                    swipeContainer.setRefreshing(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure", throwable);
                Log.e(TAG, "onFailure " + statusCode);
            }
        });
    }
}
