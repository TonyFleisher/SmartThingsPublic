/**
 *  Copyright 2015 SmartThings
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
 */
metadata {
    definition (name: "Hue Motion Sensor", namespace: "digitalgecko", author: "digitalgecko") {

        
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Illuminance Measurement" //0x0400

command "setSensitivity"
command "increaseSensitivity"
command "decreaseSensitivity"
command "sensitivityLow"
command "sensitivityMed"
command "sensitivityHigh"

attribute "sensitivity", "enum", ["low","medium","high", "unknown"]

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0406,0400,0402", outClusters: "0019", manufacturer: "Philips", model: "SML001", deviceJoinName: "Hue Motion Sensor"
    }

	preferences {
    		section {
			input title: "Temperature Offset", description: "This feature allows you to correct any temperature variations by selecting an offset. Ex: If your sensor consistently reports a temp that's 5 degrees too warm, you'd enter '-5'. If 3 degrees too cold, enter '+3'.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			input "tempOffset", "number", title: "Degrees", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false
		}
            section {
			input title: "Luminance Offset", description: "This feature allows you to correct the luminance reading by selecting an offset. Enter a value such as 20 or -20 to adjust the luminance reading.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			input "luxOffset", "number", title: "Lux", description: "Adjust luminance by this amount", range: "*..*", displayDuringSetup: false
		}
        
//       	section {
//            input title: "Sensitivity", description:"Sensitivity for motion", displayDuringSetup: false, type:"paragraph", element:"paragraph"	
//            input ("sensitivity", "enum", title:"Sensitivity", description: "0 = Low, 2 = High", range:"0..2", displayDuringSetup: false, 
//            	options: ["low", "medium", "high"], defaultValue: "device.sensitivity")
//        }
    }

    tiles(scale: 2) {
		multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
			tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
				attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
			}
            tileAttribute ("device.sensitivity", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "increaseSensitivity"
                attributeState "VALUE_DOWN", action: "decreaseSensitivity"
//			tileAttribute ("device.sensitivity", key: "SECONDARY_CONTROL") {
//				attributeState "sensitivity", label: 'Sensitivity: ${currentValue}'
            }
		}
       
        
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label:'${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}
        
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		valueTile("illuminance", "device.illuminance", width: 2, height: 2) {
			state("illuminance", label:'${currentValue}', unit:"lux",
				backgroundColors:[
					[value: 9, color: "#767676"],
					[value: 315, color: "#ffa81e"],
					[value: 1000, color: "#fbd41b"]
				]
			)
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("sensitivityLow", "sensitivityLow", decoration: "flat", width: 2, height: 2) {
			state "default", label:'Low\nSensitivity', action: "sensitivityLow"
		}
        standardTile("sensitivityMed", "sensitivityMed", decoration: "flat", width: 2, height: 2) {
			state "default", label:'Medium\nSensitivity', action: "sensitivityMed"
		}
        standardTile("sensitivityHigh", "sensitivityHigh", decoration: "flat", width: 2, height: 2) {
			state "default", label:'High\nSensitivity', action: "sensitivityHigh"
		}

        main "motion"
        details(["motion","temperature","battery","illuminance","sensitivityLow","sensitivityMed", "sensitivityHigh", "refresh"])
    }
}

def setSensitivity(value = "NOVALUE") {
	log.debug "SetSensitivity: ${value}"
	def cmds = []
	cmds += writeSensitivityAttribute(value)
	return cmds
}

def sensitivityHigh() {
	setSensitivity(0x02)
}

def sensitivityMed() {
	setSensitivity(0x01)
}

def sensitivityLow() {
	setSensitivity(0x00)
}

def increaseSensitivity()
{
	def curVal = device.currentValue("sensitivity")
	log.trace "increaseSensitivity : " + curVal
	
	def nextValue = curVal == "high" ? null : curVal == "medium" ? 0x02 : 0x01
	writeSensitivityAttribute(nextValue)
}

def decreaseSensitivity()
{
	def curVal = device.currentValue("sensitivity")
	log.trace "decreaseSensitivity : " + curVal
	
	def nextValue = curVal == "low" ? null : curVal == "medium" ? 0x00 : 0x01
	writeSensitivityAttribute(nextValue)

}

def getSensitivity(){
	return device.currentValue("sensitivity")
}

