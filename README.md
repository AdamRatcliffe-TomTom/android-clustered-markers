## Clustered markers example for Android ##

:warning: This example uses SDK version **0.3.1056**. Changes may be required for later SDK versions.

This example uses the [JavaSuperCluster](https://github.com/utahemre/JavaSuperCluster) library to manage marker density by clustering markers in near proximity. SuperCluster enables the efficient management of large numbers of markers - the example creates 100,000 markers at random locations in the San Francisco Bay Area.


### Running the example ###

1. To run the example you'll need an API key with the **Map Display** API enabled.

2. Open the project in Android Studio, the file `local.properties` will be generated in your project level directory, and add the following code to local.properties, replacing `YOUR_API_KEY` with your API key.

<code>API\_KEY=*YOUR\_API\_KEY*</code>

3. Save the file and run the app.