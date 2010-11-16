package com.bbcnewsreader;

import java.util.ArrayList;

import com.bbcnewsreader.data.DatabaseHandler;
import com.bbcnewsreader.resource.rss.RSSItem;
import com.bbcnewsreader.resource.rss.RSSManager;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ResourceService extends Service implements ResourceInterface {
	/* variables */
	ArrayList<Messenger> clients = new ArrayList<Messenger>(); //holds references to all of our clients
	final Messenger messenger = new Messenger(new IncomingHandler()); //the messenger used for communication
	DatabaseHandler database; //the database
	RSSManager rssManager;
	
	/* command definitions */
	static final int MSG_REGISTER_CLIENT_WITH_DATABASE = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_CLIENT_REGISTERED = 3; //returned to a client when registered
	static final int MSG_LOAD_DATA = 4; //sent to request a data load
	static final int MSG_CATEOGRY_LOADED = 6; //sent when a category has loaded
	static final int MSG_ERROR = 7; //help! An error occurred
	
	//the handler class to process new messages
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg){
			//decide what to do with the message
			switch(msg.what){
			case MSG_REGISTER_CLIENT_WITH_DATABASE:
				clients.add(msg.replyTo); //add a reference to the client to our list
				sendMsg(msg.replyTo, MSG_CLIENT_REGISTERED, null);
				break;
			case MSG_UNREGISTER_CLIENT:
				clients.remove(msg.replyTo); //remove our reference to the client
				//check if that was our last client
				if(clients.size() == 0)
					System.exit(0);
				break;
			case MSG_LOAD_DATA:
				loadData(); //start of the loading of data
				break;
			default:
				super.handleMessage(msg); //we don't know what to do, lets hope that the super class knows
			}
		}
	}
	
	public class ResourceBinder extends Binder {
		ResourceService getService(){
			return ResourceService.this;
		}
	}
	
	public synchronized void setDatabase(DatabaseHandler db){
		this.database = db;
	}
	
	public synchronized DatabaseHandler getDatabase(){
		return database;
	}
	
	void loadData(){
		//retrieve the active category urls
		String[] urls = getDatabase().getEnabledCategories();
		//work out the names
		String[] names = new String[urls.length];
		String[] allNames = getResources().getStringArray(R.array.category_names);
		String[] allUrls = getResources().getStringArray(R.array.catergory_rss_urls);
		//FIXME very inefficient, should be done by database
		for(int i = 0; i < allUrls.length; i++){
			for(int j = 0; j < urls.length; j++){
				if(allUrls[i].equals(urls[j])){
					names[j] = allNames[i];
				}
			}
		}
		//start the RSS Manager
		rssManager = new RSSManager(names, urls, this);
	}
	
	void sendMsg(Messenger client, int what, Object object){
		try{
			//create a message according to parameters
			Message msg = Message.obtain(null, what, object);
			client.send(msg); //send the message
		}
		catch(RemoteException e){
			//We are probably shutting down, but report it anyway
			Log.e("ERROR", "Unable to send message to client: " + e.getMessage());
		}
	}
	
	void sendMsg(int clientId, int what, Object object){
		//simply call the main sendMessage but with an actual client
		sendMsg(clients.get(clientId), what, object);
	}
	
	void sendMsgToAll(int what, Object object){
		//loop through and send the message to all the clients
		for(int i = 0; i < clients.size(); i++){
			sendMsg(i, what, object);
		}
	}
	
	/**
	 * Called when an RSS feed has loaded
	 * @param item The item that has been loaded */
	public synchronized void itemRssLoaded(RSSItem item, String category){
		//insert the item into the database
		//FIXME no description given
		getDatabase().insertItem(item.getTitle(), null, item.getLink(), item.getPubDate(), category);
		//TODO tell the web manager to load this item's web page
	}
	
	public synchronized void reportError(boolean fatal, String msg){
		//an error has occurred, send a message to the gui
		//this will display something useful to the user
		String[] msgs = {Boolean.toString(fatal), msg};
		sendMsgToAll(MSG_ERROR, msgs);
	}
	
	@Override
	public void onCreate(){
		//create the database if needed
		if(database == null){
			//load the database
			setDatabase(new DatabaseHandler(this));
		}
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //We want to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }
	
	@Override
	public void onDestroy(){
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

}