def writeSensitivityAttribute(newValue) {
    log.debug "Writing sensitiviy: ${newValue}"
    def cmds= []
    if(newValue == null || newValue > 0x02 || newValue < 0x00) { 
        log.debug "Invalid value for sensitivity: ${newValue}"
        return
    }
    
    cmds += zigbee.writeAttribute(0x0406, 0x0030, 0x20, newValue, [mfgCode: 0x100b])

	//log.trace "writeSensitivity: ${ cmds }"
	return cmds
}

// Parse incoming device messages to generate events
def parse(String description) {
    def msg = zigbee.parse(description)
    def parsed = false
    //log.warn "--"
    log.trace "parse: ${description}"
    //log.debug msg
    //def x = zigbee.parseDescriptionAsMap( description )
    //log.error x
    
	Map map = [:]
    if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
        parsed=true
	}
	else if (description?.startsWith('temperature: ')) {
		map = parseCustomMessage(description)
        parsed=true
	}
    else if (description?.startsWith('illuminance: ')) {
		map = parseCustomMessage(description)
        parsed=true
	}
//	else if (description?.startsWith('zone status')) {
//		//map = parseIasMessage(description)
//        log.trace "zone status"
//	}

	def result = map ? createEvent(map) : null

	if (description?.startsWith('enroll request')) {
		List cmds = enrollResponse()
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
        parsed=true
	}
	else if (description?.startsWith('read attr -')) {
    //log.trace "parse - read attr - ${description}"
		result = parseReportAttributeMessage(description).each { createEvent(it) }
        parsed=true
	}
    
    if (!parsed){
        log.debug "NotParsed:${description}"
    }
	return result
}

/*
  Refresh Function
*/
def refresh() {
    return refreshCmds() + configCmds()
}
    
def refreshCmds() {
    log.debug "Refreshing Values"

    def refreshCmds = []
    refreshCmds +=zigbee.readAttribute(0x0001, 0x0020) // Read battery?
    refreshCmds += zigbee.readAttribute(0x0402, 0x0000) // Read temp?
    refreshCmds += zigbee.readAttribute(0x0400, 0x0000) // Read luminance?
    refreshCmds += zigbee.readAttribute(0x0406, 0x0000) // Read motion?

	refreshCmds += zigbee.readAttribute(0x0406, 0x0010) // Read PIROccupiedToUnoccupiedDelay

    refreshCmds += zigbee.readAttribute(0x0406, 0x0030, [mfgCode: 0x100b]) // Read sensitivity 0=low, 1=med, 2=high
    return refreshCmds
}
/*
  Configure Function
*/
def configure() {
    return configCmds() + refreshCmds()
}

def configCmds() {
	String zigbeeId = swapEndianHex(device.hub.zigbeeId)
	log.debug "Confuguring Reporting and Bindings."
    
    
	def configCmds = []
    configCmds += zigbee.batteryConfig()
	configCmds += zigbee.temperatureConfig(60, 600) // Set temp reporting times // Confirmed
    
    configCmds += zigbee.configureReporting(0x406,0x0000, 0x18, 0, 3600, null) // motion // confirmed
    
        
	configCmds += zigbee.configureReporting(0x400,0x0000, 0x21, 60, 3600, 0x20) // Set luminance reporting times?? maybe    
	configCmds += zigbee.configureReporting(0x406,0x0030, 0x20, 0, 3600, 1, [mfgCode: 0x100b]) // sensitivity reporting

	return configCmds
}

/*
	getMotionResult
 */

private Map getMotionResult(value) {
    //log.trace "Motion : " + value
	
    def descriptionText = value == "01" ? '{{ device.displayName }} detected motion':
			'{{ device.displayName }} stopped detecting motion'
    
    return [
		name: 'motion',
		value: value == "01" ? "active" : "inactive",
		descriptionText: descriptionText,
		translatable: true,
	]
}


/*
  getTemperatureResult
*/
private Map getTemperatureResult(value) {

	//log.trace "Temperature : " + value
	if (tempOffset) {
		def offset = tempOffset as int
		def v = value as int
		value = v + offset
	}
	def descriptionText = temperatureScale == 'C' ? '{{ device.displayName }} was {{ value }}°C':
			'{{ device.displayName }} was {{ value }}°F'

	return [
		name: 'temperature',
		value: value,
		descriptionText: descriptionText,
		translatable: true,
		unit: temperatureScale
	]
}

