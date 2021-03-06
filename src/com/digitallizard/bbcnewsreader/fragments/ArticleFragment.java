package com.digitallizard.bbcnewsreader.fragments;

import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.digitallizard.bbcnewsreader.R;
import com.digitallizard.bbcnewsreader.ResourceService;
import com.digitallizard.bbcnewsreader.ServiceManager;
import com.digitallizard.bbcnewsreader.ServiceManager.MessageReceiver;
import com.digitallizard.bbcnewsreader.data.DatabaseHandler;
import com.digitallizard.bbcnewsreader.resource.web.HtmlParser;

public class ArticleFragment extends Fragment implements MessageReceiver {
	private static final int ID_NO_ARTICLE_LOADED = -10;
	
	private DatabaseHandler database;
	private ServiceManager service;
	
	private int itemId;
	private WebView webView;
	private TextView loadingText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		database = new DatabaseHandler(getActivity());
		service = new ServiceManager(getActivity(), this);
		
		itemId = ID_NO_ARTICLE_LOADED;
		
		service.doBindService();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.article, container, false);
		
		// create references to views
		loadingText = (TextView) view.findViewById(R.id.articleLoadingText);
		webView = (WebView) view.findViewById(R.id.articleWebView);
		
		webView.loadDataWithBaseURL(null, "Please select an article.", "text/html", "utf-8", null);
		
		return view;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		service.doUnbindService();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.articleMenuItemReload) {
			// reload the current article
			loadArticle(itemId);
		}
		return true; // we have received the press so we can report true
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		// inflate the menu XML file
		inflater.inflate(R.menu.article_menu, menu);
	}
	
	public void displayArticle(int id) {
		itemId = id;
		
		if (itemId == ID_NO_ARTICLE_LOADED) {
			return;
		}
		
		// try to get the article, display or load it
		byte[] html = database.getHtml(id);
		if (html != null) {
			showArticle(html);
		}
		else {
			loadArticle(id);
		}
	}
	
	private void showArticle(byte[] html) {
		// parse the html, load it in the webview
		String parsedHtml = HtmlParser.parsePage(html);
		webView.loadDataWithBaseURL(null, parsedHtml, "text/html", "utf-8", null);
		
		// swap the loading text and webview
		loadingText.setVisibility(View.GONE);
		webView.setVisibility(View.VISIBLE);
		
		// scroll the webview up to the top
		webView.scrollTo(0, 0);
	}
	
	public void loadArticle(int id) {
		itemId = id;
		
		if (itemId == ID_NO_ARTICLE_LOADED) {
			return;
		}
		
		// make the loading text visible
		loadingText.setVisibility(View.VISIBLE);
		webView.setVisibility(View.GONE);
		
		// load the article, it will be displayed when it is returned by the service
		Bundle bundle = new Bundle();
		bundle.putInt(ResourceService.KEY_ITEM_ID, itemId);
		service.sendMessageToService(ResourceService.MSG_LOAD_ARTICLE, bundle);
	}
	
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case ResourceService.MSG_ARTICLE_LOADED:
			// display the reloaded article
			displayArticle(itemId);
			break;
		}
	}
}
