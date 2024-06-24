# Tealium Library for Java

[![License](https://img.shields.io/badge/license-Proprietary-blue.svg?style=flat
            )](https://github.com/Tealium/tealium-java/blob/master/LICENSE.txt)
[![Language](https://img.shields.io/badge/language-Java-orange.svg?style=flat
             )](http://www.oracle.com/technetwork/java/javase/overview/index.html)

This mobile library leverages the power of Tealium's [AudienceStream™](http://tealium.com/products/audiencestream/) making them natively available to Java applications.

Please contact your Account Manager first to verify yours agreement(s) for licensed products.

### What is Audience Stream ?

Tealium AudienceStream™ is the leading omnichannel customer segmentation and action engine, combining robust audience management and profile enrichment capabilities with the ability to take immediate, relevant action.

AudienceStream™ allows you to create a unified view of your customers, correlating data across every customer touchpoint, and then leverage that comprehensive customer profile across your entire digital marketing stack.

## How To Get Started

* Check out the [Getting Started](https://docs.tealium.com/platforms/java/install/) guide for a step by step walkthrough of adding Tealium to an existing project.  
* There are many other useful articles at the [Tealium Learning Community](https://community.tealiumiq.com).

## Contact Us

* If you have **code questions** or have experienced **errors** please post an issue in the [issues page](../../issues)
* If you have **general questions** or want to network with other users please visit the [Tealium Learning Community](https://community.tealiumiq.com)
* If you have **account specific questions** please contact your Tealium account manager

## Change Log

- 1.4.0 JDK 9+ Support
    - Removed unnecessary reference to `javax.xml.ws.http.HTTPException` to support newer JDKs without additional dependencies.
- 1.3.2 Persistent Data Deletion
    - `deletePersistentData` method added to the `DataManager` class  
- 1.3.1 Persistent Data 
    - Updated track method to use a copy of Persistent Data to stop event data unexpectedly being stored. 
- 1.3.0 Remove visitor_id and switch to event endpoint
    - "tealium_visitor_id" and "tealium_vid" removed
    - Use the "event" endpoint using the POST method with json
    - Create a Udo data object to represent data instead of the ambiguous Map<String, Object>
    - Update methods to use the Udo data type and deprecate api methods using Map<String, Object> for data.
    - Lots of refactoring and introduction of TDD practices to encourage more maintainable code in the library.
- 1.2.0 Add Datasource
	- New variable added:
		- tealium_datasource
	- Persistent file name changed to account.profile.data - NOTE: if upgrading from prior version, file no longer accessible.
	- Bug fix for unit test
- 1.1.0 API Update
    - New variable added:
        - tealium_event_type
    - track(type, title, data, completion) added as new primary track method, types avail:
        - activity
        - conversion
        - derived
        - interaction
        - view
    - DispatchCallback update to return info dictionary instead of just encodedUrl -> ( boolean, map<String, Object>, error) that 
    - Removal of convenience retry track(string, callBack)
- 1.0.1 Bug Fix
    - LogLevel enum converted to public API 
- 1.0.0 Initial Release
- Tealium universal data sources added for all dispatches:
    - event_name (transitionary - will be deprecated)
    - tealium_account
    - tealium_environment
    - tealium_event
    - tealium_library_name
    - tealium_library_version
    - tealium_profile
    - tealium_random
    - tealium_session_id
    - tealium_timestamp_epoch
    - tealium_visitor_id
    - tealium_vid (legacy support - will be deprecated)

## License

Use of this software is subject to the terms and conditions of the license agreement contained in the file titled "LICENSE.txt".  Please read the license before downloading or using any of the files contained in this repository. By downloading or using any of these files, you are agreeing to be bound by and comply with the license agreement.


---
Copyright (C) 2012-2024, Tealium Inc.
