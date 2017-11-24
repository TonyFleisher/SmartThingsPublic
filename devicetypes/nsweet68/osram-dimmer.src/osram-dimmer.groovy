/**
 *  OSRAM Lightify Dimming Switch
 *
 *  Copyright 2016 Smartthings Comminuty
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Thanks to Michael Hudson for OSRAM Lightify Dimming Switch device.
 *  Also borrowed pieces from Virtual Dimmer and GE/Jasco Dimmer
 */

metadata {
	definition (name: "OSRAM Dimmer", namespace: "nsweet68", author: "nick@sweet-stuff.cc") {
		capability "Actuator"
		capability "Battery"
		capability "Button"
		capability "Holdable Button"
		capability "Switch"
		capability "Switch Level"
		capability "Configuration"
		capability "Refresh"

		attribute "lastButton", "string"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") { attributeState "level", action:"switch level.setLevel" }
		}
		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) { state "level", label: 'Level ${currentValue}%' }
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) { state "battery", label:'${currentValue}% battery' }

		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switch"
		details([
			"switch",
			"level",
			"battery",
			"levelSliderControl",
			"refresh"
		])
	}
	preferences {
		input name: "dimmerRate", type: "number", title: "Dimmer Rate", description: "Amount to Adjust Level when button is held", required: true, displayDuringSetup: true
		input name: "repeatHeld", type: "boolean", title: "Keep Held?", description: "If True, will send Held event and adjust level every second that button remains held", required: true, displayDuringSetup: true
	}
	fingerprint profileId: "0104", deviceId: "0001", inClusters: "0000, 0001, 0003, 0020, 0402, 0B05", outClusters: "0003, 0006, 0008, 0019", manufacturer: "CentraLite", model: "3130", deviceJoinName: "CentraList/OSRAM Dimming Switch"
}

// Parse incoming device messages to generate events
def parse(String description) {

	def result = []
	log.debug "parse description: $description"
	if (description?.startsWith('catchall:')) {
		// call parseCatchAllMessage to parse the catchall message received
		result += parseCatchAllMessage(description)
	} else if (description?.startsWith('read')) {
		// call parseReadMessage to parse the read message received
		result += parseReadMessage(description)
	} else {
		log.debug "Unknown message received: $description"
	}
	log.trace "Parse result is: ${result}"
	//return event list
	return result
}

def configure() {
	log.debug "Setting default values: inc and numberOfButton"
	setNumberOfButtons(2)
	log.debug "Confuguring Reporting and Bindings."
	def configCmds = [
		// Bind the outgoing on/off cluster from remote to hub, so the hub receives messages when On/Off buttons pushed
		"zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}",
		// Bind the outgoing level cluster from remote to hub, so the hub receives messages when Dim Up/Down buttons pushed
		"zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}",
		// Bind the incoming battery info cluster from remote to hub, so the hub receives battery updates
		"zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}",
	]
	return configCmds
}

def refresh() {
	sendEvent(name: 'battery', value: state.battery)
	def refreshCmds = [
		zigbee.readAttribute(0x0001, 0x0020)
	]
	//when refresh button is pushed, read updated status
	return refreshCmds
}

def parseReadMessage(String description) {
	// Create a map from the message description to make parsing more intuitive
	def msg = zigbee.parseDescriptionAsMap(description)
	//def msg = zigbee.parse(description)
	if (msg.clusterInt==1 && msg.attrInt==32) {
		// call getBatteryResult method to parse battery message into event map

		def result = getBatteryResult(Integer.parseInt(msg.value, 16))
	} else {
		log.debug "Unknown read message received, parsed message: $msg"
	}
	// return map used to create event
	return result
}

