package io.odoc.webintent;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import java.io.File;
import java.io.IOException;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.webkit.MimeTypeMap;
import android.provider.MediaStore;
import android.database.Cursor;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;

public class WebIntent extends CordovaPlugin {

    private CallbackContext onNewIntentCallbackContext = null;

    //public boolean execute(String action, JSONArray args, String callbackId) {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {

            if (action.equals("startActivity")) {
                if (args.length() == 0) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                // Parse the arguments
				final CordovaResourceApi resourceApi = webView.getResourceApi();
                JSONObject obj = args.getJSONObject(0);
                String type = obj.has("type") ? obj.getString("type") : null;
                Uri uri = obj.has("url") ? resourceApi.remapUri(Uri.parse(obj.getString("url"))) : null;
                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                startActivity(obj.getString("action"), uri, type, extrasMap);
                //return new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;

            } else if (action.equals("hasExtra")) {
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                String extraName = args.getString(0);
                //return new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName));
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName)));
                return true;

            } else if (action.equals("getExtra")) {
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                
                Intent intent = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                String intentAction = intent.getAction();
                String intentType = intent.getType();
                
                JSONObject obj = new JSONObject();
                JSONObject extra = new JSONObject();
                
                if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {

                    if ("text/plain".equals(intentType)) {
                        //extra.put("content", handleSendText(intent));
                        handleSendText(intent, extra);
                    } else { //else if (type.startsWith("image/")) {
                        //extra.put("content", handleSendFile(intent, ));
                        handleSendFile(intent, extra);
                    }

                    extra.put("type", intentType);
                    obj.put("extras", extra);
                    
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction) && intentType != null) {
                    
                    //if (intentType.startsWith("image/")) {
                        obj.put("extras", handleSendMultipleImages(intent));
                    //}
                    
                } else {
                    //return new PluginResult(PluginResult.Status.ERROR);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    return false;
                }
                
                
                
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, obj.toString()));
                
                return true;
                /*
                String extraName = args.getString(0);
                if (i.hasExtra(extraName)) {
                    //return new PluginResult(PluginResult.Status.OK, i.getStringExtra(extraName));
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, i.getStringExtra(extraName)));
                    return true;
                } else {
                    //return new PluginResult(PluginResult.Status.ERROR);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    return false;
                }*/
            } else if (action.equals("getUri")) {
                if (args.length() != 0) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                Intent i = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                String uri = i.getDataString();
                //return new PluginResult(PluginResult.Status.OK, uri);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, uri));
                return true;
            } else if (action.equals("onNewIntent")) {
            	//save reference to the callback; will be called on "new intent" events
                this.onNewIntentCallbackContext = callbackContext;
        
                if (args.length() != 0) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                
                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true); //re-use the callback on intent events
                callbackContext.sendPluginResult(result);
                return true;
                //return result;
            } else if (action.equals("sendBroadcast")) 
            {
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                // Parse the arguments
                JSONObject obj = args.getJSONObject(0);

                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                sendBroadcast(obj.getString("action"), extrasMap);
                //return new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;
            }
            //return new PluginResult(PluginResult.Status.INVALID_ACTION);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            String errorMessage=e.getMessage();
            //return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION,errorMessage));
            return false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    	 
        if (this.onNewIntentCallbackContext != null) {
        	PluginResult result = new PluginResult(PluginResult.Status.OK, intent.getDataString());
        	result.setKeepCallback(true);
            this.onNewIntentCallbackContext.sendPluginResult(result);
        }
    }

    void startActivity(String action, Uri uri, String type, Map<String, String> extras) {
        Intent i = (uri != null ? new Intent(action, uri) : new Intent(action));
        
        if (type != null && uri != null) {
            i.setDataAndType(uri, type); //Fix the crash problem with android 2.3.6
        } else {
            if (type != null) {
                i.setType(type);
            }
        }
        
        for (String key : extras.keySet()) {
            String value = extras.get(key);
            // If type is text html, the extra text must sent as HTML
            if (key.equals(Intent.EXTRA_TEXT) && type.equals("text/html")) {
                i.putExtra(key, Html.fromHtml(value));
            } else if (key.equals(Intent.EXTRA_STREAM)) {
                // allowes sharing of images as attachments.
                // value in this case should be a URI of a file
				final CordovaResourceApi resourceApi = webView.getResourceApi();
                i.putExtra(key, resourceApi.remapUri(Uri.parse(value)));
            } else if (key.equals(Intent.EXTRA_EMAIL)) {
                // allows to add the email address of the receiver
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { value });
            } else {
                i.putExtra(key, value);
            }
        }
        ((CordovaActivity)this.cordova.getActivity()).startActivity(i);
    }

    void sendBroadcast(String action, Map<String, String> extras) {
        Intent intent = new Intent();
        intent.setAction(action);
        for (String key : extras.keySet()) {
            String value = extras.get(key);
            intent.putExtra(key, value);
        }

        ((CordovaActivity)this.cordova.getActivity()).sendBroadcast(intent);
    }
    
    void handleSendText(Intent intent, JSONObject extras) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        long size = 0L;
        
        if (sharedText != null) {
            size = sharedText.length();
        }
        
        try {
            extras.put("content", sharedText);
            extras.put("size", size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void handleSendFile(Intent intent, JSONObject extras) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String path = "";
        long size = 0L;
        try {
            if (uri != null) {
                // Update UI to reflect image being shared
                File file = new File(getRealPathFromURI(uri));
                if (file.exists()) {
                    size = file.length();
                    path = file.getCanonicalPath();
                }
                else {
                    path = "null";
                    size = 0;
                }
            }
            
            extras.put("content", path);
            extras.put("size", size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = this.cordova.getActivity().getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
    
    JSONArray handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        JSONArray extras = new JSONArray();

        try {
            if (uris != null) {
            
                for(int i = 0; i < uris.size(); i++) {
                
                    JSONObject obj = new JSONObject();
                    String uri = uris.get(i).getPath();

                    obj.put("type", getMimeType(uri));
                    obj.put("content", uri);
                
                    extras.put(obj);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return extras;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        } /*else {
            File f = new File(url);
            type = new MimetypesFileTypeMap().getContentType(f);
        }*/
        return type;
    }
}
