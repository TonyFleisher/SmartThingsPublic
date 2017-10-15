definition(
    name: "My SmartHome",
    namespace: "TonyFleisher",
    //Change line below to 'false' to allow for multi app install (Advanced...see instructions)
    	singleInstance: true,
    //-----------------------------------------------------------
    author: "Tony Fleisher",
    //parent: parent ? "TonyFleisher.MySmartHome-Alexa" : null,
    description: "Smart Home Controls",
    category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        	oauth: true)
  	
preferences(oauthPage: pageOAuthDevices) { 
	page name: "pageOAuthDevices"
}

def pageOAuthDevices() { 
	dynamicPage (pageName: "oauthDevices", install: true, uninstall: false) {
		section("Choose the devices to allow", hideWhenEmpty: true) {
			input "myDevices", "capability.Actuator", title: "Choose Devices", multiple: true, required: false
		}
	}
}

// Initialization
def installed() { initialize() }
def updated() { initialize() }
def initialize () { 
// Do some stuff
log.debug "Initialize.."
}