def getTemperature(value) {
	def celsius = Integer.parseInt(value, 16).shortValue() / 100
	if(getTemperatureScale() == "C"){
		return Math.round(celsius)
		} else {
			return Math.round(celsiusToFahrenheit(celsius))
		}
	}

private Map getLuminanceResult(rawValue) {
	//log.debug "Luminance rawValue = ${rawValue}"

	if (luxOffset) {
		def offset = luxOffset as int
		def v = rawValue as int
		rawValue = v + offset
	}
    
	def result = [
		name: 'illuminance',
		value: '--',
		translatable: true,
 		unit: 'lux'
	]
    
    result.value = rawValue as Integer
    return result
}

/*
	getBatteryResult
*/
//TODO: needs calibration
private Map getBatteryResult(rawValue) {
	//log.debug "Battery rawValue = ${rawValue}"

	def result = [
		name: 'battery',
		value: '--',
		translatable: true
	]

	def volts = rawValue / 10

	if (rawValue == 0 || rawValue == 255) {}
	else {
		if (volts > 3.5) {
			result.descriptionText = "{{ device.displayName }} battery has too much power: (> 3.5) volts."
		}
		else {
			if (device.getDataValue("manufacturer") == "SmartThings") {
				volts = rawValue // For the batteryMap to work the key needs to be an int
				def batteryMap = [28:100, 27:100, 26:100, 25:90, 24:90, 23:70,
								  22:70, 21:50, 20:50, 19:30, 18:30, 17:15, 16:1, 15:0]
				def minVolts = 15
				def maxVolts = 28

				if (volts < minVolts)
					volts = minVolts
				else if (volts > maxVolts)
					volts = maxVolts
				def pct = batteryMap[volts]
				if (pct != null) {
					result.value = pct
					result.descriptionText = "{{ device.displayName }} battery was {{ value }}%"
				}
			}
			else {
				def minVolts = 2.1
				def maxVolts = 3.0
				def pct = (volts - minVolts) / (maxVolts - minVolts)
				def roundedPct = Math.round(pct * 100)
				if (roundedPct <= 0)
					roundedPct = 1
				result.value = Math.min(100, roundedPct)
				result.descriptionText = "{{ device.displayName }} battery was {{ value }}%"
			}
		}
	}

	return result
}

/*
	getSensitivityResult
 */

private Map getSensitivityResult(value) {
    // log.debug "getSensitivityResult : " + value
	
    def sensitivityString = getSensitivityString(value)
    
    def descriptionText = '{{ device.displayName }} sensitivity {{ value }}'
	
    log.trace "Sdesc: ${sensitivityString}"
    
    def result = [
		name: 'sensitivity',
		value: sensitivityString,
		descriptionText: descriptionText,
		translatable: true,
        isStateChange: true
	]
    // log.debug "Sensitivity Result: {$result}"
    return result
}

private String getSensitivityString(value) {
    return value == 0 ? "low": value == 1 ? "medium" : value == 2 ? "high" : "unknown"
}

/*
	parseCustomMessage
*/
private Map parseCustomMessage(String description) {
	// log.trace "parseCustomMessage: ${ description }"
	Map resultMap = [:]
	if (description?.startsWith('temperature: ')) {
		def value = zigbee.parseHATemperatureValue(description, "temperature: ", getTemperatureScale())
		resultMap = getTemperatureResult(value)
	}
    
    if (description?.startsWith('illuminance: ')) {
    //log.warn "parse illuminance:value: " + description.split(": ")[1]

		def value = zigbee.lux( description.split(": ")[1] as Integer ) //zigbee.parseHAIlluminanceValue(description, "illuminance: ", getTemperatureScale())
            //log.warn "parse illuminance:proc: " + value
		resultMap = getLuminanceResult(value)
	}
	return resultMap
}

