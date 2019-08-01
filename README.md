# RequestsJ
Requests for Android

# Getting started
The build is hosted on jcenter so you could just add the line:

```
implementation 'pk.codebase.requestsj:requestsj:0.11.1'
```

# Usage
The API is written for simplicity, below examples provide a peak into it.

### GET
```java
HttpRequest request = new HttpRequest();
request.setOnResponseListener(new HttpRequest.OnResponseListener() {
    @Override
    public void onResponse(HttpResponse response) {
        if (response.code == HttpResponse.HTTP_OK) {
            System.out.println(response.toJSONObject());
        }
    }
});
request.setOnErrorListener(new HttpRequest.OnErrorListener() {
    @Override
    public void onError(HttpError error) {
        // There was an error, deal with it
    }
});
request.get("https://httpbin.org/get");
```

### POST
```java
HttpRequest request = new HttpRequest();
request.setOnResponseListener(new HttpRequest.OnResponseListener() {
    @Override
    public void onResponse(HttpResponse response) {
        if (response.code == HttpResponse.HTTP_OK) {
            System.out.println(response.toJSONObject());
        }
    }
});
request.setOnErrorListener(new HttpRequest.OnErrorListener() {
    @Override
    public void onError(HttpError error) {
        // There was an error, deal with it
    }
});

JSONObject json;
try {
    json = new JSONObject();
    json.put("foo", "bar");
    json.put("jane", "doe");
} catch (JSONException ignore) {
    return;
}
request.post("https://httpbin.org/post", json);
```
### PUT, PATCH and DELETE
These methods are also supported and expected the same set of parameters as POST. The convenience methods are
request.put();
request.patch();
request.delete();

### Custom Headers
Headers can be sent using the `HttpHeaders` class.
```java
HttpHeaders headers = new HttpHeaders("Authorization", "Token sasdasdai2sadas")
request.get("https://httpbin.org/get", headers);
```
### FormData
```java
FormData data = new FormData();
data.put("foo", "bar");
data.put("file2", new File("/sdcard/image.png"));
request.post("https://httpbin.org/post", data);
```
