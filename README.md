# ProximityKit Android Refence App

Demo application showing a sample setup and implementation of the [Proximity
Kit](https://proximitykit.radiusnetworks.com) Android library. This is intended
for demonstration purposes and is not meant to be a fully feature application.

## Installation

### Android Studio

This project builds with Android Studio 1.0.1 with Gradle 2.2.1

1. Clone the repo

  ```console
  $ git clone https://github.com/RadiusNetworks/proximitykit-reference-android
  ```

2. Open Android Studio.

3. Select 'Open Project', navigate to where you cloned the repository and
   select the [`build.gradle`](build.gradle) file to open the project.

4. Ensure you have the Google Play services library installed:

   `Android SDK Manager` > `Extras` > `Google Play services`

   Refer to the latest [documentation on installing Google Play
   services](https://developer.android.com/google/play-services/setup.html) for
   up to date instructions.

5. Connect a device and run the app.

### Eclipse

TBD

## Usage

The app runs and works. Hooray!!! However, you don't use any of our sample
beacons and/or you aren't in our sample geofence area.

No problem. You can swap our sample kit configuration out with your own kit:

1. Sign up for [Proximity Kit](https://proximitykit.radiusnetworks.com/plans)
   if you have not done so yet. We offer free plans if you just want to try
   things out.

2. After logging in, download [your kit's](https://proximitykit.radiusnetworks.com/kits)
   `.properties` file.

3. Replace [`ProximityKit.properties`](AndroidProximityKitReference/src/main/resources/ProximityKit.properties)
   with your downloaded file.

4. Run the app.

## Contributing

1. Fork it ( https://github.com/radiusnetworks/proximitykit-reference-android/fork )
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

## License

Copyright (c) 2014 by Radius Networks
http://www.radiusnetworks.com

All Rights Reserved

ATTRIBUTION ASSURANCE LICENSE (adapted from the original BSD license) see
[LICENSE.txt](LICENSE.txt) for details.
