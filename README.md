# TESLA Camera Client
TESLA-based Android client for the demo camera application.

With this application installed on your Android devices, you can create a low-cost camera system. You can use it to protect your home or watch out for your children or your pet. With this solution, you can utilize your old out-of-use phones or even avoid to buy an expensive IP camera system.

The application needs the following important permissions:
* camera
* write external storage
* internet
* change the wifi multicast state
* wake lock
* access coarse and fine location

After the application granted all the necessary premissions, you can configure it. You must provide the address and the port of the TESLA server, and also the address of the web application, so the client can comminicate with the REST API.

After the configuration, you have to perform loose time synchronization, so the app can communicate well with the TESLA server.

After all these things, just place the phone in a good position, then start the app. When the client receives a broadcast message, it captures an image, then sends it with some extra data (like GPS coordinates) to the REST endpoint.

Currently there is not any release APK file that you can install directly on your Android phone, so the best option is to connect your phone to your development PC, and then install the app with the Android Studio IDE.