def parseCatchAllMessage(String description) {
	def results = []

	// Create a map from the raw zigbee message to make parsing more intuitive
	def msg = zigbee.parse(description)
	log.debug "Parse CatchAll $msg"
	switch(msg.clusterId) {
		case 1:
		// call getBatteryResult method to parse battery message into event map
			log.info 'BATTERY MESSAGE'
			results << getBatteryResult(Integer.parseInt(msg.value, 16))
			break
		case 6:
		// Pushed Events
			def button = (msg.command == 1 ? 1 : 2)
			if (button == 1) {
				on()
				state.pressed = 0
				state.lastButton = "button 1"
				results << buttonEvent(button,"pushed")
			}
			else
			{
				off()
				state.pressed = 0
				state.lastButton = "button 2"
				results << buttonEvent(button,"pushed")
			}
			break

		case 8:
		// Held (and release) Events
			switch(msg.command) {
				case 1: // brightness decrease command
					state.pressed = 1
					adjDimmer(dimmerRate * -1)
					state.lastButton = "button 2"
					results << buttonEvent(2,"held")
					scheduleIfNeeded(2)
				break
				case 3:
					state.pressed = 0
					log.info "Received stop hold command"
					results << createEvent(name: "release", value: "release", descriptionText: "${state.lastButton} released", isStateChange: true)
					break
				case 5: // brightness increase command
					state.pressed = 1
					adjDimmer(dimmerRate)
					state.lastButton = "button 1"
					results << buttonEvent(1,"held")
					scheduleIfNeeded(1)
					break
			}
	}

	return results
}


//obtained from other examples, converts battery message into event map
private Map getBatteryResult(rawValue) {
	def linkText = getLinkText(device)
	def result = [
		name: 'battery',
		value: state.battery
	]
	def volts = rawValue / 10
	def descriptionText
	if (rawValue == 0) {
		state.battery="unknown"
	} else {
		if (volts > 3.5) {
			result.descriptionText = "${linkText} battery has too much power (${volts} volts)."
			state.battery="overvoltage"
		} else if (volts > 0){
			def minVolts = 2.1
			def maxVolts = 3.0
			def pct = (volts - minVolts) / (maxVolts - minVolts)
			result.value = Math.min(100, (int) pct * 100)
			state.battery="${result.value}"
			result.descriptionText = "${linkText} battery was ${result.value}%"
		}
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def setNumberOfButtons(val) {
	state.numberOfButtons = val
	sendEvent(name:"numberOfButtons",value:2)
}

def on() {
	sendEvent(name: "switch", value: "on")
	if (state.dimmer < 1) {
		setLevel(dimmerRate)
	}
	log.info "Dimmer On"
}

def off() {
	sendEvent(name: "switch", value: "off")
	log.info "Dimmer Off"
}

def setLevel(val){
	log.info "setLevel $val"

	// make sure we don't drive switches past allowed values (command will hang device waiting for it to
	// execute. Never commes back)


	if (val < 0){
		val = 0
	}

	if( val > 100){
		val = 100
	}
	state.dimmer = val

	if (val == 0){ // I liked that 0 = off
		sendEvent(name:"level",value:val)
		off()
	}
	else
	{
		on()
		sendEvent(name:"level",value:val)
	}
}

def adjDimmer(adj){
	def dimVal = state.dimmer + adj
	setLevel(dimVal)


}

def buttonEvent(button,held) {
	createEvent(name: "button", value: held, data: [buttonNumber: button],
		descriptionText: "$device.displayName button $button was $held", isStateChange: true)
}

def scheduleIfNeeded(button) {
	if (repeatHeld) {
		runIn(1, buttonHeld, [data: [button:button]])
	}
}

def buttonHeld(data) {
	if (data.button == 2) {
		log.debug "Held down"
		holdDown()
	} else if (data.button == 1) {
		log.debug "Held up"
		holdUp()
	} else {
		log.debug "Unknown held: ${ data}"
	}
}

def holdDown() {
	if (state.pressed == 1 && state.lastButton == "button 2") {
		adjDimmer(dimmerRate * -1)
		scheduleIfNeeded(2)
	} else {
		log.debug "Hold Down stopped"
	}
}

def holdUp() {
	if (state.pressed == 1 && state.lastButton == "button 1") {
		adjDimmer(dimmerRate)
		scheduleIfNeeded(1)
	} else {
		log.debug "Hold Up stopped"
	}
}