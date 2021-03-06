import Flutter
import UIKit
import CoreLocation

public class SwiftFlutterCompassPlugin: NSObject, FlutterPlugin, FlutterStreamHandler, CLLocationManagerDelegate {

    private var eventSink: FlutterEventSink?;
    private var location: CLLocationManager = CLLocationManager();

    init(channel: FlutterEventChannel) {
        super.init()
        let status  = CLLocationManager.authorizationStatus()
        if (status == .authorizedAlways) {
            print("status enabled")
        } else {
            location.requestAlwaysAuthorization();
            location.requestWhenInUseAuthorization();
        }
        location.delegate = self
        location.headingFilter = 1;
        channel.setStreamHandler(self);
    }


  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterEventChannel.init(name: "hemanthraj/flutter_compass", binaryMessenger: registrar.messenger())
    _ = SwiftFlutterCompassPlugin(channel: channel);
  }

    public func onListen(withArguments arguments: Any?,
                         eventSink: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = eventSink;
        location.startUpdatingHeading();
        return nil;
    }

    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        eventSink = nil;
        location.stopUpdatingHeading();
        return nil;
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        if(newHeading.headingAccuracy>0){
            let heading:CLLocationDirection!;
            var adjustDegree = 0.0;
            switch UIDevice.current.orientation{
                case .portrait:
                    adjustDegree = 0.0;
                case .portraitUpsideDown:
                    adjustDegree = 0.0;
                case .landscapeLeft:
                   adjustDegree = -90;
                case .landscapeRight:
                   adjustDegree = 90;
                default:
                   adjustDegree = 0.0;
            }
            heading = newHeading.trueHeading > 0 ? newHeading.trueHeading : newHeading.magneticHeading;
            eventSink!(heading - adjustDegree);
        }
    }
}