/*
	parseReportAttributeMessage
*/
private List parseReportAttributeMessage(String description) {
	// log.trace "parseReportAttributeMessage: ${description}"
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}

	List result = []
    
    // Temperature
	if (descMap.cluster == "0402" && descMap.attrId == "0000") {
		log.trace "parsing temp report atribute"
		def value = getTemperature(descMap.value)
		result << getTemperatureResult(value)
	}
    
    // Motion
   	else if (descMap.cluster == "0406" && descMap.attrId == "0000") {
		log.trace "parsing motion report atribute"
		result << getMotionResult(descMap.value)
	}
    
    // Battery
	else if (descMap.cluster == "0001" && descMap.attrId == "0020") {
		log.trace "parsing battery report atribute"
		result << getBatteryResult(Integer.parseInt(descMap.value, 16))
	}
    
    // Luminance
    else if (descMap.cluster == "0402" ) { //&& descMap.attrId == "0020") {
		log.trace "parsing lux report atribute"
		log.error "Luminance Response " + description
        //result << getBatteryResult(Integer.parseInt(descMap.value, 16))
	}

	// Sensitivity
    else if (descMap.cluster == "0406" && descMap.attrId == "0030") {
		log.trace "parsing sensitity report atribute:${descMap.value}"
		result << getSensitivityResult(Integer.parseInt(descMap.value, 16))
        
    }
    else if (descMap.cluster == "0406" && descMap.attrId == "0010") {
		log.trace "parsing PIROccupiedToUnoccupiedDelay:${descMap.value}"
		// Nothing to do        
    }

    else {
     log.warn "Unhandled attribute: ${descMap.cluster} ${descMap.attrId} ${descMap.value}"
    }
	return result
}


/*
	parseCatchAllMessage
*/
private Map parseCatchAllMessage(String description) {
	//log.trace "parseCatchallMessage: ${ description }"

	Map resultMap = [:]
	def cluster = zigbee.parse(description)
//	log.debug cluster
	if (shouldProcessMessage(cluster)) {
		switch(cluster.clusterId) {
			case 0x0001:
				// 0x07 - configure reporting
				if (cluster.command != 0x07) {
					resultMap = getBatteryResult(cluster.data.last())
				}
			break

			case 0x0400:
            	if (cluster.command == 0x07) { // Ignore Configure Reporting Response
                	if(cluster.data[0] == 0x00) {
						log.trace "Luminance Reporting Configured"
						sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
					}
					else {
						log.warn "Luminance REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
					}
				}
				else {
            		log.debug "catchall : luminance" + cluster
                	resultMap = getLuminanceResult(cluster.data.last());
                }

			break
            
			
            
			case 0x0402:
				if (cluster.command == 0x07) {
					if(cluster.data[0] == 0x00) {
						log.trace "Temperature Reporting Configured"
						sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
					}
					else {
						log.warn "TEMP REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
					}
				}
				else {
					// temp is last 2 data values. reverse to swap endian
					String temp = cluster.data[-2..-1].reverse().collect { cluster.hex1(it) }.join()
					def value = getTemperature(temp)
					resultMap = getTemperatureResult(value)
				}
			break
			
			case 0x0406:
				if (cluster.command == 0x07) {
					if(cluster.data[0] == 0x00) {
						log.trace "Motion Reporting Configured"
					}
					else {
						log.warn "Motion REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
					}
				}
				
				if (cluster.command == 0x04) { 
					if (cluster.data[0] == 0x00) { 
						log.trace "Write Attribute Response: OK"
					} else { 
						log.warn "Write Attribute FAILED- error code: ${ cluster.data[0]}"
					}
				}
			break			
		}
	}

	return resultMap
}

private boolean shouldProcessMessage(cluster) {
	// 0x0B is default response indicating message got through
	boolean ignoredMessage = cluster.profileId != 0x0104 ||
	cluster.command == 0x0B ||
	(cluster.data.size() > 0 && cluster.data.first() == 0x3e)
    
    //log.trace "shouldProcess is: ${ !ignoredMessage}: ${cluster}"
	return !ignoredMessage
}


// This seems to be IAS Specific and not needed we are not really a motion sensor
def enrollResponse() {
//	log.debug "Sending enroll response"
//	String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
//	[
//		//Resending the CIE in case the enroll request is sent before CIE is written
//		"zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}", "delay 200",
//		"send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 500",
//		//Enroll Response
//		"raw 0x500 {01 23 00 00 00}", "delay 200",
//		"send 0x${device.deviceNetworkId} 1 1", "delay 200"
//	]
}

def configureHealthCheck() {
    Integer hcIntervalMinutes = 12
    refresh()
    sendEvent(name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def updated() {
    log.debug "in updated()"
    configureHealthCheck()
}

def ping() {
    return zigbee.onOffRefresh()
}





private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}