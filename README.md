# requests
XmlHttpRequest for Android

# Usage
```java
HttpRequest request = new HttpRequest(context);
request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
      @Override
      public void onReadyStateChange(HttpRequest request, int readyState) {
          switch (readyState) {
              case HttpRequest.STATE_DONE:
                  switch (request.getStatus()) {
                     case HttpURLConnection.HTTP_OK:
                        // Request successful
                        break;
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        // Something was wrong
                        request.getResponseText();
                        request.getStatusText();
                        break;
            }
        }
    }
});
request.open("POST", URL);
requst.send(JsonString);
```

Note: this document is still WIP.
